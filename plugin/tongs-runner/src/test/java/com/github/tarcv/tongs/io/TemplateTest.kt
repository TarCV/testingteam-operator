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
package com.github.tarcv.tongs.io

import com.github.mustachejava.DefaultMustacheFactory
import com.github.mustachejava.MustacheFactory
import com.github.tarcv.tongs.model.Device
import com.github.tarcv.tongs.model.Diagnostics
import com.github.tarcv.tongs.model.DisplayGeometry
import com.github.tarcv.tongs.model.Pool.Builder.aDevicePool
import com.github.tarcv.tongs.model.TestCase
import com.github.tarcv.tongs.runner.*
import com.github.tarcv.tongs.summary.ResultStatus
import com.github.tarcv.tongs.system.io.FileType
import com.github.tarcv.tongs.system.io.StandardFileTypes.*
import com.github.tarcv.tongs.system.io.TestCaseFileManager
import org.junit.Assert
import org.junit.Test
import java.io.File
import java.io.StringReader
import java.io.StringWriter

class TemplateTest {
    companion object {
        private val mustacheFactory: MustacheFactory = DefaultMustacheFactory()
    }

    val device = object: Device() {
        override fun getSerial(): String = "DeviceSerial"

        override fun getManufacturer(): String = "DeviceManufacturer"

        override fun getModelName(): String = "DeviceModel"

        override fun getOsApiLevel(): Int = 25

        override fun getLongName(): String = "LongDeviceName"

        override fun getDeviceInterface(): Any = java.lang.Object()

        override fun isTablet(): Boolean = false

        override fun getGeometry(): DisplayGeometry? = DisplayGeometry(300)

        override fun getSupportedVisualDiagnostics(): Diagnostics = Diagnostics.VIDEO

        override fun getUniqueIdentifier(): Any = java.lang.Object()
    }
    val pool = aDevicePool()
            .addDevice(device)
            .withName("TestPool")
            .build()

    @Test
    fun fullTestResultPageIsCorrect() {
        val mustache = mustacheFactory.compile("tongspages/pooltest.html")
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
        File("C:\\Users\\const\\Documents\\repos\\fork\\templateTest\\pool\\tests\\test.html").bufferedWriter().use {
            mustache.execute(it, ReportWrapper(model))
        }
    }

    @Test
    fun tableDataIsCorrectlyDisplayed() {
        val mustache = StringReader("""
            {{#data}}
                {{#titleIs.Table title}}is Table{{/titleIs.Table title}}
                - title: {{title}}
                {{#table}}
                    {{#headers}}|{{.}}{{/headers}}|
                    {{#rows}}
                    {{#cells}}{{.}}-{{/cells}}
                    {{/rows}}
                {{/table}}
            {{/data}}
            """.trimIndent()).use {
            mustacheFactory.compile(it, "template")
        }
        val model = aTestCaseRunResult(datas = listOf(TableReportData("Table title",
                tableOf(
                        listOf("foo", "bar"),
                        listOf("1", "2"),
                        listOf("3", "4")
                )
        )))
        val result = StringWriter().use {
            mustache.execute(it, model)

            it.toString()
        }
        Assert.assertEquals("""
            |    - title: Table
            |        |foo|bar|
            |        1-2-
            |        3-4-
            |""".trimMargin("|"), result)
    }

    private fun aTestCaseRunResult(datas: List<TestReportData>): TestCaseRunResult {
        return TestCaseRunResult(
                pool, device,
                TestCase("method", "Class"),
                ResultStatus.FAIL, "stackTrace\n\ttrace",
                10F, 3,
                mapOf("metric" to "value"), null,
                datas
        )
    }
}