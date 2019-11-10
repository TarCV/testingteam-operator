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
package com.github.tarcv.tongs.summary;

import com.github.tarcv.tongs.TongsConfiguration;
import com.github.tarcv.tongs.model.TestCase;
import com.github.tarcv.tongs.model.*;
import com.github.tarcv.tongs.runner.TestCaseRunResult;
import com.google.common.collect.Sets;

import javax.annotation.Nullable;
import java.util.*;
import java.util.stream.Collectors;

import static com.github.tarcv.tongs.runner.PoolTestRunner.DROPPED_BY;
import static com.github.tarcv.tongs.summary.PoolSummary.Builder.aPoolSummary;
import static com.github.tarcv.tongs.summary.ResultStatus.ERROR;
import static com.github.tarcv.tongs.summary.ResultStatus.FAIL;
import static com.github.tarcv.tongs.summary.Summary.Builder.aSummary;
import static java.lang.String.format;
import static java.util.Locale.ENGLISH;

public class SummaryCompiler {
    private final TongsConfiguration configuration;
    private final DeviceTestFilesRetriever deviceTestFilesRetriever;

    public SummaryCompiler(TongsConfiguration configuration, DeviceTestFilesRetriever deviceTestFilesRetriever) {
        this.configuration = configuration;
        this.deviceTestFilesRetriever = deviceTestFilesRetriever;
    }

    Summary compileSummary(Collection<Pool> pools, Map<Pool, Collection<TestCaseEvent>> testCasesPerPool, List<TestCaseRunResult> results) {
        Summary.Builder summaryBuilder = aSummary();
        summaryBuilder.addResults(results);

        Map<Pool, List<TestCaseRunResult>> resultsByPool = results.stream()
                .collect(Collectors.groupingBy(TestCaseRunResult::getPool));

        Set<TestCaseRunResult> testResults = Sets.newHashSet();
        for (Pool pool : pools) {
            Collection<TestCaseRunResult> testResultsForPool = resultsByPool.get(pool);
            testResults.addAll(testResultsForPool);

            PoolSummary poolSummary = aPoolSummary()
                    .withPoolName(pool.getName())
                    .addTestResults(testResultsForPool)
                    .build();

            summaryBuilder.addPoolSummary(poolSummary);

            Collection<TestCaseEvent> testCasesForPool = testCasesPerPool.get(pool);
            addFatalCrashedTests(pool, testCasesForPool, testResultsForPool, summaryBuilder);
            addFailedOrCrashedTests(testResultsForPool, summaryBuilder);

            addIgnoredTests(testResultsForPool, summaryBuilder);
        }

        addFatalCrashedPools(pools, testCasesPerPool, summaryBuilder);

        // TODO: Use TestCaseRunResult datas instead of reading videos, logcat, etc

        summaryBuilder.withTitle(configuration.getTitle());
        summaryBuilder.withSubtitle(configuration.getSubtitle());

        return summaryBuilder.build();
    }

    private static void addFatalCrashedPools(Collection<Pool> pools, Map<Pool, Collection<TestCaseEvent>> testCases, Summary.Builder summaryBuilder) {
        Sets.difference(new HashSet<>(pools), testCases.keySet())
                .forEach(pool -> {
                    summaryBuilder.addFatalError("Pool " + pool.getName() + " not executed");
                });
    }

    private Collection<TestResult> getTestResultsForPool(Pool pool, Map<com.github.tarcv.tongs.model.TestCase , TestCaseEvent> eventMap) {
        Set<TestResult> testResults = Sets.newHashSet();

        Collection<TestResult> testResultsForPoolDevices = pool.getDevices()
                .stream()
                .map(device -> {
                    return deviceTestFilesRetriever
                            .getTestResultsForDevice(pool, device).stream()
                            .map(testResult -> {
                                // TODO: In current Tongs implementation testMetrics and properties is the same, probably split them later
                                TestCaseEvent testCaseEvent = eventMap.get(new TestCase(testResult.getTestMethod(), testResult.getTestClass()));
                                if (testCaseEvent != null) {
                                    Map<String, String> xmlMetrics = testResult.getMetrics();
                                    Map<String, String> outMetrics = new HashMap<>();

                                    // Metrics from XML Output take priority
                                    outMetrics.putAll(testCaseEvent.getProperties());
                                    outMetrics.putAll(xmlMetrics);

                                    return new TestResult.Builder(testResult)
                                            .withTestMetrics(outMetrics)
                                            .build();
                                } else {
                                    return testResult;
                                }
                            })
                            .collect(Collectors.toSet());
                })
                .reduce(Sets.newHashSet(), (accum, set) -> {
                    accum.addAll(set);
                    return accum;
                });
        testResults.addAll(testResultsForPoolDevices);

        Device watchdog = getPoolWatchdog(pool.getName());
        Collection<TestResult> testResultsForWatchdog =
                deviceTestFilesRetriever.getTestResultsForDevice(pool, watchdog);
        testResults.addAll(testResultsForWatchdog);

        return testResults;
    }

