/*
 * Copyright 2019 TarCV
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
package com.github.tarcv.tongs.runner.listeners;

import com.android.ddmlib.testrunner.TestIdentifier;
import com.android.ddmlib.testrunner.XmlTestRunListener;
import com.github.tarcv.tongs.model.Device;
import com.github.tarcv.tongs.model.Pool;
import com.github.tarcv.tongs.model.TestCaseEvent;
import com.github.tarcv.tongs.runner.AndroidDeviceTestRunner;
import com.github.tarcv.tongs.runner.ProgressReporter;
import com.github.tarcv.tongs.system.io.FileManager;
import com.github.tarcv.tongs.system.io.FileType;
import com.google.common.collect.ImmutableMap;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nonnull;
import java.io.File;
import java.util.Map;

import static com.github.tarcv.tongs.model.TestCaseEvent.newTestCase;
import static com.github.tarcv.tongs.summary.TestResult.SUMMARY_KEY_TOTAL_FAILURE_COUNT;

public class TongsXmlTestRunListener implements TongsTestListener {
    @Override
    public void onTestStarted() {
        // no op
    }

    @Override
    public void onTestSuccessful() {
        // no op
    }

    @Override
    public void onTestSkipped(@NotNull AndroidDeviceTestRunner.TestCaseSkipped skipResult) {
        onTestFinished();
    }

    @Override
    public void onTestFailed(@NotNull AndroidDeviceTestRunner.TestCaseFailed failureResult) {
        onTestFinished();
    }

    @Override
    public void onTestAssumptionFailure(@NotNull AndroidDeviceTestRunner.TestCaseSkipped skipResult) {
        onTestFinished();
    }

    public void onTestFinished() {
        // TODO:
    }
}
