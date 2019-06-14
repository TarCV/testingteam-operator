package com.github.tarcv.tongs.test

import org.junit.Assume.assumeTrue
import org.junit.Test
import java.nio.file.Files
import java.nio.file.Paths

class FunctionalLogTest {
    @Test
    fun testClearBeforeTests() {
        (getTestLineGroups(0) + getTestLineGroups(1)).entries
                .filter { entry -> entry.key.isNotEmpty() }
                .map { entry -> entry.value }
                .forEach {
            val beforeLines = it.subList(0, it.size - 1)
            assert(beforeLines.count { line -> line == "pm clear com.github.tarcv.tongstestapp" } == 1)
            assert(beforeLines.count { line -> line == "pm clear com.github.tarcv.tongstestapp.test" } == 1)
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

    /*@Test
    fun testPermissionGrantBeforeTests() {
        val grantTests = setOf(
                "com.github.tarcv.test.GrantPermissionsForClassTest#testPermissionGranted1",
                "com.github.tarcv.test.GrantPermissionsForClassTest#testPermissionGranted2",
                "com.github.tarcv.test.GrantPermissionsForInheritedClassTest#testPermissionGranted1",
                "com.github.tarcv.test.GrantPermissionsForInheritedClassTest#testPermissionGranted2",
                "com.github.tarcv.test.GrantPermissionsTest#testPermissionGranted"
        )
        (getTestLineGroups(0) + getTestLineGroups(1)).entries.forEach { entry ->
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
    }*/

    private fun getTestLineGroups(whichDevice: Int): Map<String, List<String>> {
        assumeTrue(System.getenv("CI_STUBBED") == "true")

        val lineRegex = Regex("""^\d+\s+(.+)$""")
        val testNameRegex = Regex(""".*-e filterMethod (\S+)\s.*-e filterClass (\S+)\s.*""")
        return Files.readAllLines(Paths.get("..", "tongs-${5554 + whichDevice*2}_adb.log"))
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
                    val nameMatch = testNameRegex.matchEntire(group.last())
                    if (nameMatch != null) {
                        "${nameMatch.groupValues[2]}#${nameMatch.groupValues[1]}" to group.toList()
                    } else {
                        "" to group.toList()
                    }
                }
    }
}