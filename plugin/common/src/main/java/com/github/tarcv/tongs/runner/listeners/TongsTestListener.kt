/*
 * Copyright 2020 TarCV
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License.
 *
 * You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */

package com.github.tarcv.tongs.runner.listeners

import com.github.tarcv.tongs.api.result.TestCaseRunResult
import com.github.tarcv.tongs.api.run.ResultStatus
import com.github.tarcv.tongs.api.run.TestCaseRunRule
import com.github.tarcv.tongs.api.run.TestCaseRunRuleAfterArguments

abstract class TongsTestListener: TestCaseRunRule {
    abstract fun onTestStarted()
    abstract fun onTestSuccessful()
    abstract fun onTestSkipped(skipResult: TestCaseRunResult)
    abstract fun onTestFailed(failureResult: TestCaseRunResult)
    abstract fun onTestAssumptionFailure(skipResult: TestCaseRunResult)

    override fun before() {
        onTestStarted()
    }

    override fun after(arguments: TestCaseRunRuleAfterArguments) {
        val result = arguments.result
        val status = result.status
        val forceExhaustive = when (status) {
            ResultStatus.PASS -> onTestSuccessful()
            ResultStatus.IGNORED -> onTestSkipped(result)
            ResultStatus.ASSUMPTION_FAILED -> onTestAssumptionFailure(result)
            ResultStatus.FAIL, ResultStatus.ERROR -> onTestFailed(result)
        }
    }
}