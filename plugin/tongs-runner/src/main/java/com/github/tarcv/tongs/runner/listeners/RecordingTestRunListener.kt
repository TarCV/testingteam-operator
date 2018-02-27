/*
 * Copyright 2018 TarCV
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License.
 *
 * You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */

package com.github.tarcv.tongs.runner.listeners

import com.android.ddmlib.testrunner.ITestRunListener
import com.android.ddmlib.testrunner.TestIdentifier
import com.github.tarcv.tongs.model.Device
import java.io.File
import java.io.FileWriter

class RecordingTestRunListener(device: Device, isLogOnly: Boolean) : ITestRunListener {
    private val deviceSerial = device.serial
    private val writer: FileWriter by lazy {
        val filename = if (isLogOnly) {
            "eventsForLogOnly.log"
        } else {
            "events.log"
        }
        val logFile = File(filename)
        FileWriter(logFile, true)
    }
    private val separator = System.lineSeparator()

    override fun testRunStarted(runName: String?, testCount: Int) {
        writer.append("$deviceSerial testRunStarted $runName,$testCount$separator")
    }

    override fun testStarted(test: TestIdentifier?) {
        writer.append("$deviceSerial testStarted $test$separator")
    }

    override fun testAssumptionFailure(test: TestIdentifier?, trace: String?) {
        writer.append("$deviceSerial testAssumptionFailure $test,$trace$separator")
    }

    override fun testRunStopped(elapsedTime: Long) {
        writer.append("$deviceSerial testRunStopped $elapsedTime$separator")
    }

    override fun testFailed(test: TestIdentifier?, trace: String?) {
        writer.append("$deviceSerial testFailed $test,$trace$separator")
    }

    override fun testEnded(test: TestIdentifier?, testMetrics: Map<String, String>?) {
        val metricsString = testMetrics
                ?.entries
                ?.toList()
                ?.sortedBy { it.key }
                ?.joinToString(";") { "${it.key} -> ${it.value}" }
        writer.append("$deviceSerial testEnded $test,{$metricsString}$separator")
    }

    override fun testIgnored(test: TestIdentifier?) {
        writer.append("$deviceSerial testIgnored $test$separator")
    }

    override fun testRunFailed(errorMessage: String?) {
        writer.append("$deviceSerial testRunFailed $errorMessage$separator")
    }

    override fun testRunEnded(elapsedTime: Long, runMetrics: Map<String, String>?) {
        val metricsString = runMetrics
                ?.entries
                ?.toList()
                ?.sortedBy { it.key }
                ?.joinToString(";") { "${it.key} -> ${it.value}" }
        writer.append("$deviceSerial testRunEnded $elapsedTime,{$metricsString}$separator")
        writer.close()
    }
}