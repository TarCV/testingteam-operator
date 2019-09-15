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

import com.github.tarcv.tongs.TongsConfiguration;
import com.github.tarcv.tongs.device.DeviceTestFilesCleanerImpl;
import com.github.tarcv.tongs.model.*;
import com.github.tarcv.tongs.runner.PreregisteringLatch;
import com.github.tarcv.tongs.runner.ProgressReporter;
import com.github.tarcv.tongs.runner.TestRetryerImpl;
import com.github.tarcv.tongs.runner.TongsTestCaseContext;
import com.github.tarcv.tongs.system.io.FileManager;
import com.github.tarcv.tongs.system.io.TestCaseFileManager;
import com.google.gson.Gson;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static com.github.tarcv.tongs.model.Diagnostics.SCREENSHOTS;
import static com.github.tarcv.tongs.model.Diagnostics.VIDEO;
import static java.util.Arrays.asList;

public class TestRunListenersFactoryTongs {

    private final TongsConfiguration configuration;
    private final FileManager fileManager;
    private final Gson gson;

    public TestRunListenersFactoryTongs(TongsConfiguration configuration,
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
        TestCase testIdentifier = new TestCase(testCase.getTestClass(), testCase.getTestMethod());
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

    private TongsTestListener buildRetryListener(TestCaseEvent testCase,
                                                 Device device,
                                                 Pool pool,
                                                 ProgressReporter progressReporter,
                                                 TestCaseEventQueue testCaseEventQueue) {
        TestRetryerImpl testRetryer = new TestRetryerImpl(progressReporter, pool, testCaseEventQueue);
        DeviceTestFilesCleanerImpl deviceTestFilesCleaner = new DeviceTestFilesCleanerImpl(fileManager, pool, device);
        return new RetryListener(pool, device, testCase, testRetryer, deviceTestFilesCleaner);
    }

    private TongsTestListener getTongsAdditionalXmlTestRunListener(FileManager fileManager,
                                                                   File output,
                                                                   Pool pool,
                                                                   Device device,
                                                                   TestCaseEvent testCase,
                                                                   ProgressReporter progressReporter) {
        return new TongsXmlTestRunListener(); // TODO: move removed parts from TestCaseFileManager to this listener
    }
}
