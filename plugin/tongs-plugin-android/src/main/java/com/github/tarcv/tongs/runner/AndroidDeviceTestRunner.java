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

import com.android.ddmlib.DdmPreferences;
import com.android.ddmlib.IDevice;
import com.android.ddmlib.testrunner.TestIdentifier;
import com.github.tarcv.tongs.injector.listeners.TestRunListenersFactoryInjector;
import com.github.tarcv.tongs.injector.runner.TestRunFactoryInjector;
import com.github.tarcv.tongs.model.*;
import com.github.tarcv.tongs.runner.listeners.ResultListener.Status;
import com.github.tarcv.tongs.runner.listeners.TongsTestListener;
import com.github.tarcv.tongs.system.adb.Installer;
import com.github.tarcv.tongs.system.io.RemoteFileManager;
import com.github.tarcv.tongs.system.io.TestCaseFileManager;
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

import static com.github.tarcv.tongs.device.DeviceUtilsKt.clearLogcat;
import static com.github.tarcv.tongs.injector.system.FileManagerInjector.fileManager;
import static com.github.tarcv.tongs.runner.listeners.ResultListener.Status.*;

public class AndroidDeviceTestRunner implements Runnable {
    private static final Logger logger = LoggerFactory.getLogger(AndroidDeviceTestRunner.class);

    private final Installer installer;
    private final Pool pool;
    private final AndroidDevice device;
    private final TestCaseEventQueue queueOfTestsInPool;
    private final CountDownLatch deviceCountDownLatch;
    private final ProgressReporter progressReporter;

    public AndroidDeviceTestRunner(Installer installer,
                                   Pool pool,
                                   Device device,
                                   TestCaseEventQueue queueOfTestsInPool,
                                   CountDownLatch deviceCountDownLatch,
                                   ProgressReporter progressReporter) {
        this.installer = installer;
        this.pool = pool;
        this.device = (AndroidDevice) device;
        this.queueOfTestsInPool = queueOfTestsInPool;
        this.deviceCountDownLatch = deviceCountDownLatch;
        this.progressReporter = progressReporter;
    }

    @Override
    public void run() {
        IDevice deviceInterface = device.getDeviceInterface();
        try {
            DdmPreferences.setTimeOut(30000);
            installer.prepareInstallation(deviceInterface);
            // For when previous run crashed/disconnected and left files behind
            RemoteFileManager.removeRemoteDirectory(deviceInterface);
            RemoteFileManager.createRemoteDirectory(deviceInterface);
            RemoteFileManager.createCoverageDirectory(deviceInterface);
            clearLogcat(deviceInterface);

            while (true) {
                TestCaseEventQueue.TestCaseTask testCaseTask = queueOfTestsInPool.pollForDevice(device, 10);
                if (testCaseTask != null) {
                    testCaseTask.doWork(testCaseEvent -> {
                        TestCaseFileManager testCaseFileManager = new TestCaseFileManager(FileManagerInjector.fileManager(), pool, device, testCaseEvent);
                        TongsTestCaseContext context = new TongsTestCaseContext<AndroidDevice>(
                                ConfigurationInjector.configuration(), testCaseFileManager,
                                pool, device, testCaseEvent);

                        List<TongsTestListener> testRunListeners = new ArrayList<>();
                        testRunListeners.addAll(TestRunListenersFactoryInjector.testRunListenersFactory().createTongsListners(
                                testCaseEvent,
                                device,
                                pool,
                                progressReporter,
                                queueOfTestsInPool,
                                ConfigurationInjector.configuration().getTongsIntegrationTestRunType()));

                        TestIdentifier identifier = new TestIdentifier(testCaseEvent.getTestClass(), testCaseEvent.getTestMethod());

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
