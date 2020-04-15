/*
 * Copyright 2020 TarCV
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License.
 *
 * You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */

package com.github.tarcv.tongs.test

import com.github.tarcv.test.BuildConfig.FLAVOR
import com.github.tarcv.test.Config.PACKAGE
import com.github.tarcv.tongs.test.util.ResultsSupplier
import com.github.tarcv.tongs.test.util.TestResult
import org.apache.commons.exec.CommandLine
import org.apache.commons.exec.DefaultExecutor
import org.apache.commons.exec.PumpStreamHandler
import org.junit.Assume
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import java.io.ByteArrayOutputStream
import java.io.File
import java.nio.file.Files
import java.util.concurrent.atomic.AtomicInteger
import kotlin.test.asserter

@RunWith(Parameterized::class)
class FunctionalSummaryTest(name: String, private val supplier: ResultsSupplier) {
    private val packageForRegex = PACKAGE.replace(".", """\.""")

    @Test
    fun testAllTestcasesExecutedExactlyOnce() {
        val simplifiedResults = getSimplifiedResults()

        simplifiedResults.fold(HashSet<String>()) { acc, result ->
            assert(acc.add(result.first)) { "All tests should be executed only once (${result.first} was executed more times)" }
            acc
        }

        val filteredNum = if (FLAVOR == "f2") {
            1 * 4
        } else {
            2 * 4
        }
        // Numbers are test case counts per classes in alphabetical order
        asserter.assertEquals("All tests should be executed", 12+filteredNum+2+2+2+2+1+8+8+2+4, simplifiedResults.size)
    }

    @Test
    fun testNumberedParameterizedTestExecutedCorrectly() {
        doAssertionsForParameterizedTests(
                """$packageForRegex\.ParameterizedTest#test\[\d+]""".toRegex(), 8)
    }

    @Test
    fun testNamedParameterizedTestExecutedCorrectly() {
        doAssertionsForParameterizedTests(
                """$packageForRegex\.ParameterizedNamedTest#test\[\s*param = .+]""".toRegex(), 8)
    }

    @Test
    fun testDangerousNamesTestExecutedCorrectly() {
        doAssertionsForParameterizedTests(
                """$packageForRegex\.DangerousNamesTest#test\[\s*param = .+]""".toRegex(), 12)
    }

    @Test
    fun testApi22IsOnlyExecutedOnTheSecondDevice() {
        val simplifiedResults = getSimplifiedResults()
        val testsPerDevice = simplifiedResults
                .filter { """$packageForRegex\.FilteredTest#api22Only\[\s*.+]""".toRegex().matches(it.first) }
                .fold(HashMap<String, AtomicInteger>()) { acc, test ->
                    acc
                            .computeIfAbsent(test.second) { AtomicInteger(0) }
                            .incrementAndGet()
                    acc
                }
                .entries
                .toList()
        assert(testsPerDevice.isNotEmpty()) { "API 20 only tests should be executed" }
        assert(testsPerDevice.size == 1) { "API 20 only should be executed on exactly 1 device (got ${testsPerDevice.size})" }
        assert(testsPerDevice[0].value.get() == 4) { "Exactly 4 API 20 only test cases should be executed on ${testsPerDevice[0].key} device" }
    }

    @Test
    fun testFilteredByF2FilterIsExecutedForF1() {
        Assume.assumeTrue(FLAVOR != "f2")
        doAssertionsForParameterizedTests(
                """$packageForRegex\.FilteredTest#filteredByF2Filter\[\d+]""".toRegex(), 4)
    }

    @Test
    fun testFilteredByF2FilterAreNotExecutedForF2() {
        Assume.assumeTrue(FLAVOR == "f2")
        doAssertionsForParameterizedTests(
                """$packageForRegex\.FilteredTest#filteredByF2Filter\[\d+]""".toRegex(), 0)
    }

    @Test
    fun testVideoRecorderIsCalledWithGoodFilename() {
        Assume.assumeTrue("Executing on a *NIX system", File(shellBinary).exists())
        Assume.assumeTrue("Running tests with stubbed ADB",
                System.getenv("CI_STUBBED")?.toBoolean() ?: false)

        readAdbLogLines { lines ->
            lines.filter { it.contains("[START SCREEN RECORDER] ") }
                    .map {
                        """.+\[START SCREEN RECORDER] (.+?),\{.+"""
                                .toRegex()
                                .matchEntire(it)
                                ?.groupValues
                                ?.get(1)
                                ?: throw AssertionError("Unexpected screenrecord line ${it}")
                    }
                    .forEach { actualPath ->
                        this@FunctionalSummaryTest.assertNotMangledByShell(actualPath)
                    }
        }
    }

    @Test
    fun testSuiteIsExecutedForLogWithGoodArgument() {
        Assume.assumeTrue("Executing on a *NIX system", File(shellBinary).exists())
        Assume.assumeTrue("Running tests with stubbed ADB",
                System.getenv("CI_STUBBED")?.toBoolean() ?: false)

        readAdbLogLines { lines ->
            lines.filter { it.contains("[START SCREEN RECORDER] ") }
                    .map {
                        """.+\[START SCREEN RECORDER] (.+?),\{.+"""
                                .toRegex()
                                .matchEntire(it)
                                ?.groupValues
                                ?.get(1)
                                ?: throw AssertionError("Unexpected screenrecord line ${it}")
                    }
                    .forEach { actualPath ->
                        this@FunctionalSummaryTest.assertNotMangledByShell(actualPath)
                    }
        }
    }

