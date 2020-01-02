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
import com.github.tarcv.tongs.model.AnnotationInfo
import com.github.tarcv.tongs.model.Device
import com.github.tarcv.tongs.model.TestCaseEvent
import com.github.tarcv.tongs.runner.AndroidTestRunFactory
import com.github.tarcv.tongs.runner.IRemoteAndroidTestRunnerFactory
import com.google.gson.*
import org.slf4j.LoggerFactory
import java.lang.IllegalStateException
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

public class JUnitTestSuiteLoader(
        private val context: TestSuiteLoaderContext,
        private val testRunFactory: AndroidTestRunFactory,
        private val remoteAndroidTestRunnerFactory: IRemoteAndroidTestRunnerFactory) : TestSuiteLoader {
    private val logger = LoggerFactory.getLogger(JUnitTestSuiteLoader::class.java)

    @Throws(NoTestCasesFoundException::class)
    override fun loadTestSuite(): Collection<TestCaseEvent> {
        val result = ArrayList(askDevicesForTests())
        logger.debug("Found tests: $result")
        if (result.isEmpty()) {
            throw NoTestCasesFoundException("No tests cases were found")
        }

        return result
    }

    private fun askDevicesForTests(): Collection<TestCaseEvent> {
        try {
            val testInfoMessages = Collections.synchronizedList(ArrayList<Map.Entry<TestIdentifier, JsonObject>>())
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
                    val testCollector = TestCollectingListener()
                    deviceTestCollectors.add(Pair(device, testCollector))
                    val deviceTestRunner = Runnable {
                        val deviceInterface = device.deviceInterface
                        try {
                            DdmPreferences.setTimeOut(30000)
                            installer.prepareInstallation(deviceInterface as IDevice)

                            val collectionLatch = CountDownLatch(1)

                            val collectingTestRun = testRunFactory.createCollectingRun(
                                    device as AndroidDevice, context.pool, testCollector, collectionLatch)
                            collectingTestRun.execute()

                            collectionLatch.await(configuration.testOutputTimeout + logcatWaiterSleep * 2, TimeUnit.MILLISECONDS)
                            testInfoMessages.addAll(testCollector.infos)
                        } finally {
                            logger.info("Device {} from pool {} finished", device.serial, context.pool.name)
                            deviceCountDownLatch.countDown()
                        }
                    }
                    concurrentDeviceExecutor!!.execute(deviceTestRunner)
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
                events = calculateDeviceExcludes(allTestsSet, poolTests)
            }
            val infos = calculateAnnotatedInfo(allTestsSet, testInfoMessages)
            return joinTestInfo(allTestsSet, events, infos)
        } catch (e: InterruptedException) {
            // TODO: replace with concrete exception
            throw RuntimeException("Reading suites were interrupted")
        }
    }

    private class ExtraInfo(
            val properties: Map<String, String>,
            val info: List<AnnotationInfo>
    )

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

        fun calculateDeviceExcludes(
                allTestsSet: Set<TestIdentifier>,
                poolTests: List<Pair<Device, Set<TestIdentifier>>>
        ): Map<TestIdentifier, Collection<Device>> {

            return poolTests
                    .flatMap { (device, tests) ->
                        val excludedTests = (allTestsSet - tests)
                        excludedTests.map { it to device }
                    }
                    .groupBy({ (test, _) -> test }, { (_, device) -> device })
        }

        private fun joinTestInfo(
                allTestsSet: Set<TestIdentifier>,
                excludes: Map<TestIdentifier, Collection<Device>>,
                infos: Map<TestIdentifier, ExtraInfo>
        ): Collection<TestCaseEvent> {
            return allTestsSet.map {
                val excludedDevices = excludes[it] ?: emptyList()
                val info = infos[it] ?: ExtraInfo(emptyMap(), emptyList())
                TestCaseEvent.newTestCase(
                        it.testName,
                        it.className,
                        info.properties,
                        info.info,
                        excludedDevices
                )
            }
        }

        private fun toStringList(array: JsonArray): ArrayList<String> {
            val output = ArrayList<String>(array.size())
            array.forEach { jsonElement -> output.add(jsonElement.asString) }
            return output
        }

        private fun calculateAnnotatedInfo(tests: Set<TestIdentifier>, testInfos: MutableList<Map.Entry<TestIdentifier, JsonObject>>): Map<TestIdentifier, ExtraInfo> {
            val testInfoMap = testInfos.associateBy { it.key }
            return tests
                    .map { testIdentifier ->
                        val info = testInfoMap[testIdentifier]
                        if (info != null) {
                            val properties = HashMap<String, String>()

                            val annotations = info.value.get("annotations")
                            val annotationInfos = if (annotations != null) {
                                val classNameKey = "annotationType"

                                annotations.asJsonArray.map { annotationElement ->
                                    val annotation = annotationElement.asJsonObject
                                    val annotationType = annotation.get(classNameKey).asString
                                    val properties = (convertToJava(annotation) as Map<String, Any?>)
                                            .filter { it.key != classNameKey }
                                    AnnotationInfo(
                                            annotationType,
                                            properties
                                    )
                                }
                            } else {
                                emptyList<AnnotationInfo>()
                            }
                            testIdentifier to ExtraInfo(properties, annotationInfos)
                        } else {
                            testIdentifier to ExtraInfo(emptyMap(), emptyList())
                        }
                    }
                    .toMap()
        }

        private fun convertToJava(value: JsonElement?): Any? {
            return when {
                value == null || value.isJsonNull -> {
                    null
                }
                value.isJsonArray -> {
                    value.asJsonArray
                            .map { convertToJava(it) }
                            .toList()
                }
                value.isJsonObject -> {
                    value.asJsonObject
                            .entrySet()
                            .associateBy({ it.key }, { convertToJava(it.value) })
                }
                value.isJsonPrimitive -> {
                    val primitive = value.asJsonPrimitive
                    return if (primitive.isNumber) {
                        primitive.asNumber
                    } else if (primitive.isString) {
                        primitive.asString
                    } else {
                        throw IllegalStateException("Got unknown type of JSON Element: ${value}")
                    }
                }
                else -> {
                    throw IllegalStateException("Got unknown type of JSON Element: ${value}")
                }
            }
        }

        fun allTestsFromPoolTests(poolTests: List<Pair<Device, Set<TestIdentifier>>>): HashSet<TestIdentifier> {
            return poolTests
                    .flatMap { (_, tests) -> tests }
                    .toHashSet()
        }
    }
}
