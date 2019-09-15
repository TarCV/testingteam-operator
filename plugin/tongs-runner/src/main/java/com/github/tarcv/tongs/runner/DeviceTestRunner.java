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
package com.github.tarcv.tongs.runner;

import com.github.tarcv.tongs.Configuration;
import com.github.tarcv.tongs.injector.ConfigurationInjector;
import com.github.tarcv.tongs.injector.runner.TestRunFactoryInjector;
import com.github.tarcv.tongs.injector.system.FileManagerInjector;
import com.github.tarcv.tongs.model.*;
import com.github.tarcv.tongs.runner.listeners.ResultListener.Status;
import com.github.tarcv.tongs.runner.listeners.TongsTestListener;
import com.github.tarcv.tongs.system.io.TestCaseFileManager;
import com.github.tarcv.tongs.system.io.TestCaseFileManagerImpl;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static com.github.tarcv.tongs.injector.listeners.TestRunListenersTongsFactoryInjector.testRunListenersTongsFactory;
import static com.github.tarcv.tongs.runner.listeners.ResultListener.Status.*;

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
                        TestCaseFileManager testCaseFileManager = new TestCaseFileManagerImpl(FileManagerInjector.fileManager(), pool, device, testCaseEvent);
                        Configuration configuration = ConfigurationInjector.configuration();
                        TongsTestCaseContext context = new TongsTestCaseContext(
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

                        TestCase identifier = new TestCase(testCaseEvent.getTestClass(), testCaseEvent.getTestMethod());

                        // TODO: Add some defensive code
                        testRunListeners.forEach(baseListener -> {
                            baseListener.onTestStarted();
                        });
                        TestCaseRunResult result = executeTestCase(context);
                        testRunListeners.forEach(baseListener -> {
                            if (result instanceof TestCaseSuccessful) {
                                // no op
                            } else if (result instanceof TestCaseSkipped) {
                                // TODO: support assumption failed
                                baseListener.onTestSkipped((TestCaseSkipped) result);
                            } else {
                                TestCaseFailed failureResult;
                                if (result instanceof TestCaseFailed) {
                                    failureResult = (TestCaseFailed) result;
                                } else {
                                    failureResult = new TestCaseFailed();
                                }
                                baseListener.onTestFailed(failureResult);
                            }
                        });
                        return null;
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
    private static TestCaseRunResult executeTestCase(TongsTestCaseContext context) {
        AndroidTestRunFactory androidTestRunFactory = TestRunFactoryInjector.testRunFactory(context.getConfiguration());
        PreregisteringLatch workCountdownLatch = new PreregisteringLatch();
        try {
            AtomicReference<Status> testStatus = new AtomicReference<>(UNKNOWN);
            try {
                AndroidInstrumentedTestRun testRun = androidTestRunFactory.createTestRun(context, context.getTestCaseEvent(),
                        (AndroidDevice) context.getDevice(),
                        context.getPool(),
                        testStatus,
                        workCountdownLatch);
                workCountdownLatch.finalizeRegistering();
                testRun.execute();
            } finally {
                workCountdownLatch.await(15, TimeUnit.SECONDS);
            }
            return statusToResultObject(testStatus.get());
        } catch (Throwable e) {
            logger.error("Exception during test case execution", e);

            String stackTrace = traceAsStream(e);
            return new TestCaseFailed(stackTrace);
        }
    }

    private static String traceAsStream(Throwable e) {
        ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
        PrintStream printStream = new PrintStream(new BufferedOutputStream(byteStream));
        e.printStackTrace(printStream);

        return byteStream.toString();
    }

    private static TestCaseRunResult statusToResultObject(Status status) {
        // TODO: remove, TestCaseRunResult should be created by actual test runners
        switch (status) {
            case UNKNOWN:
            case FAILED:
                return new TestCaseFailed();
            case SUCCESSFUL:
                return new TestCaseSuccessful();
            case SKIPPED:
            case ASSUMPTION_FAILED:
                return new TestCaseSkipped();
            default:
                throw new IllegalArgumentException("Unexpected test run state: " + status);
        }
    }


}
