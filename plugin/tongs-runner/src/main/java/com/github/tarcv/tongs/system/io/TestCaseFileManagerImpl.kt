package com.github.tarcv.tongs.system.io

import com.github.tarcv.tongs.model.Device
import com.github.tarcv.tongs.model.Pool
import com.github.tarcv.tongs.model.TestCase
import com.github.tarcv.tongs.model.TestCaseEvent
import java.io.File

class TestCaseFileManagerImpl(
        private val fileManager: FileManager,
        private val pool: Pool,
        private val device: Device,
        private val testCaseEvent: TestCase
) : TestCaseFileManager {
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