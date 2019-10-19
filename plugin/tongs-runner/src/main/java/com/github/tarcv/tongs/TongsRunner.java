/*
 * Copyright 2019 TarCV
 * Copyright 2014 Shazam Entertainment Limited
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License.
 *
 * You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package com.github.tarcv.tongs;

import com.github.tarcv.tongs.model.Pool;
import com.github.tarcv.tongs.model.TestCaseEvent;
import com.github.tarcv.tongs.pooling.NoDevicesForPoolException;
import com.github.tarcv.tongs.pooling.NoPoolLoaderConfiguredException;
import com.github.tarcv.tongs.pooling.PoolLoader;
import com.github.tarcv.tongs.runner.PoolTestRunnerFactory;
import com.github.tarcv.tongs.runner.ProgressReporter;
import com.github.tarcv.tongs.runner.TestCaseRunResult;
import com.github.tarcv.tongs.suite.JUnitTestSuiteLoader;
import com.github.tarcv.tongs.suite.NoTestCasesFoundException;
import com.github.tarcv.tongs.suite.TestSuiteLoaderContext;
import com.github.tarcv.tongs.summary.SummaryGeneratorHook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.stream.Collectors;

import static com.github.tarcv.tongs.Utils.namedExecutor;
import static com.github.tarcv.tongs.injector.ConfigurationInjector.configuration;
import static com.github.tarcv.tongs.injector.runner.RemoteAndroidTestRunnerFactoryInjector.remoteAndroidTestRunnerFactory;
import static com.github.tarcv.tongs.injector.runner.TestRunFactoryInjector.testRunFactory;

public class TongsRunner {
    private static final Logger logger = LoggerFactory.getLogger(TongsRunner.class);

    private final PoolLoader poolLoader;
    private final PoolTestRunnerFactory poolTestRunnerFactory;
    private final ProgressReporter progressReporter;
    private final SummaryGeneratorHook summaryGeneratorHook;

    public TongsRunner(PoolLoader poolLoader,
                       PoolTestRunnerFactory poolTestRunnerFactory,
                       ProgressReporter progressReporter,
                       SummaryGeneratorHook summaryGeneratorHook) {
        this.poolLoader = poolLoader;
        this.poolTestRunnerFactory = poolTestRunnerFactory;
        this.progressReporter = progressReporter;
        this.summaryGeneratorHook = summaryGeneratorHook;
    }

    public boolean run() {
        ExecutorService poolExecutor = null;
        try {
            Collection<Pool> pools = poolLoader.loadPools();
            int numberOfPools = pools.size();
            CountDownLatch poolCountDownLatch = new CountDownLatch(numberOfPools);
            poolExecutor = namedExecutor(numberOfPools, "PoolExecutor-%d");

            Map<Pool, Collection<TestCaseEvent>> poolTestCasesMap = new HashMap<>();
            for (Pool pool : pools) {
                Collection<TestCaseEvent> testCases = createTestSuiteLoaderForPool(pool);
                poolTestCasesMap.put(pool, testCases);
            }

            // TODO: check that different sets of test cases in different pools doesn't fail run
            Collection<TestCaseEvent> allTestCases = poolTestCasesMap.values().stream()
                    .flatMap(poolEvents -> poolEvents.stream())
                    .collect(Collectors.toSet());
            List<TestCaseRunResult> allResults = new ArrayList<>();
            summaryGeneratorHook.registerHook(pools, allTestCases, allResults);

            progressReporter.start();
            for (Pool pool : pools) {
                Collection<TestCaseEvent> poolTestCases = poolTestCasesMap.get(pool);
                Runnable poolTestRunner = poolTestRunnerFactory.createPoolTestRunner(pool,
                        poolTestCases, allResults,
                        poolCountDownLatch, progressReporter);
                poolExecutor.execute(poolTestRunner);
            }
            poolCountDownLatch.await();
            progressReporter.stop();

            boolean overallSuccess = summaryGeneratorHook.defineOutcome();
            summaryGeneratorHook.unregisterHook();
            logger.info("Overall success: " + overallSuccess);
            return overallSuccess;
        } catch (NoPoolLoaderConfiguredException | NoDevicesForPoolException e) {
            logger.error("Configuring devices and pools failed", e);
            return false;
        } catch (NoTestCasesFoundException e) {
            logger.error("Error when trying to find test classes", e);
            return false;
        } catch (Exception e) {
            logger.error("Error while Tongs was executing", e);
            return false;
        } finally {
            if (poolExecutor != null) {
                poolExecutor.shutdown();
            }
        }
    }

    // TODO: move to a separate file
    private static Collection<TestCaseEvent> createTestSuiteLoaderForPool(Pool pool) throws NoTestCasesFoundException {
        Configuration configuration = configuration();
        TestSuiteLoaderContext testSuiteLoaderContext = new TestSuiteLoaderContext(configuration, pool);
        return new JUnitTestSuiteLoader(testSuiteLoaderContext, testRunFactory(configuration), remoteAndroidTestRunnerFactory(configuration))
                .loadTestSuite();
    }
}
