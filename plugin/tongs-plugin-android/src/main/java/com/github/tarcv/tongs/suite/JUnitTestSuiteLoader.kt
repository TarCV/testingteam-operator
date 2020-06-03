package com.github.tarcv.tongs.suite

/*
 * Copyright 2020 TarCV
 * Copyright 2016 Shazam Entertainment Limited
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License.
 *
 * You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */

import com.android.ddmlib.DdmPreferences
import com.android.ddmlib.IDevice
import com.android.ddmlib.logcat.LogCatMessage
import com.android.ddmlib.testrunner.TestIdentifier
import com.github.tarcv.tongs.Utils.namedExecutor
import com.github.tarcv.tongs.injector.system.InstallerInjector.installer
import com.github.tarcv.tongs.model.AndroidDevice
import com.github.tarcv.tongs.api.devices.Device
import com.github.tarcv.tongs.api.run.TestCaseEvent
import com.github.tarcv.tongs.api.result.TestCaseRunResult
import com.github.tarcv.tongs.runner.*
import com.github.tarcv.tongs.runner.AndroidCollectingTestCaseRunRule
import com.github.tarcv.tongs.api.run.TestCaseRunRuleAfterArguments
import com.github.tarcv.tongs.api.run.ResultStatus
import com.github.tarcv.tongs.api.testcases.NoTestCasesFoundException
import com.github.tarcv.tongs.api.testcases.TestCase
import com.github.tarcv.tongs.api.testcases.TestSuiteLoader
import com.github.tarcv.tongs.api.testcases.TestSuiteLoaderContext
import com.github.tarcv.tongs.util.guessPackage
import com.google.gson.*
import org.slf4j.LoggerFactory
import java.time.Instant
import java.util.*
import java.util.concurrent.CountDownLatch
import java.util.concurrent.ExecutorService
import java.util.concurrent.TimeUnit
import java.util.function.BiConsumer
import java.util.function.BinaryOperator
import java.util.function.Function
import java.util.function.Supplier
import java.util.stream.Collector
import java.util.stream.Collectors
import kotlin.collections.HashMap

