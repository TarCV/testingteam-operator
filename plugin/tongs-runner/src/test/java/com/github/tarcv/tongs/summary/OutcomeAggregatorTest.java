/*
 * Copyright 2020 TarCV
 * Copyright 2018 Shazam Entertainment Limited
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License.
 *
 * You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */

package com.github.tarcv.tongs.summary;

import com.github.tarcv.tongs.api.devices.Device;
import com.github.tarcv.tongs.api.devices.Pool;
import com.github.tarcv.tongs.api.run.ResultStatus;
import com.github.tarcv.tongs.api.result.StackTrace;
import com.github.tarcv.tongs.api.result.TestCaseRunResult;
import org.junit.Test;

import static com.github.tarcv.tongs.api.devices.Pool.Builder.aDevicePool;
import static com.github.tarcv.tongs.api.result.TestCaseRunResult.NO_TRACE;
import static com.github.tarcv.tongs.api.result.TestCaseRunResult.aTestResult;
import static com.github.tarcv.tongs.summary.PoolSummary.Builder.aPoolSummary;
import static com.github.tarcv.tongs.api.run.ResultStatus.*;
import static com.github.tarcv.tongs.summary.Summary.Builder.aSummary;
import static java.util.Arrays.asList;
import static java.util.Collections.singleton;
import static java.util.Collections.singletonList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

public class OutcomeAggregatorTest {
    private final Pool pool = aDevicePool().addDevice(Device.TEST_DEVICE).build();

    @Test
    public void returnsFalseIfThereAreFatalCrashedTests() {
        Summary summary = aSummary()
                .addFatalCrashedTest(aTestResult("com.example.FatalCrashedTest", "testMethod", ERROR, NO_TRACE))
                .addPoolSummary(aPoolSummary()
                        .withPoolName("pool")
                        .addTestResults(singleton(TestCaseRunResult.Companion.aTestResult("com.example.SuccessfulTest", "testMethod", ResultStatus.ERROR,
                                singletonList(new StackTrace("error", "", "error")))))
                        .build())
                .build();

        boolean successful = new OutcomeAggregator().aggregate(summary);

        assertThat(successful, equalTo(false));
    }

    @Test
    public void returnsTrueIfThereAreOnlyPassedAndIgnoredTests() {
        Summary summary = aSummary()
                .addIgnoredTest(aTestResult("com.example.IgnoredTest", "testMethod", IGNORED, NO_TRACE))
                .addPoolSummary(aPoolSummary()
                        .withPoolName("pool")
                        .addTestResults(asList(
                                TestCaseRunResult.Companion.aTestResult("com.example.SuccessfulTest", "testMethod", PASS, NO_TRACE),
                                TestCaseRunResult.Companion.aTestResult("com.example.IgnoredTest", "testMethod", IGNORED, NO_TRACE)
                        ))
                        .build())
                .build();

        boolean successful = new OutcomeAggregator().aggregate(summary);

        assertThat(successful, equalTo(true));
    }
}