/*
 * Copyright 2020 TarCV
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
package com.github.tarcv.tongs.runner.listeners

import com.android.ddmlib.logcat.LogCatMessage
import com.github.tarcv.tongs.runner.Table
import com.github.tarcv.tongs.runner.TestCaseFile
import com.google.gson.Gson
import java.util.*
import java.util.function.Function
import java.util.stream.Collectors

class TableLogCatWriter(
        private val gson: Gson,
        private val file: TestCaseFile
) : LogCatWriter {
    override fun writeLogs(logCatMessages: List<LogCatMessage>) {
        convertToTable(logCatMessages)
                .writeToFile(file) { outputFile, tableJson ->
                    outputFile
                            .bufferedWriter(Charsets.UTF_8)
                            .use { writer ->
                                gson.toJson(tableJson, writer)
                            }
                }
    }

    fun convertToTable(messages: List<LogCatMessage>): Table {
        val headers = Arrays.asList("appName",
                "logLevel",
                "message",
                "pid",
                "tag",
                "tid",
                "time"
        )
        val rows: List<List<String>> = messages.stream()
                .map(Function { logCatMessage: LogCatMessage ->
                    Arrays.asList(logCatMessage.appName,
                            logCatMessage.logLevel.stringValue,
                            logCatMessage.message, logCatMessage.pid.toString(),
                            logCatMessage.tag, logCatMessage.tid.toString(),
                            logCatMessage.timestamp.toString()
                    )
                })
                .collect(Collectors.toList<List<String>>())
        return Table(headers, rows)
    }
}