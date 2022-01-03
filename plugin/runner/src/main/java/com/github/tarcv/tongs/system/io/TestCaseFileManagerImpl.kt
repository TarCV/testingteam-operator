/*
 * Copyright 2021 TarCV
 * Copyright 2018 Shazam Entertainment Limited
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
package com.github.tarcv.tongs.system.io

import com.github.tarcv.tongs.api.devices.Device
import com.github.tarcv.tongs.api.devices.Pool
import com.github.tarcv.tongs.api.result.FileType
import com.github.tarcv.tongs.api.result.TestCaseFileManager
import com.github.tarcv.tongs.api.testcases.TestCase
import java.io.File

class TestCaseFileManagerImpl(
        private val fileManager: FileManager,
        private val pool: Pool,
        private val device: Device,
        private val testCaseEvent: TestCase
) : TestCaseFileManager {
    override fun getFile(fileType: FileType, suffix: String): File {
        return fileManager.getFile(fileType, pool, device, testCaseEvent, suffix);
    }

    override fun getRelativeFile(fileType: FileType, suffix: String): File {
        return fileManager.getRelativeFile(fileType, pool, device, testCaseEvent, suffix);
    }

    override fun createFile(fileType: FileType): File {
        return fileManager.createFile(fileType, pool, device, testCaseEvent)
    }

    override fun createFile(fileType: FileType, sequenceNumber: Int): File {
        return fileManager.createFile(fileType, pool, device, testCaseEvent, sequenceNumber)
    }

    override fun createFile(fileType: FileType, suffix: String): File {
        return fileManager.createFile(fileType, pool, device, testCaseEvent, suffix)
    }
}