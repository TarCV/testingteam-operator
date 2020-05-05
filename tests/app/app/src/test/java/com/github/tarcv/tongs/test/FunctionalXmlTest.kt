package com.github.tarcv.tongs.test

import com.github.tarcv.test.Config
import com.github.tarcv.tongs.test.util.ResultsSupplier
import com.github.tarcv.tongs.test.util.attributeNamed
import com.github.tarcv.tongs.test.util.childrenAssertingNoText
import org.hamcrest.CoreMatchers.*
import org.hamcrest.core.AnyOf.anyOf
import org.junit.Assert
import org.junit.Assume
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
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
        val documentBuilderFactory = DocumentBuilderFactory.newInstance()
        val xml = documentBuilderFactory
                .newDocumentBuilder()
                .parse(xmlFile)
        val suite = xml.childrenAssertingNoText().single { it.nodeName == "testsuite" }
        val suiteChildren = suite.childrenAssertingNoText()
        val testCase = suiteChildren.single { it.nodeName == "testcase" }
        val testMethod = testCase.attributeNamed("name").nodeValue
        val testClass = testCase.attributeNamed("classname").nodeValue
        val testIdentifier = "$testClass#$testMethod"
        Assume.assumeThat(testIdentifier, anyOf(
                startsWith("${Config.PACKAGE}."),
                allOf(
                        startsWith("com.github.tarcv.test.ResultTest#"),
                        containsString("#successful"),
                        not(containsString("failAfter = true"))
                )
        ))

        // TODO: test properties
        asserter.apply {
            assertEquals("at localhost", "localhost", suite.attributeNamed("hostname").nodeValue)

            assertEquals("tests", "1", suite.attributeNamed("tests").nodeValue)
            assertEquals("skipped", "0", suite.attributeNamed("skipped").nodeValue)
            assertEquals("failures", "0", suite.attributeNamed("failures").nodeValue)
            assertEquals("errors", "0", suite.attributeNamed("errors").nodeValue)

            assertTrue("testcase tag has no childs and empty", testCase.childrenAssertingNoText().isEmpty())

            val stdOut = suiteChildren.single { it.nodeName == "system-out" }
            assertTrue("stdout tag has some text", stdOut.textContent.isNotBlank())
            val stdErr = suiteChildren.single { it.nodeName == "system-err" }
            assertTrue("stderr tag has no childs and empty", stdErr.childrenAssertingNoText().isEmpty())

            val properties = suiteChildren
                    .single { it.nodeName == "properties" }
                    .childrenAssertingNoText()
                    .associateBy(
                            { it.attributeNamed("name").nodeValue }, { it.attributeNamed("value").nodeValue}
                    )
            assertEquals("pool", "emulators", properties["pool"])
            assertEquals("totalFailureCount", "0", properties["totalFailureCount"])

            Assert.assertThat(properties["deviceId"], anyOf(
                    `is`(System.getenv("DEVICE1")), `is`(System.getenv("DEVICE2"))
            ))
        }
    }

    companion object {
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