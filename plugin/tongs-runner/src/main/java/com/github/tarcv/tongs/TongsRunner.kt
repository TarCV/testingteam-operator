/*
 * Copyright 2020 TarCV
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
package com.github.tarcv.tongs

import com.github.tarcv.tongs.injector.ConfigurationInjector
import com.github.tarcv.tongs.injector.ConfigurationInjector.configuration
import com.github.tarcv.tongs.injector.TestCaseRuleManager
import com.github.tarcv.tongs.injector.runner.RemoteAndroidTestRunnerFactoryInjector
import com.github.tarcv.tongs.injector.runner.TestRunFactoryInjector
import com.github.tarcv.tongs.model.Pool
import com.github.tarcv.tongs.model.TestCaseEvent
import com.github.tarcv.tongs.pooling.NoDevicesForPoolException
import com.github.tarcv.tongs.pooling.NoPoolLoaderConfiguredException
import com.github.tarcv.tongs.pooling.PoolLoader
import com.github.tarcv.tongs.runner.PoolTestRunnerFactory
import com.github.tarcv.tongs.runner.ProgressReporter
import com.github.tarcv.tongs.runner.TestCaseRunResult
import com.github.tarcv.tongs.runner.rules.TestCaseRuleContext
import com.github.tarcv.tongs.suite.JUnitTestSuiteLoader
import com.github.tarcv.tongs.suite.NoTestCasesFoundException
import com.github.tarcv.tongs.suite.TestSuiteLoaderContext
import com.github.tarcv.tongs.summary.SummaryGeneratorHook
import org.slf4j.LoggerFactory
import java.util.*
import java.util.concurrent.CountDownLatch
import java.util.concurrent.ExecutorService

class TongsRunner(private val poolLoader: PoolLoader,
                  private val poolTestRunnerFactory: PoolTestRunnerFactory,
                  private val progressReporter: ProgressReporter,
                  private val summaryGeneratorHook: SummaryGeneratorHook,
                  private val testCaseRuleManager: TestCaseRuleManager) {
    fun run(): Boolean {
        var poolExecutor: ExecutorService? = null
        return try {
            val pools = poolLoader.loadPools()
            val numberOfPools = pools.size
            val poolCountDownLatch = CountDownLatch(numberOfPools)
            poolExecutor = Utils.namedExecutor(numberOfPools, "PoolExecutor-%d")

            val poolTestCasesMap: Map<Pool, Collection<TestCaseEvent>> = pools
                    .map { pool ->
                        val testCaseRules = testCaseRuleManager
                                .createRulesFrom {
                                    configuration -> TestCaseRuleContext(configuration, pool)
                                }
                        val testCases = createTestSuiteLoaderForPool(pool)
                                .map { testCaseEvent: TestCaseEvent ->
                                    testCaseRules.fold(testCaseEvent) { acc, rule -> rule.transform(acc) }
                                }
                        pool to testCases
                    }
                    .toMap()

            // TODO: check that different sets of test cases in different pools doesn't fail run
            val allResults: List<TestCaseRunResult> = ArrayList()
            summaryGeneratorHook.registerHook(pools, poolTestCasesMap, allResults)

            progressReporter.start()
            for (pool in pools) {
                val poolTestCases = poolTestCasesMap[pool]
                val poolTestRunner = poolTestRunnerFactory.createPoolTestRunner(pool,
                        poolTestCases, allResults,
                        poolCountDownLatch, progressReporter)
                poolExecutor.execute(poolTestRunner)
            }
            poolCountDownLatch.await()
            progressReporter.stop()

            val overallSuccess = summaryGeneratorHook.defineOutcome()
            summaryGeneratorHook.unregisterHook()
            logger.info("Overall success: $overallSuccess")
            overallSuccess
        } catch (e: NoPoolLoaderConfiguredException) {
            logger.error("Configuring devices and pools failed", e)
            false
        } catch (e: NoDevicesForPoolException) {
            logger.error("Configuring devices and pools failed", e)
            false
        } catch (e: NoTestCasesFoundException) {
            logger.error("Error when trying to find test classes", e)
            false
        } catch (e: Exception) {
            logger.error("Error while Tongs was executing", e)
            false
        } finally {
            poolExecutor?.shutdown()
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(TongsRunner::class.java)

        // TODO: move to a separate file
        @Throws(NoTestCasesFoundException::class)
        private fun createTestSuiteLoaderForPool(pool: Pool): Collection<TestCaseEvent> {
            val configuration = ConfigurationInjector.configuration()
            val testSuiteLoaderContext = TestSuiteLoaderContext(configuration, pool)
            return JUnitTestSuiteLoader(testSuiteLoaderContext, TestRunFactoryInjector.testRunFactory(configuration), RemoteAndroidTestRunnerFactoryInjector.remoteAndroidTestRunnerFactory(configuration))
                    .loadTestSuite()
        }
    }

}