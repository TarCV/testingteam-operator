/*
 * Copyright 2018 TarCV
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
import com.github.tarcv.tongs.device.DeviceTestFilesCleaner;
import com.github.tarcv.tongs.model.Device;
import com.github.tarcv.tongs.model.Pool;
import com.github.tarcv.tongs.model.TestCaseEvent;
import com.github.tarcv.tongs.runner.PreregisteringLatch;
import com.github.tarcv.tongs.runner.TestRetryer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

import static com.google.common.base.Preconditions.checkNotNull;

public class RetryListener extends BaseListener {
    private static final Logger logger = LoggerFactory.getLogger(RetryListener.class);
    private final Device device;
    private final TestCaseEvent currentTestCaseEvent;
    private final TestRetryer testRetryer;
    private final Pool pool;
    private final DeviceTestFilesCleaner deviceTestFilesCleaner;
    private TestIdentifier startedTest;
    private TestIdentifier failedTest;

    public RetryListener(Pool pool,
                         Device device,
                         TestCaseEvent currentTestCaseEvent,
                         TestRetryer testRetryer,
                         DeviceTestFilesCleaner deviceTestFilesCleaner,
                         PreregisteringLatch latch) {
        super(latch);
        checkNotNull(device);
        checkNotNull(currentTestCaseEvent);
        checkNotNull(pool);
        checkNotNull(latch);
        this.testRetryer = testRetryer;
        this.device = device;
        this.currentTestCaseEvent = currentTestCaseEvent;
        this.pool = pool;
        this.deviceTestFilesCleaner = deviceTestFilesCleaner;
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
        failedTest = test;
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
        try {
            logger.info("Test run failed due to a fatal error: " + errorMessage);
            if (failedTest == null) {
                logger.info("Reschedule all started tests by this test run");
                rescheduleTestExecution(startedTest);
            }
        } finally {
            onWorkFinished();
        }
    }

    @Override
    public void testRunStopped(long elapsedTime) {

    }

    @Override
    public void testRunEnded(long elapsedTime, Map<String, String> runMetrics) {
        try {
            if (failedTest != null) {
                rescheduleTestExecution(failedTest);
            }
        } finally {
            onWorkFinished();
        }
    }

    private void rescheduleTestExecution(TestIdentifier test) {
        if (testRetryer.rescheduleTestExecution(test, currentTestCaseEvent)) {
            logger.info("Test " + test.toString() + " enqueued again into pool:" + pool.getName());
            removeFailureTraceFiles(test);
        } else {
            logger.info("Test " + test.toString() + " failed on device " + device.getSafeSerial()
                    + " but retry is not allowed.");
        }
    }

    private void removeFailureTraceFiles(TestIdentifier test) {
        boolean isDeleted = deviceTestFilesCleaner.deleteTraceFiles(test);
        if (!isDeleted) {
            logger.warn("Failed to remove a trace filed for a failed but enqueued again test");
        }
    }
}
