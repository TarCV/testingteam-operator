/*
 * Copyright 2020 TarCV
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

import com.github.tarcv.tongs.api.TongsConfiguration;
import com.github.tarcv.tongs.api.devices.Device;
import com.github.tarcv.tongs.api.devices.Diagnostics;
import com.github.tarcv.tongs.api.devices.DisplayGeometry;
import com.github.tarcv.tongs.api.devices.Pool;
import com.github.tarcv.tongs.api.result.StackTrace;
import com.github.tarcv.tongs.api.result.TestCaseRunResult;
import com.github.tarcv.tongs.api.run.ResultStatus;
import com.github.tarcv.tongs.api.run.TestCaseEvent;
import com.github.tarcv.tongs.api.testcases.TestCase;
import com.google.common.collect.Sets;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

import static com.github.tarcv.tongs.api.run.ResultStatus.ERROR;
import static com.github.tarcv.tongs.summary.PoolSummary.Builder.aPoolSummary;
import static com.github.tarcv.tongs.summary.Summary.Builder.aSummary;

public class SummaryCompiler {
    private final TongsConfiguration configuration;

    public SummaryCompiler(TongsConfiguration configuration) {
        this.configuration = configuration;
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
                .forEach(pool -> summaryBuilder.addFatalError("Pool " + pool.getName() + " not executed"));
    }

    private static void addFailedOrCrashedTests(Collection<TestCaseRunResult> testResultsForPool, Summary.Builder summaryBuilder) {
        for (TestCaseRunResult testResult : testResultsForPool) {
            int totalFailureCount = testResult.getTotalFailureCount();
            if (totalFailureCount > 0) {
                summaryBuilder.addFailedTests(testResult);
            } else if (ResultStatus.isFailure(testResult.getStatus())) {
                // totalFailureCount of 0 here means something went wrong and this is actually a fatal crash
                // TODO: handle this in a way that makes sure testResult.status == ERROR from plugins POV
                summaryBuilder.addFatalCrashedTest(testResult);
            }
        }
    }

    private static void addFatalCrashedTests(Pool pool, Collection<TestCaseEvent> testCasesForPool, Collection<TestCaseRunResult> testResultsForPool, Summary.Builder summaryBuilder) {
        Set<TestCase> processedTests = testResultsForPool.stream()
                .map(TestCaseRunResult::getTestCase)
                .collect(Collectors.toSet());
        Set<TestCase> allTests = testCasesForPool.stream()
                .map(TestCaseEvent::getTestCase)
                .collect(Collectors.toSet());

        Sets.difference(allTests, processedTests)
                .stream()
                .map(testResultItem -> new TestCaseRunResult(pool, NO_DEVICE,
                            testResultItem,
                            ERROR, Collections.singletonList(new StackTrace("FatalError", "Fatally crashed", "Fatally crashed")),
                            Instant.now(), Instant.EPOCH, Instant.now(), Instant.EPOCH,
                            0, Collections.emptyMap(), null, Collections.emptyList())
                )
                .forEach(summaryBuilder::addFatalCrashedTest);
    }

    private static void addIgnoredTests(Collection<TestCaseRunResult> ignoredTestResults, Summary.Builder summaryBuilder) {
        ignoredTestResults.stream()
                .filter(r ->
                        // TODO: check ASSUMPTION_FAILED eventually executed on some device are not considered skipped
                        ResultStatus.isIgnored(r.getStatus()))
                .forEach(summaryBuilder::addIgnoredTest);
    }

    private static final Device NO_DEVICE = new Device() {
        private final Object uniqueIdentifier = new Object();

        @NotNull
        @Override
        public String getHost() {
            return "N/A";
        }

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
            return uniqueIdentifier;
        }
    };
}
