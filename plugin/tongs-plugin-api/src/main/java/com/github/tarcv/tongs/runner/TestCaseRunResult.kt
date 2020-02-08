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
package com.github.tarcv.tongs.runner

import com.github.tarcv.tongs.model.Device
import com.github.tarcv.tongs.model.Pool
import com.github.tarcv.tongs.model.Pool.Builder.aDevicePool
import com.github.tarcv.tongs.model.TestCase
import com.github.tarcv.tongs.summary.ResultStatus
import com.github.tarcv.tongs.summary.TestResult.SUMMARY_KEY_TOTAL_FAILURE_COUNT
import com.github.tarcv.tongs.system.io.FileType
import com.github.tarcv.tongs.system.io.TestCaseFileManager
import java.io.File

// TODO: merge with com.github.tarcv.tongs.summary.TestResult
data class TestCaseRunResult(
        val pool: Pool,
        val device: Device,
        val testCase: TestCase,
        val status: ResultStatus,
        val stackTrace: String = "",
        val timeTaken: Float,
        val totalFailureCount: Int,
        val metrics: Map<String, String>,
        val coverageReport: TestCaseFile? = null,
        val data: List<TestReportData>
) {
    companion object {
        private val pool = aDevicePool().addDevice(Device.TEST_DEVICE).build()

        @JvmStatic
        fun aTestResult(testClass: String, testMethod: String, status: ResultStatus, trace: String): TestCaseRunResult {
            return aTestResult(pool, Device.TEST_DEVICE, testClass, testMethod, status, trace)
        }

        @JvmStatic
        @JvmOverloads
        fun aTestResult(pool: Pool, device: Device, testClass: String, testMethod: String, status: ResultStatus, trace: String, metrics: Map<String, String> = emptyMap()): TestCaseRunResult {
            val totalFailureCount: Int = metrics
                    .get(SUMMARY_KEY_TOTAL_FAILURE_COUNT)
                    ?.let(Integer::parseInt)
                    ?: 0
            return TestCaseRunResult(pool, device, TestCase(testMethod, testClass), status, trace, 15f, totalFailureCount, metrics, null, emptyList())
        }
    }
}

class TestCaseFile(
        val fileManager: TestCaseFileManager,
        val fileType: FileType,
        val suffix: String
) {
    val relativePath: String
        get() = fileManager.getRelativeFile(fileType, suffix).path

    fun create(): File {
        return fileManager.createFile(fileType, suffix)
    }

    fun toFile(): File {
        return fileManager.getFile(fileType, suffix)
    }
}

/**
 * All child classes must have some uniqely named field that is not present in other child classes
 * (so that they can be distinguished in Mustache templates)
 */
sealed class TestReportData(
    val title: String
)
class HtmlReportData(title: String, val html: String): TestReportData(title)

class TableReportData(title: String, val table: Table): TestReportData(title)
class ImageReportData(title: String, private val image: TestCaseFile): TestReportData(title) {
    val imagePath: String
        get() = image.relativePath
}
class VideoReportData(title: String, private val video: TestCaseFile): TestReportData(title) {
    val videoPath: String
        get() = video.relativePath
}
class LinkedFileReportData(title: String, val file: TestCaseFile): TestReportData(title) {
    val linkedFilePath: String
        get() = file.relativePath
}

class Table(headerStrings: Collection<String>, rowStrings: Collection<Collection<String>>) {
    val headers: List<Header>
    val rows: List<Row>

    init {
        headers = fixHeaders(headerStrings)
        rows = fixRows(rowStrings, headers)
    }

    companion object {
        fun fixHeaders(headers: Collection<String>) = headers.map { Header(it) }.toList()

        fun fixRows(rows: Collection<Collection<String>>, fixedHeaders: List<Header>): List<Row> {
            return rows
                    .map { cells ->
                        val fixedCells = cells.mapIndexed { index, cell -> Cell(fixedHeaders[index], cell) }
                        Row(fixedCells)
                    }
                    .toList()
        }
    }
}
fun tableOf(headers: List<String>, vararg rows: List<String>) = Table(headers, rows.toList())

class Row(val cells: List<Cell>)
class Header(val title: String)
class Cell(val header: Header, val text: String) {
    override fun toString(): String = text
}