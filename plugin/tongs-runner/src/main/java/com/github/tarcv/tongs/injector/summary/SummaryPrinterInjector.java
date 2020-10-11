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
package com.github.tarcv.tongs.injector.summary;

import com.github.tarcv.tongs.summary.*;

import static com.github.tarcv.tongs.injector.ConfigurationInjector.configuredOutput;
import static com.github.tarcv.tongs.injector.GsonInjector.gson;
import static com.github.tarcv.tongs.injector.summary.HtmlGeneratorInjector.htmlGenerator;
import static com.github.tarcv.tongs.injector.system.FileManagerInjector.fileManager;

public class SummaryPrinterInjector {

    private SummaryPrinterInjector() {}

    public static SummaryPrinter summaryPrinter() {
        return new CompositeSummaryPrinter(consoleSummaryPrinter(),
                htmlSummaryPrinter(),
                xmlSummaryPrinter(),
                jsonSummarySerializer()
        );
    }

    private static SummaryPrinter consoleSummaryPrinter() {
        return new LogSummaryPrinter();
    }

    private static SummaryPrinter htmlSummaryPrinter() {
        return new HtmlSummaryPrinter(configuredOutput(), htmlGenerator());
    }

    private static SummaryPrinter xmlSummaryPrinter() {
        return new XmlSummaryPrinter(configuredOutput(), fileManager(), new XmlResultWriter());
    }

    private static SummaryPrinter jsonSummarySerializer() {
        return new JsonSummarySerializer(fileManager(), gson());
    }
}
