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
package com.github.tarcv.tongs.summary

import com.github.tarcv.tongs.runner.TestCaseRunResult
import java.util.Collections.unmodifiableList


class Summary private constructor(builder: Builder) {
    val title: String
    val subtitle: String

    val poolSummaries: List<PoolSummary>
        get() = unmodifiableList(field)
    val ignoredTests: List<String>
        get() = unmodifiableList(field)
    val failedTests: List<String>
        get() = unmodifiableList(field)
    val fatalCrashedTests: List<String>
        get() = unmodifiableList(field)
    val allTests: List<TestCaseRunResult>
        get() = unmodifiableList(field)

    init {
        poolSummaries = builder.poolSummaries
        title = builder.title
        subtitle = builder.subtitle
        ignoredTests = builder.ignoredTests
        failedTests = builder.failedTests
        fatalCrashedTests = builder.fatalCrashedTests
        allTests = builder.allTests
    }

    class Builder {
        internal val poolSummaries = ArrayList<PoolSummary>()
        internal val ignoredTests = ArrayList<String>()
        internal var title = "Report Title"
        internal var subtitle = "Report Subtitle"
        internal val failedTests = ArrayList<String>()
        internal val fatalCrashedTests = ArrayList<String>()
        internal val allTests = ArrayList<TestCaseRunResult>()

        fun addPoolSummary(poolSummary: PoolSummary): Builder {
            poolSummaries.add(poolSummary)
            return this
        }

        fun withTitle(title: String): Builder {
            this.title = title
            return this
        }

        fun withSubtitle(subtitle: String): Builder {
            this.subtitle = subtitle
            return this
        }

        fun addIgnoredTest(s: String): Builder {
            this.ignoredTests.add(s)
            return this
        }

        fun addFailedTests(failedTests: String): Builder {
            this.failedTests.add(failedTests)
            return this
        }

        fun addFatalCrashedTest(fatalCrashedTest: String): Builder {
            fatalCrashedTests.add(fatalCrashedTest)
            return this
        }

        fun addResults(results: List<TestCaseRunResult>): Builder {
            allTests.addAll(results)
            return this
        }

        fun build(): Summary {
            return Summary(this)
        }

        companion object {
            @JvmStatic
            fun aSummary(): Builder {
                return Builder()
            }
        }
    }
}
