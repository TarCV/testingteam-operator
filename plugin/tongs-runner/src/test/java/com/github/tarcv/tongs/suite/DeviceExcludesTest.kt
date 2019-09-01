package com.github.tarcv.tongs.suite

import com.android.ddmlib.testrunner.TestIdentifier
import com.github.tarcv.tongs.model.AndroidDevice
import com.github.tarcv.tongs.model.AndroidDevice.Builder.aDevice
import org.junit.Assert
import org.junit.Test

class DeviceExcludesTest {
    private val device1 = aDevice().build()
    private val device2 = aDevice().build()
    private val device3 = aDevice().build()
    private val device4 = aDevice().build()
    private val test1 = TestIdentifier("class", "test1")
    private val test2 = TestIdentifier("class", "test2")

    @Test
    fun testNoExcludes() {
        val input = listOf(
                device1 to setOf(test1, test2),
                device2 to setOf(test1, test2)
        )
        val allTests = setOf(test1, test2)
        val excludes = JUnitTestSuiteLoader.calculateDeviceExcludes(
                allTests,
                input
        )
        Assert.assertEquals(
                emptyMap<TestIdentifier, Collection<AndroidDevice>>(),
                excludes
        )
    }

    @Test
    fun testEverythingExcluded() {
        val input = listOf(
                device1 to emptySet<TestIdentifier>(),
                device2 to emptySet()
        )
        val allTests = setOf(test1, test2)
        val excludes = JUnitTestSuiteLoader.calculateDeviceExcludes(
                allTests,
                input
        )
        Assert.assertEquals(
                mapOf(
                        test1 to listOf(device1, device2),
                        test2 to listOf(device1, device2)
                ),
                excludes
        )
    }

    @Test
    fun testMixedExcludes() {
        val input = listOf(
                device1 to setOf(test1, test2),
                device2 to emptySet(),
                device3 to setOf(test1),
                device4 to setOf(test2)
        )
        val allTests = setOf(test1, test2)
        val excludes = JUnitTestSuiteLoader.calculateDeviceExcludes(
                allTests,
                input
        )
        Assert.assertEquals(
                mapOf(
                        test1 to listOf(device2, device4),
                        test2 to listOf(device2, device3)
                ),
                excludes
        )
    }
}