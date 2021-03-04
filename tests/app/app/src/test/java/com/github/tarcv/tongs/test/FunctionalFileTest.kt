package com.github.tarcv.tongs.test

import com.github.tarcv.test.BuildConfig
import com.github.tarcv.tongs.test.util.isRunStubbed
import org.junit.Assume
import org.junit.Test
import java.io.File
import java.nio.file.Files
import kotlin.test.asserter

class FunctionalFileTest {
    @Test
    fun testVideoRecordedForFailures() {
        Assume.assumeFalse("Running tests with real devices", isRunStubbed)

        val recordsForExpectedFailures = File("build/reports/tongs/${BuildConfig.FLAVOR}DebugAndroidTest/screenrecord")
                .walk(FileWalkDirection.TOP_DOWN)
                .filter { file ->
                    val fname = file.name
                    file.isFile &&
                            fname.endsWith(".mp4") &&
                            fname.contains("ResultTest") &&
                            fname.contains("failureFromEspresso")
                }
                .toList()
        asserter.assertEquals("There should be videos for 2 different tests",
                2,
                recordsForExpectedFailures
                        .map { it.name }
                        .distinct()
                        .size
        )
        asserter.assertTrue("Recorded videos should not be empty and big enough",
                recordsForExpectedFailures.all { Files.size(it.toPath()) > 100_000 }
        )
    }
}