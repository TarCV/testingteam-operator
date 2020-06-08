package com.github.tarcv.tongs.test

import com.github.tarcv.test.Config
import com.github.tarcv.tongs.test.util.ResultsSupplier
import com.github.tarcv.tongs.test.util.attributeNamed
import com.github.tarcv.tongs.test.util.childrenAssertingNoText
import com.github.tarcv.tongs.test.util.isRunStubbed
import org.hamcrest.CoreMatchers.*
import org.hamcrest.Matcher
import org.hamcrest.Matchers.isEmptyString
import org.hamcrest.core.AnyOf.anyOf
import org.junit.Assert
import org.junit.Assume
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import org.w3c.dom.Node
import org.xml.sax.SAXException
import java.io.File
import java.lang.AssertionError
import java.net.URL
import javax.xml.XMLConstants.W3C_XML_SCHEMA_NS_URI
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.transform.stream.StreamSource
import javax.xml.validation.SchemaFactory
import kotlin.test.asserter


@RunWith(Parameterized::class)
class FunctionalXmlTest(private val xmlFile: File) {
    @Test
    fun validateXmlsAgainstUnofficialJUnitSchemas() {
        listOf("JUnitUnofficial.xsd", "JUnitJenkins.xsd").forEach { schema ->
            val schemaUrl = javaClass.classLoader.getResource(schema) as URL
            val factory = SchemaFactory.newInstance(W3C_XML_SCHEMA_NS_URI)
            val validatorFactory = factory.newSchema(schemaUrl)

            val validator = validatorFactory.newValidator()
            val result = xmlFile.toURI().toURL()
            try {
                validator.validate(StreamSource(result.toString()))
            } catch (e: SAXException) {
                throw AssertionError("$xmlFile is not valid according to $schema", e)
            }
        }
    }

    @Test
    fun testHappyXmlsHaveExpectedContent() {
        readSuiteAndCaseInfo().apply {
            Assume.assumeThat("$testClass#$testMethod", happyTestMatcher)

            val ti = this
            asserter.apply {
                commonTestXmlsHaveExpectedContent(ti)

                assertEquals("tests", "1", suite.attributeNamed("tests").nodeValue)
                assertEquals("skipped", "0", suite.attributeNamed("skipped").nodeValue)
                assertEquals("failures", "0", suite.attributeNamed("failures").nodeValue)
                assertEquals("errors", "0", suite.attributeNamed("errors").nodeValue)

                assertTrue("testcase tag has no childs and empty", testCase.childrenAssertingNoText().isEmpty())

                assertTrue("stdout tag has some text", stdOut.textContent.isNotBlank())

                assertEquals("totalFailureCount", "0", properties["totalFailureCount"])

            }
        }
    }

    @Test
    fun testNegativeXmlsHaveExpectedContent() {
        readSuiteAndCaseInfo().apply {
            Assume.assumeThat("$testClass#$testMethod", not(happyTestMatcher))

            val isResultTest = testClass == "com.github.tarcv.test.ResultTest"
            val expectFailure = isResultTest && testMethod.startsWith("failure")
            val expectAfterFailure = isResultTest && testMethod.contains("failAfter = true")
            val expectSkipped = isResultTest && testMethod.startsWith("assumptionFailure")

            val ti = this
            asserter.apply {
                commonTestXmlsHaveExpectedContent(ti)

                assertEquals("tests", "1", suite.attributeNamed("tests").nodeValue)
                assertEquals("skipped", (expectSkipped && !expectAfterFailure).toTestNumber(),
                        suite.attributeNamed("skipped").nodeValue)
                assertEquals("failures", (expectFailure || expectAfterFailure).toTestNumber(),
                        suite.attributeNamed("failures").nodeValue)
                assertEquals("errors", "0", suite.attributeNamed("errors").nodeValue)

                // assertTrue("testcase tag has no childs and empty", testCase.childrenAssertingNoText().isEmpty())
                val testCaseChildren = testCase.childrenAssertingNoText()
                val expectStackTrace: String? = when {
                    expectAfterFailure || expectFailure -> {
                        // JUnit would add 2 failure tags for (expectSkipped && expectAfterFailure),
                        //  but that's hard to support on Android
                        testCaseChildren
                                .single { it.nodeName == "failure" }
                                .nodeValue
                    }
                    !expectAfterFailure && expectSkipped -> {
                        testCaseChildren
                                .single { it.nodeName == "skipped" }
                                .nodeValue
                    }
                    else -> {
                        assertTrue("testcase tag has no childs and empty", testCaseChildren.isEmpty())
                        null
                    }
                }
                if (expectStackTrace != null) {
                    Assert.assertThat("Stacktrace for a failure or skip should not be empty",
                            expectStackTrace, not(isEmptyString()))
                }

                val stdOutText = stdOut.textContent
                assertTrue("stdout tag has some text", stdOutText.isNotBlank())
                if (expectStackTrace != null) {
                    // This is not actually JUnit compliant, but it compensates for non-compliance above:
                    //  this system-out can be used to get all test case failures
                    Assert.assertThat(stdOutText, containsString(expectStackTrace))
                }
                val multipleFailuresMatchers = listOf(
                        containsString("There were 2 failures:"),
                        containsString("2) ")
                )
                multipleFailuresMatchers.forEach {
                    if ((expectFailure || expectSkipped) && expectAfterFailure) {
                        Assert.assertThat(stdOutText, it)
                    } else {
                        Assert.assertThat(stdOutText, not(it))
                    }
                }

                if (expectFailure || expectAfterFailure) {
                    assertTrue("totalFailureCount > 0", properties.getValue("totalFailureCount").toInt() > 0)
                } else {
                    assertEquals("totalFailureCount", "0", properties["totalFailureCount"])
                }
            }
        }
    }

