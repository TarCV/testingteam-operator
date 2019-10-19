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

import com.github.tarcv.tongs.model.Device;
import com.github.tarcv.tongs.model.Diagnostics;
import com.github.tarcv.tongs.model.TestCase;
import com.github.tarcv.tongs.model.TestCaseEvent;
import com.github.tarcv.tongs.runner.TestCaseRunResult;
import com.github.tarcv.tongs.system.io.FileManager;
import com.github.tarcv.tongs.system.io.FileUtils;
import com.google.common.base.Function;

import javax.annotation.Nullable;

import static com.github.tarcv.tongs.model.Diagnostics.SCREENSHOTS;
import static com.github.tarcv.tongs.model.Diagnostics.VIDEO;
import static com.github.tarcv.tongs.system.io.StandardFileTypes.DOT_WITHOUT_EXTENSION;
import static com.github.tarcv.tongs.utils.ReadableNames.*;
import static com.google.common.collect.Collections2.transform;

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

	private static Function<TestCaseRunResult, HtmlTestResult> toHtmlTestResult(
			final FileManager fileManager,
			final String poolName
	) {
		return new Function<TestCaseRunResult, HtmlTestResult>(){
			@Override
			@Nullable
			public HtmlTestResult apply(@Nullable TestCaseRunResult input) {
				HtmlTestResult htmlTestResult = new HtmlTestResult();
				htmlTestResult.status = computeStatus(input);
				htmlTestResult.prettyClassName = readableClassName(input.getTestCase().getTestClass());
				htmlTestResult.prettyMethodName = readableTestMethodName(input.getTestCase().getTestMethod());
				htmlTestResult.timeTaken = String.format("%.2f", input.getTimeTaken());
				TestCase testIdentifier = new TestCase(input.getTestCase().getTestMethod(), input.getTestCase().getTestClass());
				htmlTestResult.testIdentifier = TestCaseEvent.newTestCase(testIdentifier);
				htmlTestResult.fileNameForTest = FileUtils.createFilenameForTest(testIdentifier , DOT_WITHOUT_EXTENSION);
				htmlTestResult.poolName = poolName;
				htmlTestResult.trace = input.getStackTrace().split("\n");
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

	private static String computeStatus(@Nullable TestCaseRunResult input) {
		String result  = input.getStatus().name().toLowerCase();
		if(input.getStatus() == ResultStatus.PASS
				&& input.getTotalFailureCount() > 0){
			result = "warn";
		}
		return result;
	}
}
