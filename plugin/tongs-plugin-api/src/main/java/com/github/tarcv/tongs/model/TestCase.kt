package com.github.tarcv.tongs.model

import com.github.tarcv.tongs.utils.ReadableNames
import java.util.Collections.emptyMap

data class TestCase @JvmOverloads constructor(
        val testMethod: String,
        val testClass: String,
        val properties: Map<String, String> = emptyMap()
) {
    val testClassSimpleName: String
        get() = testClass.split(".").last()

    val testMethodReadable: String
        get() = ReadableNames.readableTestMethodName(testMethod)

    /**
     * Returns a readable string uniquely identifying a test case for use in logs and file names.
     * In current implementation it consists of the testMethod name and the name of the test class
     */
    override fun toString(): String = "$testClass#$testMethod:"
}
