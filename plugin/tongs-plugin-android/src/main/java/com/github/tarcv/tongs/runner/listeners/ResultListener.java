/*
 * Copyright 2019 TarCV
 * Copyright 2016 Shazam Entertainment Limited
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License.
 *
 * You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */

package com.github.tarcv.tongs.runner.listeners;

import com.android.ddmlib.testrunner.TestIdentifier;
import com.github.tarcv.tongs.model.Device;
import com.github.tarcv.tongs.model.Pool;
import com.github.tarcv.tongs.model.TestCaseEvent;
import com.github.tarcv.tongs.runner.PreregisteringLatch;
import com.github.tarcv.tongs.summary.ResultStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static com.github.tarcv.tongs.summary.ResultStatus.*;
import static com.google.common.base.Preconditions.checkNotNull;

// TODO: Return ResultStatus.ERROR when necessary
public class ResultListener extends BaseListener {
    private static final Logger logger = LoggerFactory.getLogger(ResultListener.class);
    private final TestCaseEvent currentTestCaseEvent;
    private TestIdentifier startedTest;
    private TestIdentifier failedTest;

    private final AtomicReference<ResultStatus> testStatus;

    public ResultListener(TestCaseEvent currentTestCaseEvent,
                          AtomicReference<ResultStatus> testStatus,
                          PreregisteringLatch latch) {
        super(latch);
        checkNotNull(currentTestCaseEvent);
        checkNotNull(testStatus);
        checkNotNull(latch);
        this.testStatus = testStatus;
        this.currentTestCaseEvent = currentTestCaseEvent;
    }

    private void setStatus(ResultStatus newStatus) {
        if (!testStatus.compareAndSet(UNKNOWN, newStatus)) {
            logger.warn("Tried to set run status for {}#{} twice. Falling back to FAILED",
                    currentTestCaseEvent.getTestClass(), currentTestCaseEvent.getTestMethod());
            testStatus.set(FAIL);
        }
    }

    @Override
    public void testRunStarted(String runName, int testCount) {

    }

    @Override
    public void testStarted(TestIdentifier test) {
        startedTest = test;
    }

    @Override
    public void testFailed(TestIdentifier test, String trace) {
        setStatus(FAIL);
    }

    @Override
    public void testAssumptionFailure(TestIdentifier test, String trace) {
        setStatus(ASSUMPTION_FAILED);
    }

    @Override
    public void testIgnored(TestIdentifier test) {
        setStatus(IGNORED);
    }

    @Override
    public void testEnded(TestIdentifier test, Map<String, String> testMetrics) {
        testStatus.compareAndSet(UNKNOWN, PASS);
    }

    @Override
    public void testRunFailed(String errorMessage) {
        try {
            setStatus(IGNORED);
        } finally {
            onWorkFinished();
        }
    }

    @Override
    public void testRunStopped(long elapsedTime) {

    }

    @Override
    public void testRunEnded(long elapsedTime, Map<String, String> runMetrics) {
        onWorkFinished();
    }
}
