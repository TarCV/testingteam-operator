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

import com.github.tarcv.tongs.runner.TestCaseRunResult;
import com.github.tarcv.tongs.system.io.FileManager;
import com.google.common.base.Function;

import javax.annotation.Nullable;

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
        htmlSummary.fatalErrors = summary.getFatalErrors();
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
                htmlPoolSummary.poolName = poolName;
                htmlPoolSummary.testResults = poolSummary.getTestResults();
				return htmlPoolSummary;
			}

            private String getPoolStatus(PoolSummary poolSummary) {
                Boolean success = OutcomeAggregator.toPoolOutcome().apply(poolSummary);
                return (success != null && success? "pass" : "fail");
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
