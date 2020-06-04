/*
 * Copyright 2018 TarCV
 * Copyright 2015 Shazam Entertainment Limited
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

public interface ProgressReporter {

    void start();

    void stop();

    void addPoolProgress(Pool pool, PoolProgressTracker poolProgressTracker);

    PoolProgressTracker getProgressTrackerFor(Pool pool);

    /**
     * The time tests have been executing so far.
     *
     * @return the execution time in millis
     */
    long millisSinceTestsStarted();

    int getFailures();

    float getProgress();

    boolean requestRetry(Pool pool, TestCaseEvent testCaseEvent);

    void recordFailedTestCase(Pool pool, TestCaseEvent testCase);

    int getTestFailuresCount(Pool pool, TestCaseEvent testCase);
}