public class JUnitTestSuiteLoader(
        private val context: TestSuiteLoaderContext,
        private val testRunFactory: AndroidTestRunFactory,
        private val remoteAndroidTestRunnerFactory: IRemoteAndroidTestRunnerFactory) : TestSuiteLoader {
    private val logger = LoggerFactory.getLogger(JUnitTestSuiteLoader::class.java)

    @Throws(NoTestCasesFoundException::class)
    override fun loadTestSuite(): Collection<TestCaseEvent> {
        val result = ArrayList(askDevicesForTests())
        logger.debug("Found tests: $result")

        return result
    }

    private fun askDevicesForTests(): Collection<TestCaseEvent> {
        try {
            val testInfos = Collections.synchronizedMap(HashMap<TestIdentifier, TestInfo>())
            val numberOfPools = 1
            val poolCountDownLatch = CountDownLatch(numberOfPools)
            val poolTests = Collections.synchronizedList(ArrayList<Pair<Device, Set<TestIdentifier>>>())
            var concurrentDeviceExecutor: ExecutorService? = null
            val poolName = context.pool.name
            val deviceTestCollectors = Collections.synchronizedList(ArrayList<Pair<Device, TestCollectingListener>>())
            try {
                val devicesInPool = context.pool.size()
                concurrentDeviceExecutor = namedExecutor(devicesInPool, "DeviceExecutor-%d")
                val deviceCountDownLatch = CountDownLatch(devicesInPool)
                logger.info("Pool {} started", poolName)
                val configuration = context.configuration
                val installer = installer(configuration)
                for (device in context.pool.devices) {
                    if (device is AndroidDevice) {
                        val testCollector = TestCollectingListener()
                        deviceTestCollectors.add(Pair(device, testCollector))
                        val deviceTestRunner = Runnable {
                            val deviceInterface = device.deviceInterface
                            try {
                                DdmPreferences.setTimeOut(30000)
                                installer.prepareInstallation(deviceInterface as IDevice)

                                val collectionLatch = CountDownLatch(1)

                                val collectingRule = AndroidCollectingTestCaseRunRule(device, testCollector, collectionLatch)
                                val collectingTestRun = testRunFactory.createCollectingRun(
                                        device, context.pool, testCollector, collectionLatch)
                                try {
                                    collectingRule.before()
                                    collectingTestRun.execute()
                                } finally {
                                    val stubCase = TestCase(
                                            ApkTestCase::class.java,
                                            "dummy", "dummy.Dummy", "dummy",
                                            listOf("dummy"))
                                    val stubResult = TestCaseRunResult(
                                            context.pool, device,
                                            stubCase,
                                            ResultStatus.PASS,
                                            emptyList(),
                                            startTimestampUtc = Instant.now(),
                                            netStartTimestampUtc = Instant.now(),
                                            netEndTimestampUtc = null,
                                            baseTotalFailureCount = 0,
                                            additionalProperties = emptyMap(),
                                            data = emptyList()
                                    )
                                    collectingRule.after(TestCaseRunRuleAfterArguments(stubResult))
                                }

                                collectionLatch.await(configuration.testOutputTimeout + logcatWaiterSleep * 2, TimeUnit.MILLISECONDS)
                                testInfos.putAll(testCollector.infos)
                            } finally {
                                logger.info("Device {} from pool {} finished", device.serial, context.pool.name)
                                deviceCountDownLatch.countDown()
                            }
                        }
                        concurrentDeviceExecutor!!.execute(deviceTestRunner)
                    }
                }
                deviceCountDownLatch.await()
            } catch (e: InterruptedException) {
                logger.warn("Pool {} was interrupted while running", poolName)
            } finally {
                concurrentDeviceExecutor?.shutdown()
                logger.info("Pool {} finished", poolName)
                synchronized(deviceTestCollectors) {
                    synchronized(poolTests) {
                        deviceTestCollectors.forEach { (first, second) -> poolTests.add(Pair(first, second.tests)) }
                    }
                }
                poolCountDownLatch.countDown()
                logger.info("Pools remaining: {}", poolCountDownLatch.count)
            }
            logger.info("Successfully loaded test cases")

            val allTestsSet: HashSet<TestIdentifier>
            val events: Map<TestIdentifier, Collection<Device>>
            synchronized(poolTests) {
                allTestsSet = allTestsFromPoolTests(poolTests)
                events = calculateDeviceIncludes(poolTests)
            }
            return joinTestInfo(allTestsSet, events, testInfos)
        } catch (e: InterruptedException) {
            // TODO: replace with concrete exception
            throw RuntimeException("Reading suites were interrupted")
        }
    }

    internal class TestInfoCatCollector : Collector<LogCatMessage, ArrayList<TestInfoCatCollector.MessageTriple>, List<JsonObject>> {
        override fun supplier(): Supplier<ArrayList<MessageTriple>> {
            return Supplier {
                ArrayList<MessageTriple>()
            }
        }

        override fun accumulator(): BiConsumer<ArrayList<MessageTriple>, LogCatMessage> {
            return BiConsumer { triples, logCatMessage ->
                val parts = logCatMessage.message.split(":".toRegex(), 2).toTypedArray()
                val indexParts = parts[0].split("-".toRegex(), 2).toTypedArray()
                val triple = MessageTriple(indexParts[0], indexParts[1], parts[1])
                triples.add(triple)
            }
        }

        override fun combiner(): BinaryOperator<ArrayList<MessageTriple>> {
            return BinaryOperator { triples1, triples2 ->
                val output = ArrayList<MessageTriple>(triples1.size + triples2.size)
                output.addAll(triples1)
                output.addAll(triples2)
                output
            }
        }

        override fun finisher(): Function<ArrayList<MessageTriple>, List<JsonObject>> {
            return Function { triples ->
                var joined = triples.stream()
                        .sorted()
                        .map { messageTriple -> messageTriple.line }
                        .collect(Collectors.joining())
                if (joined.endsWith(",")) {
                    joined = joined.substring(0, joined.length - 1)
                }
                joined = "[$joined]"

                try {
                    jsonParser.parse(joined).asJsonArray
                            .map { it.asJsonObject }
                            .toList()
                } catch (e: JsonParseException) {
                    throw RuntimeException("Failed to parse: $joined", e)
                }
            }
        }

        override fun characteristics(): Set<Collector.Characteristics> {
            return emptySet()
        }

        companion object {
            private val jsonParser = JsonParser()
        }

        class MessageTriple(private val objectId: String, private val index: String, internal val line: String) : Comparable<MessageTriple> {

            override fun compareTo(other: MessageTriple): Int {
                val result: Int = objectId.compareTo(other.objectId)
                return if (result != 0) result else index.compareTo(other.index)
            }
        }
    }

    companion object {
        const val logcatWaiterSleep: Long = 2500

        fun calculateDeviceIncludes(
                poolTests: List<Pair<Device, Set<TestIdentifier>>>
        ): Map<TestIdentifier, Collection<Device>> {
            return poolTests
                    .flatMap { (device, tests) ->
                        tests.map { it to device }
                    }
                    .groupBy( { (test, _) -> test }, { (_, device) -> device})
        }

        private fun joinTestInfo(
                allTestsSet: Set<TestIdentifier>,
                includes: Map<TestIdentifier, Collection<Device>>,
                infos: Map<TestIdentifier, TestInfo>
        ): Collection<TestCaseEvent> {
            return allTestsSet.map {
                val includedDevices = includes[it] ?: emptyList()
                val info = infos[it] ?: TestInfo(it, guessPackage(it.className), emptyList(), emptyList())
                TestCaseEvent(
                        TestCase(
                            ApkTestCase::class.java,
                            info.`package`,
                            it.className,
                            it.testName,
                            info.readablePath,
                            emptyMap(),
                            info.annotations,
                            Any()
                        ),
                        includedDevices,
                        emptyList()
                )
            }
        }

        private fun toStringList(array: JsonArray): ArrayList<String> {
            val output = ArrayList<String>(array.size())
            array.forEach { jsonElement -> output.add(jsonElement.asString) }
            return output
        }

        fun allTestsFromPoolTests(poolTests: List<Pair<Device, Set<TestIdentifier>>>): HashSet<TestIdentifier> {
            return poolTests
                    .flatMap { (_, tests) -> tests }
                    .toHashSet()
        }
    }
}
