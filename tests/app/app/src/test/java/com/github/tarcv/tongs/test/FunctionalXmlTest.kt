package com.github.tarcv.tongs.test

import com.github.tarcv.tongs.test.util.ResultsSupplier
import org.junit.Test
import org.xml.sax.SAXException
import java.lang.AssertionError
import java.net.URL
import javax.xml.XMLConstants.W3C_XML_SCHEMA_NS_URI
import javax.xml.transform.stream.StreamSource
import javax.xml.validation.SchemaFactory


class FunctionalXmlTest {
    @Test
    fun validateXmlsAgainstUnofficialJUnitSchemas() {
        val schemas = listOf("JUnitUnofficial.xsd", "JUnitJenkins.xsd")
        schemas.forEach {schema ->
            val schemaUrl = javaClass.classLoader.getResource(schema) as URL
            val factory = SchemaFactory.newInstance(W3C_XML_SCHEMA_NS_URI)
            val validatorFactory = factory.newSchema(schemaUrl)
            ResultsSupplier.JUnitXmls.rootFile
                    .walkTopDown()
                    .filter { it.isFile && it.extension.toLowerCase() == "xml" }
                    .forEach {
                        val validator = validatorFactory.newValidator()
                        val result = it.toURL()
                        try {
                            validator.validate(StreamSource(result.toString()))
                        } catch (e: SAXException) {
                            throw AssertionError("$it is not valid according to $schema", e)
                        }
                    }
        }
    }
}