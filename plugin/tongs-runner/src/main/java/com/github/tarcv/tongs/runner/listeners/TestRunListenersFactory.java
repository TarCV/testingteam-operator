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

import com.github.tarcv.tongs.model.*;
import com.github.tarcv.tongs.runner.PreregisteringLatch;
import com.google.gson.Gson;
import com.github.tarcv.tongs.Configuration;
import com.github.tarcv.tongs.TongsConfiguration;
import com.github.tarcv.tongs.device.DeviceTestFilesCleanerImpl;
import com.github.tarcv.tongs.runner.ProgressReporter;
import com.github.tarcv.tongs.runner.TestRetryerImpl;
import com.github.tarcv.tongs.system.io.FileManager;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

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

    public List<BaseListener> createTestListeners(TestCaseEvent testCase,
                                                  AndroidDevice device,
                                                  Pool pool,
                                                  ProgressReporter progressReporter,
                                                  TestCaseEventQueue testCaseEventQueue,
                                                  PreregisteringLatch latch,
                                                  TongsConfiguration.TongsIntegrationTestRunType tongsIntegrationTestRunType) {
        final List<BaseListener> normalListeners = asList(
                new ProgressTestRunListener(pool, progressReporter),
                getTongsXmlTestRunListener(fileManager, configuration.getOutput(), pool, device, testCase, progressReporter, latch),
                new ConsoleLoggingTestRunListener(configuration.getTestPackage(), device.getSerial(),
                        device.getModelName(), progressReporter),
                new LogCatTestRunListener(gson, fileManager, pool, device, latch),
                new SlowWarningTestRunListener(),
                getScreenTraceTestRunListener(fileManager, pool, device, latch),
                buildRetryListener(testCase, device, pool, progressReporter, testCaseEventQueue, latch),
                getCoverageTestRunListener(configuration, device, fileManager, pool, testCase, latch));
        if (tongsIntegrationTestRunType == TongsConfiguration.TongsIntegrationTestRunType.RECORD_LISTENER_EVENTS) {
            ArrayList<BaseListener> testListeners = new ArrayList<>(normalListeners);
            testListeners.add(new RecordingTestRunListener(device, false, latch));
            return Collections.unmodifiableList(testListeners);
        } else {
            return normalListeners;
        }
    }

    private RetryListener buildRetryListener(TestCaseEvent testCase,
                                             Device device,
                                             Pool pool,
                                             ProgressReporter progressReporter,
                                             TestCaseEventQueue testCaseEventQueue,
                                             PreregisteringLatch workCountdownLatch) {
        TestRetryerImpl testRetryer = new TestRetryerImpl(progressReporter, pool, testCaseEventQueue);
        DeviceTestFilesCleanerImpl deviceTestFilesCleaner = new DeviceTestFilesCleanerImpl(fileManager, pool, device);
        return new RetryListener(pool, device, testCase, testRetryer, deviceTestFilesCleaner, workCountdownLatch);
    }

    private BaseListener getTongsXmlTestRunListener(FileManager fileManager,
                                                    File output,
                                                    Pool pool,
                                                    Device device,
                                                    TestCaseEvent testCase,
                                                    ProgressReporter progressReporter,
                                                    PreregisteringLatch latch) {
        TongsXmlTestRunListener xmlTestRunListener = new TongsXmlTestRunListener(fileManager, pool, device, testCase, progressReporter);
        xmlTestRunListener.setReportDir(output);
        return new BaseListenerWrapper(latch, xmlTestRunListener);
    }

    private BaseListener getCoverageTestRunListener(Configuration configuration,
                                                    AndroidDevice device,
                                                    FileManager fileManager,
                                                    Pool pool,
                                                    TestCaseEvent testCase,
                                                    PreregisteringLatch latch) {
        if (configuration.isCoverageEnabled()) {
            return new CoverageListener(device, fileManager, pool, testCase, latch);
        }
        return new BaseListenerWrapper(null, new NoOpITestRunListener());
    }

    private BaseListener getScreenTraceTestRunListener(FileManager fileManager, Pool pool, AndroidDevice device, PreregisteringLatch latch) {
        if (VIDEO.equals(device.getSupportedVisualDiagnostics())) {
            return new ScreenRecorderTestRunListener(fileManager, pool, device, latch);
        }

        if (SCREENSHOTS.equals(device.getSupportedVisualDiagnostics()) && configuration.canFallbackToScreenshots()) {
            return new ScreenCaptureTestRunListener(fileManager, pool, device, latch);
        }

        return new BaseListenerWrapper(null, new NoOpITestRunListener());
    }
}