    private fun commonTestXmlsHaveExpectedContent(testCaseInfo: TestCaseInfo) {
        asserter.assertEquals("at localhost", "localhost", testCaseInfo.suite.attributeNamed("hostname").nodeValue)
        asserter.assertTrue("stderr tag has no childs and empty", testCaseInfo.stdErr.childrenAssertingNoText().isEmpty())
        asserter.assertEquals("pool", "emulators", testCaseInfo.properties["pool"])

        val actualDeviceId = testCaseInfo.properties["deviceId"]
        val expectedIdMatcher = if (isRunStubbed) {
            anyOf(`is`("tongs-5554"), `is`("tongs-5556"))
        } else {
            anyOf(`is`(System.getenv("DEVICE1")), `is`(System.getenv("DEVICE2")))
        }
        Assert.assertThat(actualDeviceId, expectedIdMatcher)
    }

    private fun readSuiteAndCaseInfo(): TestCaseInfo {
        val documentBuilderFactory = DocumentBuilderFactory.newInstance()
        val xml = documentBuilderFactory
                .newDocumentBuilder()
                .parse(xmlFile)
        val suite = xml.childrenAssertingNoText().single { it.nodeName == "testsuite" }
        val suiteChildren = suite.childrenAssertingNoText()
        val testCase = suiteChildren.single { it.nodeName == "testcase" }
        val testMethod = testCase.attributeNamed("name").nodeValue as String
        val testClass = testCase.attributeNamed("classname").nodeValue as String
        val stdOut = suiteChildren.single { it.nodeName == "system-out" }
        val stdErr = suiteChildren.single { it.nodeName == "system-err" }
        val properties = suiteChildren
                .single { it.nodeName == "properties" }
                .childrenAssertingNoText()
                .associateBy(
                        { it.attributeNamed("name").nodeValue }, { it.attributeNamed("value").nodeValue }
                )
        return TestCaseInfo(
                suite,
                properties,
                stdOut,
                stdErr,
                testCase,
                testMethod,
                testClass
        )
    }

    companion object {
        val happyTestMatcher: Matcher<String> = anyOf(
                startsWith("${Config.PACKAGE}."),
                allOf(
                        startsWith("com.github.tarcv.test.ResultTest#"),
                        containsString("#successful"),
                        not(containsString("failAfter = true"))
                )
        )

        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun fileProvider(): Iterable<File> {
            return ResultsSupplier.JUnitXmls.rootFile
                    .walkTopDown()
                    .filter { it.isFile && it.extension.toLowerCase() == "xml" }
                    .toList()
        }
    }
}

private data class TestCaseInfo(
        val suite: Node,
        val properties: Map<String, String>,
        val stdOut: Node,
        val stdErr: Node,
        val testCase: Node,
        val testMethod: String,
        val testClass: String
)

private fun Boolean.toTestNumber(): String {
    val intVal = if (this) {
        1
    } else {
        0
    }
    return intVal.toString()
}
