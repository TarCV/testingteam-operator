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

import com.android.ddmlib.testrunner.TestIdentifier
import com.github.tarcv.tongs.api.run.ResultStatus
import com.github.tarcv.tongs.api.run.TestCaseEvent
import com.github.tarcv.tongs.runner.PreregisteringLatch
import org.slf4j.LoggerFactory
import javax.annotation.concurrent.GuardedBy
import javax.annotation.concurrent.ThreadSafe

@ThreadSafe
class ResultListener(private val currentTestCaseEvent: TestCaseEvent,
                     latch: PreregisteringLatch) : BaseListener(latch), FullTestRunListener {

    private val lock = Any()

    @field:GuardedBy("lock") private var _result: ShellResult = ShellResult()

    fun finishAndGetResult(): ShellResult = synchronized(lock) {
        if (state != State.RUN_ENDED && state != State.RUN_FAILED) {
            logger.warn("Run ${currentTestCaseEvent.testCase} ended unexpectedly (probably the device got disconnected)")
            runEnded()
        }
        _result
    }

    @GuardedBy("lock") private var expectedTests = -1
    @GuardedBy("lock") private var state = State.BEFORE_RUN

    data class ShellResult(
            val status: ResultStatus? = null,
            val output: String = "",
            val metrics: Map<String, String> = emptyMap(),
            val trace: String = "",
            val startTime: Long? = null,
            val endTime: Long? = null
    )

    override fun testRunStarted(runName: String, testCount: Int) {
        synchronized(lock) {
            expectedTests = testCount
            if (testCount > 0) {
                checkAndUpdate(State.BEFORE_RUN, State.RUN_STARTED_OR_TEST_ENDED, null)
            } else {
                logger.error("No tests were found in ${currentTestCaseEvent.testCase} run")
                fatallyFailRun()
            }
        }
    }

    override fun testStarted(test: TestIdentifier) {
        synchronized(lock) {
            _result = _result.copy(startTime = System.currentTimeMillis())
            checkAndUpdate(State.RUN_STARTED_OR_TEST_ENDED, State.TEST_STARTED, null)
        }
    }
    override fun testFailed(test: TestIdentifier, trace: String) {
        synchronized(lock) {
            appendTrace(trace)
            testEndedWithStatus(ResultStatus.FAIL)
        }
    }

    override fun testAssumptionFailure(test: TestIdentifier, trace: String) {
        appendTrace(trace)
        testEndedWithStatus(ResultStatus.ASSUMPTION_FAILED)
    }

    override fun testIgnored(test: TestIdentifier) {
        testEndedWithStatus(ResultStatus.IGNORED)
    }

    override fun testEnded(test: TestIdentifier, testMetrics: Map<String, String>) {
        synchronized(lock) {
            if (state == State.TEST_STARTED) {
                testEndedWithStatus(ResultStatus.PASS)
            }

            mergeMetrics(testMetrics)
        }
    }

    override fun testRunFailed(errorMessage: String) {
        synchronized(lock) {
            try {
                appendTrace(errorMessage)
                fatallyFailRun()
            } finally {
                onWorkFinished()
            }
        }
    }

    private fun appendTrace(trace: String) {
        val newTrace = _result.trace.let {
            if (it.isNullOrBlank()) {
                trace
            } else {
                it + "\n\n" + trace
            }
        }
        _result = _result.copy(trace = newTrace)
    }

    override fun testRunStopped(elapsedTime: Long) {}
    override fun testRunEnded(elapsedTime: Long, runMetrics: Map<String, String>?) {
        this.testRunEnded(elapsedTime, "", runMetrics)
    }

    override fun testRunEnded(elapsedTime: Long, output: String, runMetrics: Map<String, String>?) {
        synchronized(lock) {
            _result = _result.copy(output = output)
            if (runMetrics != null) {
                mergeMetrics(runMetrics)
            }
            runEnded()
        }
    }

    @GuardedBy("lock")
    private fun runEnded() {
        if (state != State.RUN_FAILED && expectedTests != 0) {
            logger.error("Executed too little or too much tests in ${currentTestCaseEvent.testCase} run")
            fatallyFailRun()
        }
        onWorkFinished()
    }

    private fun testEndedWithStatus(resultStatus: ResultStatus) {
        synchronized(lock) {
            if (_result.startTime != null) {
                _result = _result.copy(endTime = System.currentTimeMillis())
            }

            expectedTests -= 1
            val newStatus = _result.status.let {
                if (it == null) {
                    resultStatus
                } else if (it.overrideCompareTo(resultStatus) >= 0) {
                    it
                } else {
                    resultStatus
                }
            }

            checkAndUpdate(State.TEST_STARTED, State.RUN_STARTED_OR_TEST_ENDED, newStatus)
        }
    }

    @GuardedBy("lock")
    private fun mergeMetrics(testMetrics: Map<String, String>) {
        val metrics = _result.metrics.let {
            if (it.isEmpty()) {
                testMetrics
            } else {
                // TODO: implement merging
                it + testMetrics
            }
        }
        _result = _result.copy(metrics = metrics)
    }

    @GuardedBy("lock")
    private fun checkAndUpdate(expectedState: State, nextState: State, newStatus: ResultStatus?) {
        if (state == expectedState) {
            if (newStatus != null) {
                _result = _result.copy(status = newStatus)
            }

            state = nextState
        } else {
            fatallyFailRun()
        }
        return Unit
    }

    @GuardedBy("lock")
    private fun fatallyFailRun() {
        _result = _result.copy(status = ResultStatus.ERROR)
        state = State.RUN_FAILED
    }

    private enum class State {
        BEFORE_RUN,
        RUN_STARTED_OR_TEST_ENDED,
        TEST_STARTED,
        RUN_ENDED,
        RUN_FAILED
    }

    companion object {
        private val logger = LoggerFactory.getLogger(ResultListener::class.java)
    }
}