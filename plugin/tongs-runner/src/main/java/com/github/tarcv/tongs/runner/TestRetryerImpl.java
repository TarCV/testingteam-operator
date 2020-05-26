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

package com.github.tarcv.tongs.runner;

import com.github.tarcv.tongs.api.devices.Pool;
import com.github.tarcv.tongs.api.run.TestCaseEvent;
import com.github.tarcv.tongs.model.TestCaseEventQueue;

import static com.github.tarcv.tongs.api.run.TestCaseEvent.newTestCase;

public class TestRetryerImpl implements TestRetryer {
    private final ProgressReporter progressReporter;
    private final Pool pool;
    private final TestCaseEventQueue queueOfTestsInPool;

    public TestRetryerImpl(ProgressReporter progressReporter, Pool pool, TestCaseEventQueue queueOfTestsInPool) {
        this.progressReporter = progressReporter;
        this.pool = pool;
        this.queueOfTestsInPool = queueOfTestsInPool;
    }

    @Override
    public boolean rescheduleTestExecution(TestCaseEvent testCaseEvent) {
        progressReporter.recordFailedTestCase(pool, testCaseEvent);
        if (progressReporter.requestRetry(pool, testCaseEvent)) {
            queueOfTestsInPool.offer(testCaseEvent);
            return true;
        }
        return false;
    }
}
