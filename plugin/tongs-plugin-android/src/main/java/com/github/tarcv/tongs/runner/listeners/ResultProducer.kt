/*
 * Copyright 2020 TarCV
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
package com.github.tarcv.tongs.runner.listeners

import com.github.tarcv.tongs.TongsConfiguration
import com.github.tarcv.tongs.injector.GsonInjector.gson
import com.github.tarcv.tongs.model.*
import com.github.tarcv.tongs.runner.*
import com.github.tarcv.tongs.runner.rules.TestCaseRunRuleContext
import com.github.tarcv.tongs.summary.DeviceTestFilesRetrieverImpl
import com.github.tarcv.tongs.summary.ResultStatus
import com.github.tarcv.tongs.summary.TestResult.SUMMARY_KEY_TOTAL_FAILURE_COUNT
import com.github.tarcv.tongs.system.io.TestCaseFileManager
import org.simpleframework.xml.core.Persister
import java.util.concurrent.atomic.AtomicReference

interface IResultProducer {
    fun requestListeners(): List<BaseListener>
    fun getResult(): TestCaseRunResult
}

class TestCollectorResultProducer(private val pool: Pool, private val device: AndroidDevice): IResultProducer {
    override fun requestListeners(): List<BaseListener> = emptyList()

    override fun getResult(): TestCaseRunResult {
        return TestCaseRunResult(
                pool, device, TestCase("dummy", "dummy"),
                ResultStatus.PASS,
                "", 0F, 0, emptyMap(), null, emptyList()
        )
    }

}

class ResultProducer(
        private val context: TestCaseRunRuleContext,
        private val latch: PreregisteringLatch
) : IResultProducer {
    private val androidDevice = context.device as AndroidDevice
    private val testStatus = AtomicReference<ResultStatus>(ResultStatus.UNKNOWN)
    private val resultListener = ResultListener(context.testCaseEvent, testStatus, latch)
    private val xmlReportListener = AndroidXmlTestRunListener(context.fileManager)
    private val wrappedXmlReportListener = BaseListenerWrapper(latch, xmlReportListener)
    private val logCatListener = LogCatTestRunListener(gson(), context.fileManager, context.pool, androidDevice, latch)
    private val screenTraceListener = getScreenTraceTestRunListener(context.fileManager, context.pool, androidDevice, latch)
    private val coverageListener = getCoverageTestRunListener(context.configuration, androidDevice, context.fileManager, context.pool, context.testCaseEvent, latch)

    override fun requestListeners(): List<BaseListener> {
        if (testStatus.get() != ResultStatus.UNKNOWN) {
            throw IllegalStateException("Can't request listeners once tests are executed")
        }
        return listOf(
                resultListener,
                wrappedXmlReportListener,
                logCatListener,
                screenTraceListener,
                coverageListener)
    }

    override fun getResult(): TestCaseRunResult {
        val xmlResults = DeviceTestFilesRetrieverImpl(null, Persister())
                .parseTestResultsFromFile(xmlReportListener.file.toFile(), androidDevice)
        val xmlResult = xmlResults.single()

        val runStatus = testStatus.get()
        val status = when (runStatus) {
            ResultStatus.UNKNOWN, ResultStatus.ERROR -> ResultStatus.ERROR
            ResultStatus.FAIL ->
                when(xmlResult.resultStatus) {
                    ResultStatus.FAIL -> ResultStatus.FAIL
                    else -> ResultStatus.ERROR
                }
            else -> xmlResult.resultStatus
        }

        val reportBlocks = listOf(
                // TODO: add ADB shell stdout/stderr log
                TableReportData("Logcat", logCatListener.getAsTable()),
                LinkedFileReportData("Logcat", logCatListener.rawFile),
                LinkedFileReportData("Logcat as JSON", logCatListener.jsonFile),
                addTraceReport(screenTraceListener)
        ).filterNotNull()

        val coverageReport = if (coverageListener is CoverageListener) {
            coverageListener.coverageFile
        } else {
            null
        }

        return TestCaseRunResult(
                context.pool, androidDevice,
                context.testCaseEvent.testCase,
                status,
                xmlResult.trace,
                xmlResult.timeTaken,
                (xmlResult.metrics[SUMMARY_KEY_TOTAL_FAILURE_COUNT] ?: "0").toInt(),
                xmlResult.metrics,
                coverageReport,
                reportBlocks)
    }

    private fun addTraceReport(screenTraceListener: BaseListener): TestReportData? {
        val dataTitle = "Screen recording"
        return if (screenTraceListener is ScreenRecorderTestRunListener) {
            VideoReportData(dataTitle, screenTraceListener.file)
        } else if (screenTraceListener is ScreenCaptureTestRunListener) {
            ImageReportData(dataTitle, screenTraceListener.file)
        } else {
            null
        }
    }

    private fun getScreenTraceTestRunListener(fileManager: TestCaseFileManager, pool: Pool, device: AndroidDevice, latch: PreregisteringLatch): BaseListener {
        return if (Diagnostics.VIDEO == device.supportedVisualDiagnostics) {
            ScreenRecorderTestRunListener(fileManager, pool, device, latch)
        } else if (Diagnostics.SCREENSHOTS == device.supportedVisualDiagnostics && context.configuration.canFallbackToScreenshots()) {
            ScreenCaptureTestRunListener(fileManager, pool, device, latch)
        } else {
            BaseListenerWrapper(null, NoOpITestRunListener())
        }
    }

    private fun getCoverageTestRunListener(configuration: TongsConfiguration,
                                           device: AndroidDevice,
                                           fileManager: TestCaseFileManager,
                                           pool: Pool,
                                           testCase: TestCaseEvent,
                                           latch: PreregisteringLatch): BaseListener {
        return if (configuration.isCoverageEnabled) {
            CoverageListener(device, fileManager, pool, testCase, latch)
        } else BaseListenerWrapper(null, NoOpITestRunListener())
    }
}
