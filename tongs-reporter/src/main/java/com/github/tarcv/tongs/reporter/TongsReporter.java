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

package com.github.tarcv.tongs.reporter;

import com.github.tarcv.tongs.reporter.model.Executions;
import com.github.tarcv.tongs.reporter.model.FlakinessReport;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;

import javax.annotation.Nullable;

import static com.google.common.base.Strings.isNullOrEmpty;
import static com.github.tarcv.tongs.reporter.injector.ConfigurationInjector.setConfiguration;
import static com.github.tarcv.tongs.reporter.injector.ExecutionReaderInjector.executionReader;
import static com.github.tarcv.tongs.reporter.injector.FlakinessReportPrinterInjector.flakinessReportPrinter;
import static com.github.tarcv.tongs.reporter.injector.FlakinessSorterInjector.flakinessSorter;
import static com.github.tarcv.tongs.utils.Utils.millisSinceNanoTime;
import static java.lang.System.nanoTime;

public class TongsReporter {
    private static final Logger logger = LoggerFactory.getLogger(TongsReporter.class);

    private final ExecutionReader reader;
    private final FlakinessSorter flakinessSorter;
    private final FlakinessReportPrinter flakinessReportPrinter;

    private TongsReporter(Configuration configuration) {
        setConfiguration(configuration);
        reader = executionReader();
        flakinessSorter = flakinessSorter();
        flakinessReportPrinter = flakinessReportPrinter();
    }

    public void createReport() {
        long startOfTestsMs = nanoTime();

        try {
            Executions executions = reader.readExecutions();
            FlakinessReport flakinessReport = flakinessSorter.sort(executions);
            flakinessReportPrinter.printReport(flakinessReport);
        } catch (FlakinessCalculationException e) {
            logger.error("Error while calculating flakiness", e);
        } finally {
            long duration = millisSinceNanoTime(startOfTestsMs);
            logger.info("Total time taken: {} milliseconds", duration);
        }
    }

    public static class Builder {
        private File input;
        private File output;
        private String title = "";
        private String baseUrl = "";

        public static Builder tongsReporter() {
            return new Builder();
        }

        public Builder withInput(File input) {
            this.input = input;
            return this;
        }

        public Builder withOutput(File output) {
            this.output = output;
            return this;
        }

        public Builder withTitle(String title) {
            this.title = title;
            return this;
        }

        public Builder withBaseUrl(@Nullable String baseUrl) {
            if (!isNullOrEmpty(baseUrl)) {
                this.baseUrl = baseUrl;
            }
            return this;
        }

        public TongsReporter build() {
            Configuration configuration = new Configuration(input, output, title, baseUrl);
            return new TongsReporter(configuration);
        }
    }
}
