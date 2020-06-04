/*
 * Copyright 2020 TarCV
 * Copyright 2016 Shazam Entertainment Limited
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License.
 *
 * You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */
package com.github.tarcv.tongs.runner.listeners

import com.android.ddmlib.testrunner.TestIdentifier
import com.github.tarcv.tongs.api.run.TestCaseEvent
import com.github.tarcv.tongs.runner.PreregisteringLatch
import com.github.tarcv.tongs.api.run.ResultStatus
import org.slf4j.LoggerFactory
import javax.annotation.concurrent.GuardedBy
import javax.annotation.concurrent.ThreadSafe

// TODO: Return ResultStatus.ERROR when necessary
@ThreadSafe
class ResultListener(private val currentTestCaseEvent: TestCaseEvent,
                     latch: PreregisteringLatch) : BaseListener(latch), FullTestRunListener {

    private val lock = Any()

    @field:GuardedBy("lock") var result: ShellResult = ShellResult()
        @GuardedBy("lock") private set
        get() = synchronized(lock) {
            field
        }

    data class ShellResult(
            val status: ResultStatus? = null,
            val output: String = "",
            val metrics: Map<String, String> = emptyMap(),
            val trace: String = "",
            val startTime: Long? = null,
            val endTime: Long? = null
    )

    @GuardedBy("lock")
    private fun setStatus(newStatus: ResultStatus) {
        val actualNewStatus = if (result.status == null) {
            newStatus
        } else {
            logger.warn("Tried to set run status for {}#{} twice. Falling back to FAILED",
                    currentTestCaseEvent.testClass, currentTestCaseEvent.testMethod)
            ResultStatus.FAIL // TODO: consider replacing with ERROR
        }
        result = result.copy(status = actualNewStatus)
    }

    override fun testRunStarted(runName: String, testCount: Int) {}
    override fun testStarted(test: TestIdentifier) {
        synchronized(lock) {
            result = result.copy(startTime = System.currentTimeMillis())
        }
    }
    override fun testFailed(test: TestIdentifier, trace: String) {
        synchronized(lock) {
            setStatus(ResultStatus.FAIL)
            result = result.copy(trace = trace)
        }
    }

    override fun testAssumptionFailure(test: TestIdentifier, trace: String) {
        synchronized(lock) {
            setStatus(ResultStatus.ASSUMPTION_FAILED)
            result = result.copy(trace = trace)
        }
    }

    override fun testIgnored(test: TestIdentifier) {
        synchronized(lock) { setStatus(ResultStatus.IGNORED) }
    }

    override fun testEnded(test: TestIdentifier, testMetrics: Map<String, String>) {
        synchronized(lock) {
            val actualNewStatus = if (result.status == null) {
                ResultStatus.PASS
            } else {
                result.status
            }

            val endTime = if (result.startTime != null) {
                System.currentTimeMillis()
            } else {
                null
            }

            result = result.copy(status = actualNewStatus, metrics = testMetrics, endTime = endTime)
        }
    }

    override fun testRunFailed(errorMessage: String) {
        synchronized(lock) {
            try {
                setStatus(ResultStatus.IGNORED)
            } finally {
                onWorkFinished()
            }
        }
    }

    override fun testRunStopped(elapsedTime: Long) {}
    override fun testRunEnded(elapsedTime: Long, runMetrics: Map<String, String>?) {
        this.testRunEnded(elapsedTime, "", runMetrics)
    }

    override fun testRunEnded(elapsedTime: Long, output: String, runMetrics: Map<String, String>?) {
        synchronized(lock) {
            result = result.copy(output = output)
            onWorkFinished()
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(ResultListener::class.java)
    }
}