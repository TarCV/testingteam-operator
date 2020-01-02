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
package com.github.tarcv.tongs.runner;

import com.github.tarcv.tongs.Configuration;
import com.github.tarcv.tongs.injector.ConfigurationInjector;
import com.github.tarcv.tongs.injector.runner.TestRunFactoryInjector;
import com.github.tarcv.tongs.injector.system.FileManagerInjector;
import com.github.tarcv.tongs.model.*;
import com.github.tarcv.tongs.runner.listeners.TongsTestListener;
import com.github.tarcv.tongs.runner.rules.TestCaseRunRuleContext;
import com.github.tarcv.tongs.summary.ResultStatus;
import com.github.tarcv.tongs.system.io.TestCaseFileManager;
import com.github.tarcv.tongs.system.io.TestCaseFileManagerImpl;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static com.github.tarcv.tongs.injector.listeners.TestRunListenersTongsFactoryInjector.testRunListenersTongsFactory;
import static com.github.tarcv.tongs.summary.ResultStatus.ERROR;
import static com.github.tarcv.tongs.summary.ResultStatus.UNKNOWN;
import static java.util.Collections.emptyList;

public class DeviceTestRunner implements Runnable {
    private static final Logger logger = LoggerFactory.getLogger(DeviceTestRunner.class);

    private final Pool pool;
    private final Device device;
    private final TestCaseEventQueue queueOfTestsInPool;
    private final CountDownLatch deviceCountDownLatch;
    private final ProgressReporter progressReporter;

    public DeviceTestRunner(Pool pool,
                            Device device,
                            TestCaseEventQueue queueOfTestsInPool,
                            CountDownLatch deviceCountDownLatch,
                            ProgressReporter progressReporter) {
        this.pool = pool;
        this.device = device;
        this.queueOfTestsInPool = queueOfTestsInPool;
        this.deviceCountDownLatch = deviceCountDownLatch;
        this.progressReporter = progressReporter;
    }

    @Override
    public void run() {
        try {
            // TODO: call DeviceRule incl. AndroidSetupDeviceRule

            while (true) {
                TestCaseEventQueue.TestCaseTask testCaseTask = queueOfTestsInPool.pollForDevice(device, 10);
                if (testCaseTask != null) {
                    testCaseTask.doWork(testCaseEvent -> {
                        TestCaseFileManager testCaseFileManager = new TestCaseFileManagerImpl(FileManagerInjector.fileManager(), pool, device, testCaseEvent.getTestCase());
                        Configuration configuration = ConfigurationInjector.configuration();
                        TestCaseRunRuleContext context = new TestCaseRunRuleContext(
                                configuration, testCaseFileManager,
                                pool, device, testCaseEvent);

                        List<TongsTestListener> testRunListeners = new ArrayList<>();
                        testRunListeners.addAll(testRunListenersTongsFactory(configuration).createTongsListners(
                                testCaseEvent,
                                device,
                                pool,
                                progressReporter,
                                queueOfTestsInPool,
                                configuration.getTongsIntegrationTestRunType()));

                        TestCase identifier = new TestCase(testCaseEvent.getTestMethod(), testCaseEvent.getTestClass());

                        // TODO: Add some defensive code
                        testRunListeners.forEach(baseListener -> {
                            baseListener.onTestStarted();
                        });
                        TestCaseRunResult result = executeTestCase(context);

                        ResultStatus fixedStatus = result.getStatus();
                        if (fixedStatus == UNKNOWN) {
                            // TODO: Report as a fatal crashed test
                            fixedStatus = ERROR;
                        }

                        TestCaseRunResult fixedResult = result.copy(pool, device, testCaseEvent.getTestCase(), fixedStatus);
                        testRunListeners.forEach(baseListener -> {
                            ResultStatus status = fixedResult.getStatus();
                            if (status == ResultStatus.PASS) {
                                baseListener.onTestSuccessful();
                            } else if (status == ResultStatus.IGNORED) {
                                baseListener.onTestSkipped(fixedResult);
                            } else if (status == ResultStatus.ASSUMPTION_FAILED) {
                                baseListener.onTestAssumptionFailure(fixedResult);
                            } else if (status == ResultStatus.FAIL || status == ERROR) {
                                baseListener.onTestFailed(fixedResult);
                            } else {
                                throw new IllegalStateException("Got unknown status:" + status);
                            }
                        });
                        return fixedResult;
                    });
                } else if (queueOfTestsInPool.hasNoPotentialEventsFor(device)) {
                    break;
                }
            }
        } finally {
            logger.info("Device {} from pool {} finished", device.getSerial(), pool.getName());
            deviceCountDownLatch.countDown();
        }
    }

    @Nullable
    private static TestCaseRunResult executeTestCase(TestCaseRunRuleContext context) {
        AndroidTestRunFactory androidTestRunFactory = TestRunFactoryInjector.testRunFactory(context.getConfiguration());
        PreregisteringLatch workCountdownLatch = new PreregisteringLatch();
        try {
            AtomicReference<ResultStatus> testStatus = new AtomicReference<>(UNKNOWN);
            try {
                AndroidInstrumentedTestRun testRun = androidTestRunFactory.createTestRun(context, context.getTestCaseEvent(),
                        (AndroidDevice) context.getDevice(),
                        context.getPool(),
                        testStatus,
                        workCountdownLatch);
                workCountdownLatch.finalizeRegistering();
                return testRun.execute();
            } finally {
                workCountdownLatch.await(15, TimeUnit.SECONDS);
            }
        } catch (Throwable e) {
            logger.error("Exception during test case execution", e);

            String stackTrace = traceAsStream(e);
            return new TestCaseRunResult(
                    context.getPool(), context.getDevice(),
                    context.getTestCaseEvent().getTestCase(),
                    ERROR,
                    stackTrace,
                    0,
                    0,
                    Collections.emptyMap(),
                    null,
                    emptyList());
        }
    }

    private static String traceAsStream(Throwable e) {
        ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
        PrintStream printStream = new PrintStream(new BufferedOutputStream(byteStream));
        e.printStackTrace(printStream);

        return byteStream.toString();
    }
}
