/*
 * Copyright 2019 TarCV
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

import com.github.tarcv.tongs.injector.summary.HtmlGeneratorInjector.htmlGenerator
import com.github.tarcv.tongs.model.Device.TEST_DEVICE
import com.github.tarcv.tongs.model.Pool.Builder.aDevicePool
import com.github.tarcv.tongs.model.TestCase
import com.github.tarcv.tongs.runner.*
import com.github.tarcv.tongs.system.io.FileType
import com.github.tarcv.tongs.system.io.StandardFileTypes.*
import com.github.tarcv.tongs.system.io.TestCaseFileManager
import org.junit.Assert
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class TemplateTest {
    val pool = aDevicePool()
            .addDevice(TEST_DEVICE)
            .withName("TestPool")
            .build()

    @get:Rule
    val temporaryFolder = TemporaryFolder()

    @Test
    fun fullTestResultPageIsCorrect() {
        val manager = object : TestCaseFileManager {
            override fun createFile(fileType: FileType): File {
                TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
            }

            override fun createFile(fileType: FileType, sequenceNumber: Int): File {
                TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
            }

            override fun createFile(fileType: FileType, suffix: String): File {
                TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
            }

            override fun getFile(fileType: FileType, suffix: String): File {
                return File("${fileType.directory}/file$suffix${fileType.suffix}")
            }

            override fun getRelativeFile(fileType: FileType, suffix: String): File {
                return File("${fileType.directory}/file$suffix${fileType.suffix}")
            }

        }
        val model = aTestCaseRunResult(datas = listOf(
                TableReportData("Logcat", tableOf(listOf(
                        "appName",
                        "logLevel",
                        "message",
                        "pid",
                        "tag",
                        "tid",
                        "time"
                ),
                        listOf("App", "Warn", "message1", "1234", "TAG", "5678", "12:12:12 12-12-2019"),
                        listOf("App", "Warn", "message2", "1234", "TAG", "5678", "12:12:13 12-12-2019")
                )),
                LinkedFileReportData("Logcat", TestCaseFile(manager, RAW_LOG, "")),
                LinkedFileReportData("Logcat as JSON", TestCaseFile(manager, JSON_LOG, "")),
                VideoReportData("Screen recording", TestCaseFile(manager, SCREENRECORD, ""))
        ))
        htmlGenerator().generateHtml("tongspages/pooltest.html",
                temporaryFolder.root, "test.html")
    }

    @Test
    fun tableDataIsCorrectlyDisplayed() {
        val model = aTestCaseRunResult(datas = listOf(TableReportData("Table-title",
                tableOf(
                        listOf("foo", "bar"),
                        listOf("1", "2"),
                        listOf("3", "4")
                )
        )))
        val result = htmlGenerator().generateHtmlFromInline("""
            {{#data}}
                - title: {{title}}
                {{#table}}
                    {{#headers}}|{{title}}{{/headers}}|
                    {{#rows}}
                    {{#cells}}{{.}}-{{/cells}}
                    {{/rows}}
                {{/table}}
            {{/data}}
            """.trimIndent(), model)
                .lines()
                .filter { it.isNotBlank() }
                .joinToString("\n")

        Assert.assertEquals("""
            |    - title: Table-title
            |        |foo|bar|
            |        1-2-
            |        3-4-"""
                .trimMargin("|"), result)
    }

    private fun aTestCaseRunResult(datas: List<TestReportData>): TestCaseRunResult {
        return TestCaseRunResult(
                pool, TEST_DEVICE,
                TestCase("method", "Class"),
                ResultStatus.FAIL, "stackTrace\n\ttrace",
                10F, 3,
                mapOf("metric" to "value"), null,
                datas
        )
    }
}