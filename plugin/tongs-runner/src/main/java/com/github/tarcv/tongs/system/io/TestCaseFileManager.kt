/*
 * Copyright 2019 TarCV
 * Copyright 2015 Shazam Entertainment Limited
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance
 * with the License.
 *
 * You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for
 * the specific language governing permissions and limitations under the License.
 */
package com.github.tarcv.tongs.system.io

import com.android.ddmlib.testrunner.TestIdentifier
import com.github.tarcv.tongs.model.Device
import com.github.tarcv.tongs.model.Pool
import com.github.tarcv.tongs.model.TestCaseEvent
import com.github.tarcv.tongs.runner.AndroidDeviceTestRunner

import java.io.File

class TestCaseFileManager(
        private val fileManager: FileManager,
        private val pool: Pool,
        private val device: Device,
        private val testCaseEvent: TestCaseEvent
) {
    fun createFile(fileType: FileType): File {
        return fileManager.createFile(fileType, pool, device, testCaseEvent)
    }

    fun createFile(fileType: FileType, sequenceNumber: Int): File {
        return fileManager.createFile(fileType, pool, device, testCaseEvent, sequenceNumber)
    }

    fun createFile(fileType: FileType, suffix: String): File {
        return fileManager.createFile(fileType, pool, device, testCaseEvent, suffix)
    }
}