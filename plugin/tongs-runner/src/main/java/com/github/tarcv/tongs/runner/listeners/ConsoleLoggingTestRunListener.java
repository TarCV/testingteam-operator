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
package com.github.tarcv.tongs.runner.listeners;

import com.github.tarcv.tongs.model.TestCase;
import com.github.tarcv.tongs.runner.ProgressReporter;

import com.github.tarcv.tongs.runner.TestCaseFailed;
import com.github.tarcv.tongs.runner.TestCaseSkipped;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.SimpleDateFormat;
import java.util.Date;

import static java.lang.String.format;

@SuppressWarnings("UseOfSystemOutOrSystemErr")
class ConsoleLoggingTestRunListener implements TongsTestListener {
    private static final Logger logger = LoggerFactory.getLogger(ConsoleLoggingTestRunListener.class);
    private static final SimpleDateFormat TEST_TIME = new SimpleDateFormat("mm.ss");
    private static final String PERCENT = "%02d%%";
    private final String serial;
    private final String modelName;
    private final ProgressReporter progressReporter;
    private final String testPackage;
    private final TestCase test;

    ConsoleLoggingTestRunListener(String testPackage,
                                  TestCase startedTest, String serial,
                                  String modelName,
                                  ProgressReporter progressReporter) {
        this.test = startedTest;
        this.serial = serial;
        this.modelName = modelName;
        this.progressReporter = progressReporter;
        this.testPackage = testPackage;
    }

    @Override
    public void onTestStarted() {
        System.out.println(format("%s %s %s %s [%s] %s", runningTime(), progress(), failures(), modelName,
                serial, testCase(test)));
    }

    @Override
    public void onTestFailed(TestCaseFailed failureResult) {
        System.out.println(format("%s %s %s %s [%s] Failed %s\n %s", runningTime(), progress(), failures(), modelName,
                serial, testCase(test), failureResult.getStackTrace()));
    }

    @Override
    public void onTestAssumptionFailure(TestCaseSkipped skipped) {
        logger.debug("test={}", testCase(test));
        logger.debug("assumption failure {}", skipped.getStackTrace());
    }

    @Override
    public void onTestSkipped(TestCaseSkipped skipped) {
        logger.debug("ignored test {} {}", testCase(test), skipped.getStackTrace());
    }


    @Override
    public void onTestSuccessful() {

    }

    private String runningTime() {
        return TEST_TIME.format(new Date(progressReporter.millisSinceTestsStarted()));
    }

    private String testCase(TestCase test) {
        return String.valueOf(test).replaceAll(testPackage, "");
    }

    private String progress() {
        int progress = (int) (progressReporter.getProgress() * 100.0);
        return String.format(PERCENT, progress);
    }

    private int failures() {
        return progressReporter.getFailures();
    }
}
