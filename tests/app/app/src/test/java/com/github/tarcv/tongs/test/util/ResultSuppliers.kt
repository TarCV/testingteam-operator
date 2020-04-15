package com.github.tarcv.tongs.test.util

import com.github.tarcv.test.BuildConfig
import org.json.JSONObject
import org.junit.Assert
import org.w3c.dom.Node
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import javax.xml.parsers.DocumentBuilderFactory
import kotlin.test.asserter

enum class ResultsSupplier(val supplierName: String) {
    JsonSummary("JSON Summary")
    {
        override val rootFile: File
            get() {
                val summaryDir = File("build/reports/tongs/${BuildConfig.FLAVOR}DebugAndroidTest/summary")
                val jsons = summaryDir.listFiles { file, s ->
                    summaryDir == file && s.toLowerCase().endsWith(".json")
                }
                assert(jsons != null && jsons.size == 1) { "Exactly one summary json should be created" }
                return jsons[0]
            }

        override fun summarySupplier(): List<PoolResult> {
            fun getTestResults(poolSummary: Map<String, Any>): List<TestResult> {
                return poolSummary["testResults"]
                        .let { it as List<Map<String, Any>> }
                        .map { result ->
                            val testCase = result["testCase"] as Map<String, String>
                            TestResult(
                                    deviceSerial = (result["device"] as Map<String, String>).getValue("serial"),
                                    testClass = testCase.getValue("testClass"),
                                    testMethod = testCase.getValue("testMethod"),
                                    additionalProperties = result.getValue("additionalProperties") as Map<String, String>
                            )
                        }
            }

            val summaryJsonFile = rootFile
            val summaryData = JSONObject(Files.readAllBytes(summaryJsonFile.toPath()).toString(Charsets.UTF_8))

            return summaryData.getJSONArray("poolSummaries")
                    .let { it.toList() as List<Map<String, Any>> }
                    .map { poolSummary ->
                        val deviceResult = DeviceResult(getTestResults(poolSummary))
                        PoolResult(deviceResults = listOf(deviceResult))
                    }
        }
    },

    JUnitXmls("JUnit XML results") {
        private val propertiesDefinedInTongs = arrayOf(
                "pool",
                "device",
                "deviceId",
                "totalFailureCount"
        )
        override val rootFile: File = File("./build/reports/tongs/${BuildConfig.FLAVOR}DebugAndroidTest/tests")

        override fun summarySupplier(): List<PoolResult> = getJUnitXmlResults()

        private fun getJUnitXmlResults(): List<PoolResult> {
            val documentBuilderFactory = DocumentBuilderFactory.newInstance()

            return Files.newDirectoryStream(rootFile.toPath())
                    .filter { it.toFile().isDirectory }
                    .map { poolDir ->
                        val poolNameFromDirectoryName = poolDir.fileName.toString()

                        Files.newDirectoryStream(poolDir)
                                .map { deviceDir ->
                                    val deviceSerialFromDirectoryName = deviceDir.fileName.toString()

                                    Files.newDirectoryStream(deviceDir)
                                            .map { resultFile ->
                                                val xml = documentBuilderFactory
                                                        .newDocumentBuilder()
                                                        .parse(resultFile.toFile())
                                                val suite = xml.childrenAssertingNoText().single { it.nodeName == "testsuite" }
                                                val properties = suite.childrenAssertingNoText().single { it.nodeName == "properties" }
                                                val propertiesNodes = properties.childrenAssertingNoText()

                                                val propertyMap = extractPropertiesAsMap(propertiesNodes, resultFile)
                                                        .filter {
                                                            it.key !in propertiesDefinedInTongs
                                                        }

                                                val poolName = extractProperty(properties, "pool")
                                                val deviceSerial = extractProperty(properties, "deviceId")
                                                asserter.assertEquals("Pool directory name and pool name in an XML file should match ($resultFile)",
                                                        poolNameFromDirectoryName, poolName)
                                                asserter.assertEquals("Device directory name and device serial in an XML file should match ($resultFile)",
                                                        deviceSerialFromDirectoryName, deviceSerial)

                                                val testCase = suite.childrenAssertingNoText().single { it.nodeName == "testcase" }
                                                val testMethod = testCase.attributeNamed("name").nodeValue
                                                val testClass = testCase.attributeNamed("classname").nodeValue

                                                TestResult(
                                                        deviceSerial = deviceSerial,
                                                        testClass = testClass,
                                                        testMethod = testMethod,
                                                        additionalProperties = propertyMap
                                                )
                                            }
                                            .let {
                                                DeviceResult(it)
                                            }
                                }
                                .let {
                                    PoolResult(it)
                                }
                    }
        }

        private fun extractPropertiesAsMap(propertiesNodes: List<Node>, resultFile: Path): Map<String, String> {
            return propertiesNodes
                    .associate {
                        val key = it.attributeNamed("name").nodeValue
                        val value = it.attributeNamed("value").nodeValue
                        key to value
                    }
                    .also {
                        assert(it.size == propertiesNodes.size) {
                            "There are duplicated properties in $resultFile"
                        }
                    }
        }

        private fun extractProperty(properties: Node, name: String): String {
            return properties.childrenAssertingNoText()
                    .single { it.attributeNamed("name").nodeValue == name }
                    .attributeNamed("value").nodeValue
        }
    };

    abstract fun summarySupplier(): List<PoolResult>
    abstract val rootFile: File

    companion object {
        val allSuppliers = values()
    }
}

class PoolResult(val deviceResults: List<DeviceResult>)
class DeviceResult(val testResults: List<TestResult>)
class TestResult(
        val deviceSerial: String,
        val testClass: String,
        val testMethod: String,
        val additionalProperties: Map<String, String>
)

