/*
 * Copyright 2020 TarCV
 * Copyright 2015 Shazam Entertainment Limited
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License.
 *
 * You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */

package com.github.tarcv.tongs.summary;

import com.github.tarcv.tongs.Configuration;
import com.github.tarcv.tongs.model.Pool;
import com.github.tarcv.tongs.model.TestCaseEvent;
import com.github.tarcv.tongs.runner.TestCaseRunResult;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import static com.github.tarcv.tongs.TongsConfiguration.TongsIntegrationTestRunType.RECORD_LISTENER_EVENTS;
import static java.nio.file.StandardOpenOption.*;

public class Summarizer {

    private final Configuration configuration;
    private final SummaryCompiler summaryCompiler;
    private final SummaryPrinter summaryPrinter;
    private final OutcomeAggregator outcomeAggregator;

    public Summarizer(Configuration configuration, SummaryCompiler summaryCompiler, SummaryPrinter summaryPrinter, OutcomeAggregator outcomeAggregator) {
        this.configuration = configuration;
        this.summaryCompiler = summaryCompiler;
        this.summaryPrinter = summaryPrinter;
        this.outcomeAggregator = outcomeAggregator;
    }

    boolean summarize(Collection<Pool> pools, Map<Pool, Collection<TestCaseEvent>> testCases, List<TestCaseRunResult> results) {
        if (configuration.getTongsIntegrationTestRunType() == RECORD_LISTENER_EVENTS) {
            try (BufferedWriter outputWriter = Files.newBufferedWriter(
                    new File("summarizeInputs.json").toPath(),
                    CREATE, WRITE, TRUNCATE_EXISTING)) {
                SummarizerInputs inputs = new SummarizerInputs(pools, testCases, results);
                testRecorderGsonBuilder().create().toJson(inputs, outputWriter);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        Summary summary = summaryCompiler.compileSummary(pools, testCases, results);
        summaryPrinter.print(summary);
        return outcomeAggregator.aggregate(summary);
    }

    boolean summarizeFromRecordedJson(Reader reader, Gson gson) {
        SummarizerInputs inputs = gson.fromJson(reader, SummarizerInputs.class);
        return summarize(inputs.pools, inputs.testCases, inputs.results);
    }

    static GsonBuilder testRecorderGsonBuilder() {
        return new GsonBuilder()
                .enableComplexMapKeySerialization();
    }



    private final static class SummarizerInputs {
        private final Collection<Pool> pools;
        private final Map<Pool, Collection<TestCaseEvent>> testCases;
        private final List<TestCaseRunResult> results;

        private SummarizerInputs(Collection<Pool> pools, Map<Pool, Collection<TestCaseEvent>> testCases, List<TestCaseRunResult> results) {
            this.pools = pools;
            this.testCases = testCases;
            this.results = results;
        }
    }
}
