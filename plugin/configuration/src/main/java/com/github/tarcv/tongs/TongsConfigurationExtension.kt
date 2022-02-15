/*
 * Copyright 2022 TarCV
 * Copyright 2016 Shazam Entertainment Limited
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License.
 *
 * You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */
package com.github.tarcv.tongs

import com.github.tarcv.tongs.api.TongsConfiguration.TongsIntegrationTestRunType

/**
 * Tongs extension.
 */
abstract class TongsConfigurationExtension {
    /**
     * Output directory for Tongs report files. If empty, the default dir will be used.
     */
    var baseOutputDir: String? = null

    /**
     * Ignore test failures flag.
     */
    var ignoreFailures = false

    /**
     * Enables code coverage.
     */
    var isCoverageEnabled = false

    /**
     * The title of the final report
     */
    var title: String = Defaults.TITLE

    /**
     * The subtitle of the final report
     */
    var subtitle: String = Defaults.SUBTITLE

    /**
     * The package to consider when scanning for instrumentation tests to run.
     */
    var testPackage: String? = null

    /**
     * Maximum time in milli-seconds between ADB output during a test. Prevents tests from getting stuck.
     */
    var testOutputTimeout: Int = Defaults.TEST_OUTPUT_TIMEOUT_MILLIS.toInt()

    /**
     * The collection of serials that should be excluded from this test run
     */
    var excludedSerials: Collection<String> = emptyList()

    /**
     * Indicate that screenshots are allowed when videos are not supported.
     */
    var fallbackToScreenshots = false

    /**
     * Amount of re-executions of failing tests allowed.
     */
    var totalAllowedRetryQuota = 0

    /**
     * Max number of time each testCase is attempted again before declaring it as a failure.
     */
    var retryPerTestCaseQuota = Defaults.RETRY_QUOTA_PER_TEST_CASE

    /**
     * Filter test run to tests without given annotation
     */
    var excludedAnnotation: String? = null

    /**
     * Plugins to load
     */
    var plugins: List<String> = ArrayList()

    /**
     * Misc. configuration options
     */
    var configuration: Map<String, Any> = HashMap()

    /**
     * Specifies that Tongs should run using one of "under integration test" modes
     */
    var tongsIntegrationTestRunType = TongsIntegrationTestRunType.NONE
}