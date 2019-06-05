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

import com.google.gson.JsonObject;
import com.github.tarcv.tongs.model.Device;
import com.github.tarcv.tongs.model.Pool;
import com.github.tarcv.tongs.model.TestCaseEvent;

import org.jmock.Expectations;
import org.jmock.auto.Mock;
import org.jmock.integration.junit4.JUnitRuleMockery;
import org.junit.Rule;
import org.junit.Test;

import static com.github.tarcv.tongs.model.Device.Builder.aDevice;
import static com.github.tarcv.tongs.model.Pool.Builder.aDevicePool;
import static com.github.tarcv.tongs.model.TestCaseEvent.newTestCase;
import static com.github.tarcv.tongs.runner.FakePoolTestCaseAccumulator.aFakePoolTestCaseAccumulator;
import static com.github.tarcv.tongs.runner.FakeProgressReporterTrackers.aFakeProgressReporterTrackers;
import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;

public class OverallProgressReporterTest {

    @Rule public JUnitRuleMockery mockery = new JUnitRuleMockery();
    @Mock private PoolProgressTracker mockPoolProgressTracker;
    private final FakePoolTestCaseAccumulator fakeTestCasesAccumulator = aFakePoolTestCaseAccumulator();

    private final Device A_DEVICE = aDevice().build();
    private final Pool A_POOL = aDevicePool()
            .addDevice(A_DEVICE)
            .build();
    private final TestCaseEvent A_TEST_CASE = newTestCase("aTestMethod", "aTestClass", false, emptyList(), emptyMap(), new JsonObject());

    private OverallProgressReporter overallProgressReporter;

    @Test
    public void requestRetryIsAllowedIfFailedLessThanPermitted() throws Exception {
        fakeTestCasesAccumulator.thatAlwaysReturns(0);
        overallProgressReporter = new OverallProgressReporter(1, 1,
                aFakeProgressReporterTrackers().thatAlwaysReturns(mockPoolProgressTracker),
                fakeTestCasesAccumulator);

        mockery.checking(new Expectations() {{
            oneOf(mockPoolProgressTracker).trackTestEnqueuedAgain();
        }});

        overallProgressReporter.requestRetry(A_POOL, A_TEST_CASE);
    }

    @Test
    public void requestRetryIsNotAllowedIfFailedMoreThanPermitted() throws Exception {
        fakeTestCasesAccumulator.thatAlwaysReturns(2);
        overallProgressReporter = new OverallProgressReporter(1, 1,
                aFakeProgressReporterTrackers().thatAlwaysReturns(mockPoolProgressTracker),
                fakeTestCasesAccumulator);

        mockery.checking(new Expectations() {{
            never(mockPoolProgressTracker).trackTestEnqueuedAgain();
        }});

        overallProgressReporter.requestRetry(A_POOL, A_TEST_CASE);
    }

}
