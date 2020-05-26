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

import com.github.tarcv.tongs.api.run.ResultStatus;
import com.github.tarcv.tongs.api.result.TestCaseRunResult;
import com.github.tarcv.tongs.system.io.FileManager;
import com.google.common.base.Function;

import javax.annotation.Nullable;

import static com.google.common.collect.Collections2.transform;

class HtmlConverters {

	public static HtmlSummary toHtmlSummary(FileManager fileManager, Summary summary) {
		HtmlSummary htmlSummary = new HtmlSummary(
				transform(summary.getPoolSummaries(), toHtmlPoolSummary(fileManager)),
				summary.getTitle(),
				summary.getSubtitle(),
				summary.getIgnoredTests(),
				new OutcomeAggregator().aggregate(summary) ? "pass" : "fail",
				summary.getFailedTests(),
        		summary.getFatalCrashedTests(),
        		summary.getFatalErrors() // TODO: Add to template
		);
		return htmlSummary;
	}

	private static Function<PoolSummary, HtmlPoolSummary> toHtmlPoolSummary(
			final FileManager fileManager
	) {
		return new Function<PoolSummary, HtmlPoolSummary> () {
			@Override
			@Nullable
			public HtmlPoolSummary apply(@Nullable PoolSummary poolSummary) {
				return new HtmlPoolSummary(
						getPoolStatus(poolSummary),
						poolSummary.getTestResults(),
						poolSummary.getPoolName()
				);
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
