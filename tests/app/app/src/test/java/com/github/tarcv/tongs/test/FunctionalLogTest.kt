/*
 * Copyright 2019 TarCV
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License.
 *
 * You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */

package com.github.tarcv.tongs.test

import com.github.tarcv.test.BuildConfig.FLAVOR
import com.github.tarcv.tongs.test.util.isRunStubbed
import org.junit.Assume.assumeTrue
import org.junit.Test
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Paths

class FunctionalLogTest {
    @Test
    fun testRunnerIsCorrect() {
        (getTestLineGroups(0) + getTestLineGroups(1)).entries
                .filter { entry -> entry.key.isNotEmpty() }
                .map { entry -> entry.value }
                .map { it.last() }
                .forEach { runCommand ->
                    val expectedRunner = when (FLAVOR) {
                        "f1" ->"android.support.test.runner.AndroidJUnitRunner"
                        "f2" ->"com.github.tarcv.test.f2.TestRunner"
                        else -> throw AssertionError("Got an unexpected build flavor")
                    }
                    assert(runCommand.endsWith("/$expectedRunner")) {
                        "Command to run a test should contain $expectedRunner. Actual: $runCommand"
                    }
                }
    }

    @Test
    fun testFilterIsCorrect() {
        (getTestLineGroups(0) + getTestLineGroups(1)).entries
                .filter { entry -> entry.key.isNotEmpty() }
                .map { entry -> entry.value }
                .map { it.last() }
                .forEach { runCommand ->
                    val expectedFilter = when (FLAVOR) {
                        "f1" ->"com.github.tarcv.tongs.ondevice.ClassMethodFilter"
                        "f2" ->"com.github.tarcv.test.F2Filter,com.github.tarcv.tongs.ondevice.ClassMethodFilter"
                        else -> throw AssertionError("Got an unexpected build flavor")
                    }
                    assert(runCommand.contains("-e filter $expectedFilter")) {
                        "Command to run a test should contain $expectedFilter. Actual: $runCommand"
                    }
                }
    }

    @Test
    fun testCustomArgumentIsCorrect() {
        (getTestLineGroups(0) + getTestLineGroups(1)).entries
                .filter { entry -> entry.key.isNotEmpty() }
                .map { entry -> entry.value }
                .map { it.last() }
                .forEach { runCommand ->
                    val expectedArg = when (FLAVOR) {
                        "f1" ->"default"
                        "f2" ->"args\"ForF2" // should contain dangerous character
                        else -> throw AssertionError("Got an unexpected build flavor")
                    }
                    assert(runCommand.contains(Regex("\\s-e\\s+test_argument\\s+$expectedArg\\s"))) {
                        "Command to run a test should contain custom argument with '$expectedArg' value. Actual: $runCommand"
                    }
                }
    }

    @Test
    fun testClearBeforeTests() {
        (getTestLineGroups(0) + getTestLineGroups(1)).entries
                .filter { entry -> entry.key.isNotEmpty() }
                .map { entry -> entry.value }
                .forEach {
            val beforeLines = it.subList(0, it.size - 1)
            assert(beforeLines.count { line -> line == "pm clear com.github.tarcv.tongstestapp.$FLAVOR" } == 1)
            assert(beforeLines.count { line -> line == "pm clear com.github.tarcv.tongstestapp.$FLAVOR.test" } == 1)
        }
    }

    @Test
    fun testResetToHomeBeforeTests() {
        (getTestLineGroups(0) + getTestLineGroups(1)).entries
                .filter { entry -> entry.key.isNotEmpty() }
                .map { entry -> entry.value }
                .forEach {
            val beforeLines = it.subList(0, it.size - 1)
            val homeCommand = "input keyevent 3"
            val backCommand = "input keyevent 4"
            assert(beforeLines.count { line -> line == homeCommand } == 1)
            assert(beforeLines.count { line -> line == backCommand } == 1)
            assert(beforeLines.indexOf(homeCommand) < beforeLines.indexOf(backCommand))
        }
    }

    @Test
    fun testPermissionGrantBeforeTests() {
        val grantTests = setOf(
                "com.github.tarcv.test.GrantPermissionsForClassTest#testPermissionGranted1",
                "com.github.tarcv.test.GrantPermissionsForClassTest#testPermissionGranted2",
                "com.github.tarcv.test.GrantPermissionsForInheritedClassTest#testPermissionGranted1",
                "com.github.tarcv.test.GrantPermissionsForInheritedClassTest#testPermissionGranted2",
                "com.github.tarcv.test.GrantPermissionsTest#testPermissionGranted"
        )
        (getTestLineGroups(0) + getTestLineGroups(1)).entries
                .filter { entry -> entry.value.isNotEmpty() }
                .forEach { entry ->
                    val testCase = entry.key
                    val beforeLines = entry.value.subList(0, entry.value.size - 1)
                    val grantToApp = "pm grant com.github.tarcv.tongstestapp android.permission.WRITE_CALENDAR"
                    val grantToTest = "pm grant com.github.tarcv.tongstestapp.test android.permission.WRITE_CALENDAR"
                    if (grantTests.contains(testCase)) {
                        assert(beforeLines.count { line -> line == grantToApp } == 1)
                        assert(beforeLines.count { line -> line == grantToTest } == 1)
                    } else {
                        assert(beforeLines.none { line -> line == grantToApp })
                        assert(beforeLines.none { line -> line == grantToTest })
                    }
                }
    }

    private fun getTestLineGroups(whichDevice: Int): Map<String, List<String>> {
        assumeTrue(isRunStubbed)

        val lineRegex = Regex("""^\d+\s+(.+)$""")
        val testNameRegex = Regex(""".*-e tongs_filterClass (\S+)\s.*-e tongs_filterMethod (\S+).*""")
        return Files.readAllLines(
                Paths.get("..", "tongs-${5554 + whichDevice*2}_adb.log"),
                StandardCharsets.ISO_8859_1)
                .fold(arrayListOf(ArrayList<String>())) { acc, line ->
                    val result = lineRegex.matchEntire(line)!!
                    val lineNoTime = result.groupValues[1]

                    acc.last().add(lineNoTime)
                    if (lineNoTime.startsWith("am instrument") && !lineNoTime.contains("-e log true")) {
                        acc.add(ArrayList())
                    }

                    acc
                }
                .associate { group ->
                    if (group.isNotEmpty()) { // TODO: this check should not be needed
                        val nameMatch = testNameRegex.matchEntire(group.last())
                        if (nameMatch != null) {
                            "${nameMatch.groupValues[1]}#${nameMatch.groupValues[2]}" to group.toList()
                        } else {
                            "" to group.toList()
                        }
                    } else {
                        "" to group.toList()
                    }
                }
    }
}