    private static Device getPoolWatchdog(String poolName) {
        return AndroidDevice.Builder.aDevice()
                .withSerial(DROPPED_BY + poolName)
                .withManufacturer("Clumsy-" + poolName)
                .withModel("Clumsy=" + poolName)
                .build();
    }

    private static void addFailedOrCrashedTests(Collection<TestCaseRunResult> testResultsForPool, Summary.Builder summaryBuilder) {
        for (TestCaseRunResult testResult : testResultsForPool) {
            int totalFailureCount = testResult.getTotalFailureCount();
            if (totalFailureCount > 0) {
                String failedTest = format(ENGLISH, "%d times %s", totalFailureCount, getTestResultData(testResult));
                summaryBuilder.addFailedTests(testResult);
            } else if (testResult.getStatus() == ERROR || testResult.getStatus() == FAIL) {
                summaryBuilder.addFatalCrashedTest(testResult);
            }
        }
    }

    private static void addFatalCrashedTests(Pool pool, Collection<TestCaseEvent> testCasesForPool, Collection<TestCaseRunResult> testResultsForPool, Summary.Builder summaryBuilder) {
        Set<TestResultItem> processedTests = testResultsForPool.stream()
                .map(testResult -> new TestResultItem(testResult.getTestCase().getTestClass(), testResult.getTestCase().getTestMethod()))
                .collect(Collectors.toSet());
        Set<TestResultItem> allTests = testCasesForPool.stream()
                .map(testCaseEvent -> new TestResultItem(testCaseEvent.getTestClass(), testCaseEvent.getTestMethod()))
                .collect(Collectors.toSet());

        Sets.difference(allTests, processedTests)
                .stream()
                .map(testResultItem -> {
                    return new TestCaseRunResult(pool, NO_DEVICE,
                            new TestCase(testResultItem.testMethod, testResultItem.testClass),
                            ERROR, "Fatally crashed",
                            0, 0,
                            Collections.emptyMap(), null, Collections.emptyList());
                })
                .forEach(testCaseRunResult -> {
                    summaryBuilder.addFatalCrashedTest(testCaseRunResult);
                });
    }

    private static void addIgnoredTests(Collection<TestCaseRunResult> ignoredTestResults, Summary.Builder summaryBuilder) {
        ignoredTestResults.stream()
                .filter(r ->
                        // TODO: check ASSUMPTION_FAILED eventually executed on some device are not considered skipped
                        r.getStatus() == ResultStatus.IGNORED || r.getStatus() == ResultStatus.ASSUMPTION_FAILED)
                .forEach(testCaseRunResult -> {
                    summaryBuilder.addIgnoredTest(testCaseRunResult);
                });
    }

    private static String getTestResultData(TestCaseRunResult testResult) {
        return format(ENGLISH, "%s#%s on %s", testResult.getTestCase().getTestClass(), testResult.getTestCase().getTestMethod(),
                testResult.getDevice().getSerial());
    }

    private static class TestResultItem {
        private final String testClass;
        private final String testMethod;

        TestResultItem(String testClass, String testMethod) {
            this.testClass = testClass;
            this.testMethod = testMethod;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            TestResultItem that = (TestResultItem) o;
            return Objects.equals(testClass, that.testClass) &&
                    Objects.equals(testMethod, that.testMethod);
        }

        @Override
        public int hashCode() {
            return Objects.hash(testClass, testMethod);
        }
    }

    private static final Device NO_DEVICE = new Device() {
        @Override
        public String getSerial() {
            return "N/A";
        }

        @Override
        public String getManufacturer() {
            return "-";
        }

        @Override
        public String getModelName() {
            return "No Device";
        }

        @Override
        public int getOsApiLevel() {
            return 0;
        }

        @Override
        public String getLongName() {
            return "No Device";
        }

        @Override
        public Object getDeviceInterface() {
            return new Object();
        }

        @Override
        public boolean isTablet() {
            return false;
        }

        @Override
        @Nullable
        public DisplayGeometry getGeometry() {
            return new DisplayGeometry(300);
        }

        @Override
        public Diagnostics getSupportedVisualDiagnostics() {
            return Diagnostics.NONE;
        }

        @Override
        protected Object getUniqueIdentifier() {
            return new Object();
        }
    };
}
