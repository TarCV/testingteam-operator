/*
 * Copyright 2020 TarCV
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License.
 *
 * You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */

package com.github.tarcv.tongs.suite

import com.android.ddmlib.logcat.LogCatMessage
import com.android.ddmlib.testrunner.TestIdentifier
import com.github.tarcv.tongs.api.run.TestCaseEvent
import com.github.tarcv.tongs.api.testcases.NoTestCasesFoundException
import com.github.tarcv.tongs.api.testcases.TestCase
import com.github.tarcv.tongs.api.testcases.TestSuiteLoader
import com.github.tarcv.tongs.api.testcases.TestSuiteLoaderContext
import com.github.tarcv.tongs.device.clearLogcat
import com.github.tarcv.tongs.model.AndroidDevice
import com.github.tarcv.tongs.runner.AndroidTestRunFactory
import com.github.tarcv.tongs.runner.IRemoteAndroidTestRunnerFactory
import com.github.tarcv.tongs.runner.JsonInfoDecorder
import com.github.tarcv.tongs.runner.TestInfo
import com.github.tarcv.tongs.runner.listeners.LogcatReceiver
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import kotlinx.coroutines.*
import org.slf4j.LoggerFactory
import java.util.*

public class JUnitTestSuiteLoader(
        private val context: TestSuiteLoaderContext,
        private val testRunFactory: AndroidTestRunFactory,
        private val remoteAndroidTestRunnerFactory: IRemoteAndroidTestRunnerFactory) : TestSuiteLoader {
    private val logger = LoggerFactory.getLogger(JUnitTestSuiteLoader::class.java)

    companion object {
        const val logcatWaiterSleep: Long = 2500
        private val jsonInfoDecoder = JsonInfoDecorder()

        fun calculateDeviceIncludes(input: Sequence<Pair<AndroidDevice, Set<TestIdentifier>>>)
                : Map<TestIdentifier, List<AndroidDevice>> {
            return input
                    .flatMap { (device, tests) ->
                        tests
                                .asSequence()
                                .map { Pair(it, device) }
                    }
                    .groupBy({ it.first }) {
                        it.second
                    }
        }

        fun decodeMessages(testInfoMessages: Collection<LogCatMessage>): List<JsonObject> {
            class MessageKey(val id: String, val lineIndex: String): Comparable<MessageKey> {
                override fun compareTo(other: MessageKey): Int {
                    return id.compareTo(other.id)
                            .let {
                                if (it == 0) {
                                    lineIndex.compareTo(other.lineIndex)
                                } else {
                                    it
                                }
                            }
                }
            }

            val jsonParser = JsonParser()
            return testInfoMessages.asSequence()
                    .map { it.message }
                    .fold(ArrayList<Pair<MessageKey, String>>()) { acc, message ->
                        val (prefix, line) = message.split(':', limit = 2)
                        val (id, lineIndex) = prefix.split('-', limit = 2)

                        acc.apply {
                            acc.add(MessageKey(id, lineIndex) to line)
                        }
                    }
                    .groupBy({ it.first.id })
                    .values
                    .flatMap {
                        it
                                .sortedBy { it.first.lineIndex.toInt(16) }
                                .map { it.second }
                                .let {
                                    val json = it.joinToString("", "[", "]")
                                    jsonParser
                                            .parse(json)
                                            .asJsonArray
                                            .asSequence()
                                            .filter { !it.isJsonNull }
                                            .map { it.asJsonObject }
                                            .toList()
                                }
                    }
        }

    }

    @Throws(NoTestCasesFoundException::class)
    override fun loadTestSuite(): Collection<TestCaseEvent> = runBlocking {
        context.pool.devices
                .filterIsInstance(AndroidDevice::class.java) // TODO: handle other types of devices
                .map { device ->
                    async {
                        try {
                            collectTestsFromLogOnlyRun(device)
                        } catch (e: InterruptedException) {
                            throw e
                        } catch (e: Exception) {
                            // TODO: specific exception
                            throw RuntimeException("Failed to collect test cases from ${device.name}", e)
                        }
                    }
                }
                .awaitAll()
                .let { collectedInfos ->
                    finalizeTestInformation(collectedInfos)
                }
    }

    private fun finalizeTestInformation(collectedInfos: List<CollectedInfo>): Collection<TestCaseEvent> {
        val devicesInfo = collectedInfos
                .asSequence()
                .map { it.device to it.tests }
                .let { calculateDeviceIncludes(it) }

        val annotationInfos = collectedInfos
                .asSequence()
                .flatMap { it.infoMessages.entries.asSequence() }
                .associateBy({ it.key }) {
                    it.value
                }
        val allTests = devicesInfo.keys

        val testsWithoutInfo = allTests - annotationInfos.keys
        if (testsWithoutInfo.isNotEmpty()) {
            throw RuntimeException(
                    "In pool ${context.pool.name} received no additional information" +
                            " for ${testsWithoutInfo.joinToString(", ")}")
        }

        return annotationInfos
                .map { (identifier, info) ->
                    val testCase = TestCase(
                            ApkTestCase::class.java,
                            info.`package`,
                            identifier.className,
                            identifier.testName,
                            info.readablePath,
                            emptyMap(),
                            info.annotations
                    )
                    TestCaseEvent(
                            testCase,
                            devicesInfo[identifier] ?: emptyList(),
                            emptyList(),
                            0
                    )
                }
    }

    private suspend fun collectTestsFromLogOnlyRun(device: AndroidDevice): CollectedInfo {
        val testCollectingListener = TestCollectingListener()
        val logCatCollector: LogcatReceiver = LogcatReceiver(device)
        val testRun = testRunFactory.createCollectingRun(
                device, context.pool, testCollectingListener)

        val testInfoMessages: List<LogCatMessage> = withContext(Dispatchers.IO) {
            try {
                clearLogcat(device.deviceInterface)
                logCatCollector.start(this@JUnitTestSuiteLoader.javaClass.simpleName)

                testRun.execute()

                delay(JUnitTestSuiteLoader.logcatWaiterSleep) // make sure all logcat messages are read
                logCatCollector.stop()

                logCatCollector.messages
                        .filter { logCatMessage -> "Tongs.TestInfo" == logCatMessage.tag }
            } finally {
                logCatCollector.stop()
            }
        }
        val deviceTests = testCollectingListener.tests
        val testInfos = tryCollectingAndDecodingInfos(testInfoMessages)
        return CollectedInfo(device, deviceTests, testInfos)
    }

    internal fun tryCollectingAndDecodingInfos(
            testInfoMessages: List<LogCatMessage>
    ): Map<TestIdentifier, TestInfo> {
        return try {
            decodeMessages(testInfoMessages)
                    .let {
                        jsonInfoDecoder.decodeStructure(it.toList())
                    }
                    .asReversed() // make sure the first entry for duplicate keys is used
                    .associateBy { it.identifier }
        } catch (e: Exception) {
            logger.warn("Failed to collect annotation and structure information about tests", e)
            emptyMap()
        }
    }

    private class CollectedInfo(
            val device: AndroidDevice,
            val tests: Set<TestIdentifier>,
            val infoMessages: Map<TestIdentifier, TestInfo>
    )
}
