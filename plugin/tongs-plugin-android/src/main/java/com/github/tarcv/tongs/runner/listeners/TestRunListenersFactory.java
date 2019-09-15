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
import com.github.tarcv.tongs.runner.*;
import com.github.tarcv.tongs.system.io.TestCaseFileManager;
import com.google.gson.Gson;
import com.github.tarcv.tongs.TongsConfiguration;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static com.github.tarcv.tongs.model.Diagnostics.SCREENSHOTS;
import static com.github.tarcv.tongs.model.Diagnostics.VIDEO;
import static java.util.Arrays.asList;

public class TestRunListenersFactory {

    private final TongsConfiguration configuration;
    private final Gson gson;

    public TestRunListenersFactory(TongsConfiguration configuration,
                                   Gson gson) {
        this.configuration = configuration;
        this.gson = gson;
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

    private BaseListener getAndroidXmlTestRunListener(TestCaseFileManager fileManager,
                                                      PreregisteringLatch latch) {
        AndroidXmlTestRunListener xmlTestRunListener = new AndroidXmlTestRunListener(fileManager);
        return new BaseListenerWrapper(latch, xmlTestRunListener);
    }

    private BaseListener getCoverageTestRunListener(TongsConfiguration configuration,
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
