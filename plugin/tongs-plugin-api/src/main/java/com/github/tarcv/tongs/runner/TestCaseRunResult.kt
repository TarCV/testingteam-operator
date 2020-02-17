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

import com.github.tarcv.tongs.injector.GsonInjector.gson
import com.github.tarcv.tongs.model.Device
import com.github.tarcv.tongs.model.Pool
import com.github.tarcv.tongs.model.Pool.Builder.aDevicePool
import com.github.tarcv.tongs.model.TestCase
import com.github.tarcv.tongs.runner.Table.Companion.tableFromFile
import com.github.tarcv.tongs.summary.ResultStatus
import com.github.tarcv.tongs.summary.TestResult.SUMMARY_KEY_TOTAL_FAILURE_COUNT
import com.github.tarcv.tongs.system.io.FileType
import com.github.tarcv.tongs.system.io.TestCaseFileManager
import com.google.gson.Gson
import java.io.File
import java.nio.charset.StandardCharsets

// TODO: merge with com.github.tarcv.tongs.summary.TestResult
data class TestCaseRunResult(
        val pool: Pool,
        val device: Device,
        val testCase: TestCase,
        val status: ResultStatus,
        val stackTrace: String = "",
        val timeTakenMillis: Int,
        val totalFailureCount: Int,
        val metrics: Map<String, String>,
        val coverageReport: TestCaseFile? = null,
        val data: List<TestReportData>
) {
    val timeTakenSeconds: Float
        get() = timeTakenMillis / 1000f

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
            return TestCaseRunResult(pool, device, TestCase(testMethod, testClass), status, trace, 15, totalFailureCount, metrics, null, emptyList())
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
class MonoTextReportData(title: String, val type: Type, val text: String): TestReportData(title) {
    enum class Type {
        STDOUT,
        STRERR,
        OTHER
    }
}
class FileMonoTextReportData(title: String, val type: MonoTextReportData.Type, private val textPath: TestCaseFile)
    : TestReportData(title) {
    val text: String
        get() {
            return textPath.toFile()
                    .readText(StandardCharsets.UTF_8)
        }
}

class HtmlReportData(title: String, val html: String): TestReportData(title)
class FileHtmlReportData(title: String, private val htmlPath: TestCaseFile): TestReportData(title) {
    val html: String
        get() {
            return htmlPath.toFile()
                    .readText(StandardCharsets.UTF_8)
        }
}

class TableReportData(title: String, val table: Table): TestReportData(title)
class FileTableReportData(title: String, private val tablePath: TestCaseFile): TestReportData(title) {
    val table: Table
        get() = tableFromFile(tablePath)
}

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

    private class TableJson(
            var headers: Collection<String>? = null,
            var rows: Collection<Collection<String>>? = null
    )

    fun writeToFile(output: TestCaseFile, gson: Gson = gson()) {
        val headerStrings = headers.map { it.title }
        val rowStringLists = rows
                .map {
                    it.cells.map { it.text }
                }
        val adaptedForJson = TableJson(headerStrings, rowStringLists)

        output.create()
                .bufferedWriter(Charsets.UTF_8)
                .use { writer ->
                    gson.toJson(adaptedForJson, writer)
                }
    }

    companion object {
        fun tableFromFile(tablePath: TestCaseFile, gson: Gson = gson()): Table {
            return tablePath.toFile()
                    .bufferedReader(Charsets.UTF_8)
                    .use { reader ->
                        gson.fromJson(reader, TableJson::class.java)
                    }
                    .let {
                        val headers = it.headers
                        val rowsStringLists = it.rows
                        if (rowsStringLists.isNullOrEmpty()) {
                            Table(
                                    it.headers ?: emptyList(),
                                    emptyList()
                            )
                        } else {
                            if (headers.isNullOrEmpty()) {
                                throw RuntimeException("Table headers must not be empty when rows are present")
                            }
                            Table(headers, rowsStringLists)
                        }
                    }
        }

        private fun fixHeaders(headers: Collection<String>) = headers.map { Header(it) }.toList()

        private fun fixRows(rows: Collection<Collection<String>>, fixedHeaders: List<Header>): List<Row> {
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