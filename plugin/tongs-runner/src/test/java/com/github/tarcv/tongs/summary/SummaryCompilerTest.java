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

package com.github.tarcv.tongs.summary;

import com.github.tarcv.tongs.TongsConfiguration;
import com.github.tarcv.tongs.model.Device;
import com.github.tarcv.tongs.model.Pool;
import com.github.tarcv.tongs.model.TestCase;
import com.github.tarcv.tongs.model.TestCaseEvent;
import com.github.tarcv.tongs.runner.TestCaseRunResult;
import com.google.common.collect.ImmutableMap;
import com.google.gson.JsonObject;
import org.jetbrains.annotations.NotNull;
import org.jmock.Expectations;
import org.jmock.auto.Mock;
import org.jmock.integration.junit4.JUnitRuleMockery;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.github.tarcv.tongs.model.Pool.Builder.aDevicePool;
import static com.github.tarcv.tongs.model.TestCaseEvent.newTestCase;
import static com.github.tarcv.tongs.summary.FakeDeviceTestFilesRetriever.aFakeDeviceTestFilesRetriever;
import static com.github.tarcv.tongs.summary.TestResult.SUMMARY_KEY_TOTAL_FAILURE_COUNT;
import static com.google.common.collect.Lists.newArrayList;
import static java.util.Collections.emptyList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;

public class SummaryCompilerTest {
    @Rule
    public JUnitRuleMockery mockery = new JUnitRuleMockery();
    @Mock
    private TongsConfiguration mockConfiguration;

    private final FakeDeviceTestFilesRetriever fakeDeviceTestFilesRetriever = aFakeDeviceTestFilesRetriever();
    private SummaryCompiler summaryCompiler;

    private final Pool devicePool = aDevicePool()
            .addDevice(Device.TEST_DEVICE)
            .build();
    private final Collection<Pool> devicePools = newArrayList(
            devicePool
    );

    private final TestCaseRunResult firstCompletedTest = TestCaseRunResult.Companion.aTestResult(
            devicePool, Device.TEST_DEVICE,
            "com.example.CompletedClassTest",
            "doesJobProperly",
            ResultStatus.PASS,
            "");
    private final TestCaseRunResult secondCompletedTest = TestCaseRunResult.Companion.aTestResult(
            devicePool, Device.TEST_DEVICE,
            "com.example.CompletedClassTest2",
            "doesJobProperly",
            ResultStatus.PASS,
            ""
    );

    private final HashMap<String, String> testMetricsForFailedTest = new HashMap<String, String>() {{
        put(SUMMARY_KEY_TOTAL_FAILURE_COUNT, "10");
    }};
    private final List<TestCaseRunResult> testResults = newArrayList(
            firstCompletedTest,
            secondCompletedTest,
            TestCaseRunResult.Companion.aTestResult(devicePool, Device.TEST_DEVICE, "com.example.FailedClassTest", "doesJobProperly", ResultStatus.FAIL, "a failure stacktrace", testMetricsForFailedTest),
            TestCaseRunResult.Companion.aTestResult(devicePool, Device.TEST_DEVICE, "com.example.IgnoredClassTest", "doesJobProperly", ResultStatus.IGNORED, "")
    );

    private final Map<Pool, Collection<TestCaseEvent>> testCaseEvents = ImmutableMap.<Pool, Collection<TestCaseEvent>>builder()
            .put(devicePool, newArrayList(
                newTestCase(new TestCase("doesJobProperly", "com.example.CompletedClassTest")),
                newTestCase(new TestCase("doesJobProperly", "com.example.CompletedClassTest2")),
                newTestCase("doesJobProperly", "com.example.FailedClassTest",
                        testMetricsForFailedTest, emptyList(), emptyList()),
                newTestCase(new TestCase("doesJobProperly", "com.example.IgnoredClassTest")),
                newTestCase(new TestCase("doesJobProperly", "com.example.SkippedClassTest"))
            )).build();

    @Before
    public void setUp() {
        summaryCompiler = new SummaryCompiler(mockConfiguration, fakeDeviceTestFilesRetriever);
        mockery.checking(new Expectations() {{
            allowing(mockConfiguration);
        }});
    }

    @Test
    public void compilesSummaryWithCompletedTests() {
        Summary summary = summaryCompiler.compileSummary(devicePools, testCaseEvents, testResults);

        assertThat(summary.getPoolSummaries().get(0).getTestResults(), hasItems(
                firstCompletedTest, secondCompletedTest));
    }

    @Test
    public void compilesSummaryWithIgnoredTests() {
        Summary summary = summaryCompiler.compileSummary(devicePools, testCaseEvents, testResults);

        assertThat(summary.getIgnoredTests(), hasSize(1));
        assertThat(mapToStringList(summary.getIgnoredTests()), contains("com.example.IgnoredClassTest#doesJobProperly"));
    }

    @Test
    public void compilesSummaryWithFailedTests() {
        Summary summary = summaryCompiler.compileSummary(devicePools, testCaseEvents, testResults);

        assertThat(summary.getFailedTests(), hasSize(1));
        assertThat(summary.getFailedTests().stream()
                        .map(r -> String.format("%d times %s", r.getTotalFailureCount(), r.getTestCase().toString()))
                        .collect(Collectors.toList()),
                contains("10 times com.example.FailedClassTest#doesJobProperly"));
    }

    @Test
    public void compilesSummaryWithFatalCrashedTestsIfTheyAreNotFoundInPassedOrFailedOrIgnored() {
        Summary summary = summaryCompiler.compileSummary(devicePools, testCaseEvents, testResults);

        assertThat(summary.getFatalCrashedTests(), hasSize(1));
        assertThat(mapToStringList(summary.getFatalCrashedTests()),
                contains("com.example.SkippedClassTest#doesJobProperly"));
    }

    @NotNull
    private static List<String> mapToStringList(List<TestCaseRunResult> resultList) {
        return resultList.stream()
                .map(testCaseRunResult -> testCaseRunResult.getTestCase().toString())
                .collect(Collectors.toList());
    }
}