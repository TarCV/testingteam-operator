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
import com.github.tarcv.tongs.model.TestCase;
import com.github.tarcv.tongs.model.TestCaseEvent;
import com.google.common.base.Function;
import com.github.tarcv.tongs.model.Device;
import com.github.tarcv.tongs.model.Diagnostics;
import com.github.tarcv.tongs.system.io.FileManager;

import javax.annotation.Nullable;

import static com.google.common.collect.Collections2.transform;
import static com.github.tarcv.tongs.model.Diagnostics.SCREENSHOTS;
import static com.github.tarcv.tongs.model.Diagnostics.VIDEO;
import static com.github.tarcv.tongs.summary.OutcomeAggregator.toPoolOutcome;
import static com.github.tarcv.tongs.system.io.FileType.DOT_WITHOUT_EXTENSION;
import static com.github.tarcv.tongs.utils.ReadableNames.readableClassName;
import static com.github.tarcv.tongs.utils.ReadableNames.readablePoolName;
import static com.github.tarcv.tongs.utils.ReadableNames.readableTestMethodName;

class HtmlConverters {

	public static HtmlSummary toHtmlSummary(FileManager fileManager, Summary summary) {
		HtmlSummary htmlSummary = new HtmlSummary();
		htmlSummary.title = summary.getTitle();
		htmlSummary.subtitle = summary.getSubtitle();
		htmlSummary.pools = transform(summary.getPoolSummaries(), toHtmlPoolSummary(fileManager));
		htmlSummary.ignoredTests = summary.getIgnoredTests();
		htmlSummary.failedTests = summary.getFailedTests();
        htmlSummary.fatalCrashedTests = summary.getFatalCrashedTests();
        htmlSummary.overallStatus = new OutcomeAggregator().aggregate(summary) ? "pass" : "fail";
		return htmlSummary;
	}

	private static Function<PoolSummary, HtmlPoolSummary> toHtmlPoolSummary(
			final FileManager fileManager
	) {
		return new Function<PoolSummary, HtmlPoolSummary> () {
			@Override
			@Nullable
			public HtmlPoolSummary apply(@Nullable PoolSummary poolSummary) {
				HtmlPoolSummary htmlPoolSummary = new HtmlPoolSummary();
                htmlPoolSummary.poolStatus = getPoolStatus(poolSummary);
				String poolName = poolSummary.getPoolName();
				htmlPoolSummary.prettyPoolName = readablePoolName(poolName);
                htmlPoolSummary.plainPoolName = poolName;
                htmlPoolSummary.testCount = poolSummary.getTestResults().size();
                htmlPoolSummary.testResults = transform(poolSummary.getTestResults(), toHtmlTestResult(fileManager, poolName));
				return htmlPoolSummary;
			}

            private String getPoolStatus(PoolSummary poolSummary) {
                Boolean success = OutcomeAggregator.toPoolOutcome().apply(poolSummary);
                return (success != null && success? "pass" : "fail");
            }
        };
	}

	private static Function<TestResult, HtmlTestResult> toHtmlTestResult(
			final FileManager fileManager,
			final String poolName
	) {
		return new Function<TestResult, HtmlTestResult>(){
			@Override
			@Nullable
			public HtmlTestResult apply(@Nullable TestResult input) {
				HtmlTestResult htmlTestResult = new HtmlTestResult();
				htmlTestResult.status = computeStatus(input);
				htmlTestResult.prettyClassName = readableClassName(input.getTestClass());
				htmlTestResult.prettyMethodName = readableTestMethodName(input.getTestMethod());
				htmlTestResult.timeTaken = String.format("%.2f", input.getTimeTaken());
				TestCase testIdentifier = new TestCase(input.getTestClass(), input.getTestMethod());
				htmlTestResult.testIdentifier = TestCaseEvent.newTestCase(testIdentifier);
				htmlTestResult.fileNameForTest = fileManager.createFilenameForTest(testIdentifier , DOT_WITHOUT_EXTENSION);
				htmlTestResult.poolName = poolName;
				htmlTestResult.trace = input.getTrace().split("\n");
				// Keeping logcats in memory is hugely wasteful. Now they're read at page-creation.
				// htmlTestResult.logcatMessages = transform(input.getLogCatMessages(), toHtmlLogCatMessages());
				Device device = input.getDevice();
				htmlTestResult.deviceSerial = device.getSerial();
				htmlTestResult.deviceSafeSerial = device.getSafeSerial();
				htmlTestResult.deviceModelDespaced = device.getModelName().replace(" ", "_");
                Diagnostics supportedDiagnostics = device.getSupportedVisualDiagnostics();
                htmlTestResult.diagnosticVideo = VIDEO.equals(supportedDiagnostics);
                htmlTestResult.diagnosticScreenshots = SCREENSHOTS.equals(supportedDiagnostics);
				return htmlTestResult;
			}
		};
	}

	private static String computeStatus(@Nullable TestResult input) {
		String result  = input.getResultStatus().name().toLowerCase();
		if(input.getResultStatus() == ResultStatus.PASS
				&& input.getTotalFailureCount() > 0){
			result = "warn";
		}
		return result;
	}

	public static Function<LogCatMessage, HtmlLogCatMessage> toHtmlLogCatMessages() {
		return new Function<LogCatMessage, HtmlLogCatMessage>() {
			@Nullable
			@Override
			public HtmlLogCatMessage apply(@Nullable LogCatMessage logCatMessage) {
				HtmlLogCatMessage htmlLogCatMessage = new HtmlLogCatMessage();
				htmlLogCatMessage.appName = logCatMessage.getAppName();
				htmlLogCatMessage.logLevel = logCatMessage.getLogLevel().getStringValue();
				htmlLogCatMessage.message = logCatMessage.getMessage();
				htmlLogCatMessage.pid = logCatMessage.getPid();
				htmlLogCatMessage.tag = logCatMessage.getTag();
				htmlLogCatMessage.tid = logCatMessage.getTid();
				htmlLogCatMessage.time = logCatMessage.getTimestamp().toString();
				return htmlLogCatMessage;
			}
		};
	}
}
