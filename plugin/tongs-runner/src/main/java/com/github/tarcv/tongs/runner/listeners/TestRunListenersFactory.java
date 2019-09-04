/*
 * Copyright 2019 TarCV
 * Copyright 2015 Shazam Entertainment Limited
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License.
 *
 * You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */

package com.github.tarcv.tongs.runner.listeners;

import com.android.ddmlib.testrunner.TestIdentifier;
import com.github.tarcv.tongs.model.*;
import com.github.tarcv.tongs.runner.*;
import com.github.tarcv.tongs.system.io.TestCaseFileManager;
import com.google.gson.Gson;
import com.github.tarcv.tongs.Configuration;
import com.github.tarcv.tongs.TongsConfiguration;
import com.github.tarcv.tongs.device.DeviceTestFilesCleanerImpl;
import com.github.tarcv.tongs.system.io.FileManager;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static com.github.tarcv.tongs.model.Diagnostics.SCREENSHOTS;
import static com.github.tarcv.tongs.model.Diagnostics.VIDEO;
import static java.util.Arrays.asList;

public class TestRunListenersFactory {

    private final Configuration configuration;
    private final FileManager fileManager;
    private final Gson gson;

    public TestRunListenersFactory(Configuration configuration,
                                   FileManager fileManager,
                                   Gson gson) {
        this.configuration = configuration;
        this.fileManager = fileManager;
        this.gson = gson;
    }

    public List<TongsTestListener> createTongsListners(TestCaseEvent testCase,
                                                       Device device,
                                                       Pool pool,
                                                       ProgressReporter progressReporter,
                                                       TestCaseEventQueue testCaseEventQueue,
                                                       TongsConfiguration.TongsIntegrationTestRunType tongsIntegrationTestRunType) {
        TestIdentifier testIdentifier = new TestIdentifier(testCase.getTestClass(), testCase.getTestMethod());
        final List<TongsTestListener> normalListeners = asList(
                new ProgressTestRunListener(pool, progressReporter),
                new ConsoleLoggingTestRunListener(configuration.getTestPackage(), testIdentifier, device.getSerial(),
                        device.getModelName(), progressReporter),
                new SlowWarningTestRunListener(testIdentifier),
                getTongsAdditionalXmlTestRunListener(fileManager, configuration.getOutput(), pool, device, testCase, progressReporter),
                buildRetryListener(testCase, device, pool, progressReporter, testCaseEventQueue)
        );
        if (tongsIntegrationTestRunType == TongsConfiguration.TongsIntegrationTestRunType.RECORD_LISTENER_EVENTS) {
            ArrayList<TongsTestListener> testListeners = new ArrayList<>(normalListeners);
            testListeners.add(new RecordingTestRunListener(device, testIdentifier.toString(), false));
            return Collections.unmodifiableList(testListeners);
        } else {
            return normalListeners;
        }
    }

    public List<BaseListener> createAndroidListeners(TongsTestCaseContext context,
                                                     AtomicReference<ResultListener.Status> testStatus,
                                                     PreregisteringLatch latch) {
        TestCaseEvent testCase = context.getTestCaseEvent();
        AndroidDevice device = (AndroidDevice) context.getDevice();
        Pool pool = context.getPool();
        TestCaseFileManager fileManager = context.getFileManager();
        return asList(
                new ResultListener(testCase, testStatus, latch),
                getAndroidXmlTestRunListener(fileManager, latch),
                new LogCatTestRunListener(gson, fileManager, pool, device, latch),
                getScreenTraceTestRunListener(fileManager, pool, device, latch),
                getCoverageTestRunListener(configuration, device, fileManager, pool, testCase, latch));
    }

    private TongsTestListener buildRetryListener(TestCaseEvent testCase,
                                                 Device device,
                                                 Pool pool,
                                                 ProgressReporter progressReporter,
                                                 TestCaseEventQueue testCaseEventQueue) {
        TestRetryerImpl testRetryer = new TestRetryerImpl(progressReporter, pool, testCaseEventQueue);
        DeviceTestFilesCleanerImpl deviceTestFilesCleaner = new DeviceTestFilesCleanerImpl(fileManager, pool, device);
        return new RetryListener(pool, device, testCase, testRetryer, deviceTestFilesCleaner);
    }

    private BaseListener getAndroidXmlTestRunListener(TestCaseFileManager fileManager,
                                                      PreregisteringLatch latch) {
        AndroidXmlTestRunListener xmlTestRunListener = new AndroidXmlTestRunListener(fileManager);
        return new BaseListenerWrapper(latch, xmlTestRunListener);
    }

    private TongsTestListener getTongsAdditionalXmlTestRunListener(FileManager fileManager,
                                                                   File output,
                                                                   Pool pool,
                                                                   Device device,
                                                                   TestCaseEvent testCase,
                                                                   ProgressReporter progressReporter) {
        return new TongsXmlTestRunListener(); // TODO: move removed parts from TestCaseFileManager to this listener
    }

    private BaseListener getCoverageTestRunListener(Configuration configuration,
                                                    AndroidDevice device,
                                                    TestCaseFileManager fileManager,
                                                    Pool pool,
                                                    TestCaseEvent testCase,
                                                    PreregisteringLatch latch) {
        if (configuration.isCoverageEnabled()) {
            return new CoverageListener(device, fileManager, pool, testCase, latch);
        }
        return new BaseListenerWrapper(null, new NoOpITestRunListener());
    }

    private BaseListener getScreenTraceTestRunListener(TestCaseFileManager fileManager, Pool pool, AndroidDevice device, PreregisteringLatch latch) {
        if (VIDEO.equals(device.getSupportedVisualDiagnostics())) {
            return new ScreenRecorderTestRunListener(fileManager, pool, device, latch);
        }

        if (SCREENSHOTS.equals(device.getSupportedVisualDiagnostics()) && configuration.canFallbackToScreenshots()) {
            return new ScreenCaptureTestRunListener(fileManager, pool, device, latch);
        }

        return new BaseListenerWrapper(null, new NoOpITestRunListener());
    }}
