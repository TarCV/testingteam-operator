/*
 * Copyright 2019 TarCV
 * Copyright 2018 Shazam Entertainment Limited
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License.
 *
 * You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */

package com.github.tarcv.tongs.runner.listeners;

import com.android.ddmlib.testrunner.TestIdentifier;
import com.github.tarcv.tongs.api.devices.Pool;
import com.github.tarcv.tongs.model.*;
import com.github.tarcv.tongs.api.run.TestCaseEvent;
import com.github.tarcv.tongs.runner.PreregisteringLatch;
import com.github.tarcv.tongs.api.result.TestCaseFile;
import com.github.tarcv.tongs.system.io.RemoteFileManager;

import com.github.tarcv.tongs.api.result.TestCaseFileManager;
import com.github.tarcv.tongs.api.testcases.TestCase;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.Map;

import static com.github.tarcv.tongs.api.result.StandardFileTypes.COVERAGE;

public class CoverageListener extends BaseListener {

    private final AndroidDevice device;
    private final TestCaseFileManager fileManager;
    private final Pool pool;
    private final Logger logger = LoggerFactory.getLogger(CoverageListener.class);
    private final TestCaseEvent testCase;

    @NotNull
    public final TestCaseFile coverageFile;

    public CoverageListener(AndroidDevice device, TestCaseFileManager fileManager, Pool pool, TestCaseEvent testCase, PreregisteringLatch latch) {
        super(latch);
        this.device = device;
        this.fileManager = fileManager;
        this.pool = pool;
        this.testCase = testCase;
        this.coverageFile = new TestCaseFile(fileManager, COVERAGE, "");
    }

    @Override
    public void testRunStarted(String runName, int testCount) {
    }

    @Override
    public void testStarted(TestIdentifier test) {
    }

    @Override
    public void testFailed(TestIdentifier test, String trace) {
    }

    @Override
    public void testAssumptionFailure(TestIdentifier test, String trace) {
    }

    @Override
    public void testIgnored(TestIdentifier test) {
    }

    @Override
    public void testEnded(TestIdentifier test, Map<String, String> testMetrics) {
    }

    @Override
    public void testRunFailed(String errorMessage) {
    }

    @Override
    public void testRunStopped(long elapsedTime) {
    }

    @Override
    public void testRunEnded(long elapsedTime, Map<String, String> runMetrics) {
        try {
            TestCase testIdentifier = new TestCase(testCase.getTestMethod(), testCase.getTestClass());
            final String remoteFile = RemoteFileManager.getCoverageFileName(testIdentifier);
            final File file = coverageFile.create();
            try {
                device.getDeviceInterface().pullFile(remoteFile, file.getAbsolutePath());
            } catch (Exception e) {
                logger.error("Something went wrong while pulling coverage file", e);
            }
        } finally {
            onWorkFinished();
        }
    }
}
