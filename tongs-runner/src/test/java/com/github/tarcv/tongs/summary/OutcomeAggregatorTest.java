/*
 * Copyright 2018 TarCV
 * Copyright 2018 Shazam Entertainment Limited
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License.
 *
 * You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */

package com.github.tarcv.tongs.summary;

import org.junit.Test;

import static com.github.tarcv.tongs.summary.PoolSummary.Builder.aPoolSummary;
import static com.github.tarcv.tongs.summary.Summary.Builder.aSummary;
import static com.github.tarcv.tongs.summary.TestResult.Builder.aTestResult;
import static java.util.Arrays.asList;
import static java.util.Collections.singleton;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

public class OutcomeAggregatorTest {
    @Test
    public void returnsFalseIfThereAreFatalCrashedTests() {
        Summary summary = aSummary()
                .addFatalCrashedTest("com.example.FatalCrashedTest:testMethod")
                .addPoolSummary(aPoolSummary()
                        .withPoolName("pool")
                        .addTestResults(singleton(aTestResult()
                                .withTestClass("com.example.SuccessfulTest")
                                .withTestMethod("testMethod")
                                .withTimeTaken(15.0f)
                                .build()))
                        .build())
                .build();

        boolean successful = new OutcomeAggregator().aggregate(summary);

        assertThat(successful, equalTo(false));
    }

    @Test
    public void returnsTrueIfThereAreOnlyPassedAndIgnoredTests() {
        Summary summary = aSummary()
                .addIgnoredTest("com.example.IgnoredTest:testMethod")
                .addPoolSummary(aPoolSummary()
                        .withPoolName("pool")
                        .addTestResults(asList(
                                aTestResult()
                                        .withTestClass("com.example.SuccessfulTest")
                                        .withTestMethod("testMethod")
                                        .withTimeTaken(15.0f)
                                        .build(),
                                aTestResult()
                                        .withTestClass("com.example.IgnoredTest")
                                        .withTestMethod("testMethod")
                                        .withIgnored(true)
                                        .build()
                        ))
                        .build())
                .build();

        boolean successful = new OutcomeAggregator().aggregate(summary);

        assertThat(successful, equalTo(true));
    }
}