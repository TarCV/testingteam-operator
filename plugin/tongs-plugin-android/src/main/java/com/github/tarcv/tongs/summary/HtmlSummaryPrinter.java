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

import com.android.ddmlib.logcat.LogCatMessage;
import com.google.common.io.Resources;
import com.github.tarcv.tongs.io.HtmlGenerator;
import com.github.tarcv.tongs.system.io.FileManager;

import org.lesscss.LessCompiler;

import java.io.File;
import java.util.List;

import static com.google.common.base.Charsets.UTF_8;
import static com.google.common.collect.Collections2.transform;
import static com.github.tarcv.tongs.io.Files.copyResource;
import static com.github.tarcv.tongs.summary.HtmlConverters.toHtmlLogCatMessages;
import static com.github.tarcv.tongs.summary.HtmlConverters.toHtmlSummary;
import static com.github.tarcv.tongs.system.io.FileType.HTML;
import static org.apache.commons.io.FileUtils.writeStringToFile;

// TODO: split and keep some part in tongs-runner
public class HtmlSummaryPrinter implements SummaryPrinter {
	private static final String HTML_OUTPUT = "html";
	private static final String STATIC = "static";
	private static final String INDEX_FILENAME = "index.html";
	private static final String[] STATIC_ASSETS = {
		"bootstrap-responsive.min.css",
		"bootstrap.min.css",
		"tongs.css",
		"bootstrap.min.js",
		"ceiling_android.png",
        "ceiling_android-green.png",
        "ceiling_android-red.png",
		"ceiling_android-yellow.png",
		"device.png",
		"icon-devices.png",
		"icon-log.png",
		"jquery.min.js",
		"log.png"
	};
	private final File htmlOutput;
	private final File staticOutput;
	private final LogCatRetriever retriever;
    private final HtmlGenerator htmlGenerator;
	private final FileManager fileManager;

	public HtmlSummaryPrinter(
    		File rootOutput,
			LogCatRetriever retriever,
			HtmlGenerator htmlGenerator,
			FileManager fileManager
	) {
		this.retriever = retriever;
        this.htmlGenerator = htmlGenerator;
        this.fileManager = fileManager;
        htmlOutput = new File(rootOutput, HTML_OUTPUT);
		staticOutput = new File(htmlOutput, STATIC);
	}

	@SuppressWarnings("ResultOfMethodCallIgnored")
    @Override
	public void print(Summary summary) {
        htmlOutput.mkdirs();
		copyAssets();
		generateCssFromLess();
		HtmlSummary htmlSummary = HtmlConverters.toHtmlSummary(fileManager, summary);
        htmlGenerator.generateHtml("tongspages/index.html", htmlOutput, INDEX_FILENAME, htmlSummary);
        generatePoolHtml(htmlSummary);
		generatePoolTestsHtml(htmlSummary);
	}

	private void copyAssets() {
		for (String asset : STATIC_ASSETS) {
            copyResource("/static/", asset, staticOutput);
        }
	}

    private void generateCssFromLess() {
		try {
			LessCompiler compiler = new LessCompiler();
			String less = Resources.toString(getClass().getResource("/spoon.less"), UTF_8);
			String css = compiler.compile(less);
			File cssFile = new File(staticOutput, "spoon.css");
			writeStringToFile(cssFile, css);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

    /**
     * Generates an HTML page for each pool, with multiple tests
     *
     * @param htmlSummary the summary of the pool
     */
	@SuppressWarnings("ResultOfMethodCallIgnored")
    private void generatePoolHtml(HtmlSummary htmlSummary) {
        File poolsDir = new File(htmlOutput, "pools");
        poolsDir.mkdirs();
        for (HtmlPoolSummary pool : htmlSummary.pools) {
            String name = pool.plainPoolName + ".html";
            htmlGenerator.generateHtml("tongspages/pool.html", poolsDir, name, pool);
        }
    }

	/**
     * Genarates an HTML page for each test of each pool.
     *
	 * @param htmlSummary the summary containing the results
	 */
	@SuppressWarnings("ResultOfMethodCallIgnored")
    private void generatePoolTestsHtml(HtmlSummary htmlSummary) {
        for (HtmlPoolSummary pool : htmlSummary.pools) {
            File poolTestsDir = new File(htmlOutput, "pools/" + pool.plainPoolName);
            poolTestsDir.mkdirs();
            for (HtmlTestResult testResult : pool.testResults) {
				addLogcats(testResult, pool);
                htmlGenerator.generateHtml("tongspages/pooltest.html", poolTestsDir, testResult.fileNameForTest + HTML.getSuffix(), testResult, pool);
            }
        }
	}

    private void addLogcats(HtmlTestResult testResult, HtmlPoolSummary pool) {
		List<LogCatMessage> logCatMessages = retriever.retrieveLogCat(pool.plainPoolName, testResult.deviceSafeSerial, testResult.testIdentifier);
        testResult.logcatMessages = transform(logCatMessages, HtmlConverters.toHtmlLogCatMessages());
    }
}