    @Test
    fun testCasesAreExecutedWithGoodArgument() {
        Assume.assumeTrue("Executing on a *NIX system", File(shellBinary).exists())
        Assume.assumeTrue("Running tests with stubbed ADB",
                System.getenv("CI_STUBBED")?.toBoolean() ?: false)

        readAdbLogLines { lines ->
            lines.filter { it.contains("[START SCREEN RECORDER] ") }
                    .map {
                        """.+\[START SCREEN RECORDER] (.+?),\{.+"""
                                .toRegex()
                                .matchEntire(it)
                                ?.groupValues
                                ?.get(1)
                                ?: throw AssertionError("Unexpected screenrecord line ${it}")
                    }
                    .forEach { actualPath ->
                        this@FunctionalSummaryTest.assertNotMangledByShell(actualPath)
                    }
        }
    }

    @Test
    fun testExpectedPropertiesFromAnnotationsAreInJsons() {
        val testResults = getTestResults()

        val normalPropertiesTest = testResults.single { it.testMethod == "normalPropertiesTest" }
        with(normalPropertiesTest.additionalProperties) {
            assert(getValue("x") == "1")
            assert(getValue("y") == "2")
            assert(keys == setOf("x", "y"))
        }

        val normalPropertyPairsTest = testResults.single { it.testMethod == "normalPropertyPairsTest" }
        with(normalPropertyPairsTest.additionalProperties) {
            assert(getValue("v") == "1")
            assert(getValue("w") == "2")
            assert(keys == setOf("v", "w"))
        }
    }

    private fun readAdbLogLines(block: (List<String>) -> Unit) {
        getAdbLogFiles(".").let {
            if (it.isEmpty()) {
                getAdbLogFiles("..")
            } else {
                it
            }
        }
                .also {
                    assert(it.size == 2) {
                        "there should be 2 adb log files (got ${it.size})"
                    }
                }
                .forEach { file ->
                    Files
                            .readAllLines(file.toPath())
                            .let(block)
                }
    }

    private fun assertNotMangledByShell(str: String) {
        val echoCmd = CommandLine(shellBinary)
                .addArgument("-c")
                .addArgument("echo \$1", false)
                .addArgument("--")
                .addArgument(str, false)
        val receivedPath = executeCommandWithOutput(echoCmd).trim()

        assert(str.length - receivedPath.length <= 5) {
            "Path passed to screenrecord command should not be" +
                    " mangled by the shell, test 2 (got $receivedPath)"
        }
    }

    private fun getAdbLogFiles(dirPath: String): Array<out File> {
        val currentDir = File(dirPath)
        return currentDir
                .listFiles { dir, filename ->
                    filename.toLowerCase().endsWith("_adb.log")
                            && currentDir == dir
                }
    }

    private fun doAssertionsForParameterizedTests(pattern: Regex, expectedCount: Int) {
        val simplifiedResults = getSimplifiedResults()

        val testsPerDevice = simplifiedResults
                .filter { pattern.matches(it.first) }
                .fold(HashMap<String, AtomicInteger>()) { acc, test ->
                    acc
                            .computeIfAbsent(test.second) { _ -> AtomicInteger(0) }
                            .incrementAndGet()
                    acc
                }
                .entries
                .toList()
        if (expectedCount > 0) {
            assert(testsPerDevice.isNotEmpty()) { "Parameterized tests should be executed" }
            if (expectedCount >= 8) {
                assert(testsPerDevice.size == 2) { "Variants should be executed on exactly 2 devices (got ${testsPerDevice.size})" }
                assert(testsPerDevice[0].value.get() > 0) { "At least one parameterized test should be executed on ${testsPerDevice[0].key} device" }
                assert(testsPerDevice[1].value.get() > 0) { "At least one parameterized test should be executed on ${testsPerDevice[1].key} device" }
            }
            assert(testsPerDevice[0].value.get() + testsPerDevice[1].value.get() == expectedCount) {
                "Exactly $expectedCount parameterized tests should be executed" +
                        " (device1=${testsPerDevice[0].value.get()}, device2=${testsPerDevice[1].value.get()})"
            }
        } else {
            assert(testsPerDevice.isEmpty()) { "The parameterized tests should not be executed" }
        }
    }

    private fun getSimplifiedResults(
            includePackages: List<String> = listOf(PACKAGE)
    ): List<Pair<String, String>> {
        val packageRegexes = includePackages.map { Regex("^${Regex.escape(it)}\\.[A-Z].*$") }
        return getTestResults()
                .filter {
                    val testClass = it.testClass
                    packageRegexes.any { testClass.matches(it) }
                }
                .map {
                    "${it.testClass}#${it.testMethod}" to it.deviceSerial
                }
    }

    private fun getTestResults(): List<TestResult> {
        val poolSummaries = supplier.summarySupplier()

        return poolSummaries.single()
                .deviceResults
                .flatMap { it.testResults }
    }

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun provideResultsSuppliers(): List<Array<Any>> {
            return ResultsSupplier.allSuppliers
                    .map {
                        arrayOf(it.supplierName, it)
                    }
        }
    }
}

private const val shellBinary = "/bin/sh"

private fun executeCommandWithOutput(cmd: CommandLine): String {
    val outputStream = ByteArrayOutputStream()
    val pumper = PumpStreamHandler(outputStream)
    DefaultExecutor().apply {
        setExitValue(0)
        streamHandler = pumper
        execute(cmd)
    }
    return outputStream.toString()
}
