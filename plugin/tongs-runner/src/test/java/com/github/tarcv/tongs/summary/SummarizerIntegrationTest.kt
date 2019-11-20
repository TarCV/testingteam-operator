package com.github.tarcv.tongs.summary

import com.github.tarcv.tongs.Configuration
import com.github.tarcv.tongs.injector.ConfigurationInjector.configuration
import com.github.tarcv.tongs.injector.ConfigurationInjector.setConfiguration
import com.github.tarcv.tongs.injector.summary.OutcomeAggregatorInjector.outcomeAggregator
import com.github.tarcv.tongs.injector.summary.SummaryCompilerInjector.summaryCompiler
import com.github.tarcv.tongs.injector.summary.SummaryPrinterInjector.summaryPrinter
import com.github.tarcv.tongs.model.AndroidDevice
import com.github.tarcv.tongs.model.Device
import com.github.tarcv.tongs.runner.*
import com.github.tarcv.tongs.system.io.*
import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.google.gson.JsonParseException
import org.hamcrest.CoreMatchers.startsWith
import org.junit.Assert
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.lang.reflect.Type
import java.nio.file.Paths

class SummarizerIntegrationTest {
    @get:Rule
    val temporaryFolder = TemporaryFolder()

    val nullFile = File(".")

    @Test
    fun summarize() {
        initConfiguration()

        val gson = Summarizer.testRecorderGsonBuilder()
                .registerTypeAdapter(Device::class.java, ForceClassDeserializer(AndroidDevice::class.java))
                .registerTypeAdapter(TestReportData::class.java, TestReportDataDeserializer())
                .registerTypeAdapter(TestCaseFileManager::class.java, ForceClassDeserializer(TestCaseFileManagerImpl::class.java))
                .registerTypeAdapter(FileManager::class.java, ForceClassDeserializer(TongsFileManager::class.java))
                .registerTypeAdapter(FileType::class.java, ComplexEnumDeserializer(StandardFileTypes.values()))
                .create()

        val summarizer = Summarizer(configuration(), summaryCompiler(), summaryPrinter(), outcomeAggregator())
        SummarizerIntegrationTest::class.java.getResourceAsStream("/summarizerIntegration/summarizeInputs.json").bufferedReader().use {
            summarizer.summarizeFromRecordedJson(it, gson)
        }

        checkGeneratedFileTree()
    }

    private fun checkGeneratedFileTree() {
        val actualFilePaths = subDirectoriesPaths(temporaryFolder.root).sorted()

        val resourceRoot = "/summarizerIntegration/expected"
        val commonResourcePathPrefix = "$resourceRoot/"
        val expectedFilePaths = subResourcesPaths(resourceRoot, "html")
                .map {
                    Assert.assertThat(it, startsWith(commonResourcePathPrefix))
                    it.removePrefix(commonResourcePathPrefix)
                }
                .sorted()
        Assert.assertEquals("All expected HTML report files are created", expectedFilePaths, actualFilePaths)
        checkFileContents(actualFilePaths, resourceRoot, temporaryFolder.root)
    }

    private fun initConfiguration() {
        val configuration = Configuration.Builder()
                .withAndroidSdk(nullFile)
                .withApplicationPackage("com.github.tarcv.tongstestapp.f2")
                .withInstrumentationPackage("com.github.tarcv.tongstestapp.test")
                .withTestRunnerClass("android.support.test.runner.AndroidJUnitRunner")
                .withTestRunnerArguments(mapOf(
                        "test_argument" to "default",
                        "test_argument" to "args\"ForF2",
                        "filter" to "com.github.tarcv.test.F2Filter"
                ))
                .withOutput(temporaryFolder.root)
                .build()
        setConfiguration(configuration)
    }

    internal class ComplexEnumDeserializer<T: Enum<*>>(val constants: Array<T>) : JsonDeserializer<T> {
        override fun deserialize(json: JsonElement, typeOfT: Type, context: JsonDeserializationContext): T {
            val enumName = json.asString
            return constants.single { it.name == enumName }
        }
    }

    internal class TestReportDataDeserializer : JsonDeserializer<TestReportData> {
        override fun deserialize(json: JsonElement, typeOfT: Type, context: JsonDeserializationContext): TestReportData {
            val obj = json.asJsonObject
            val title = obj.get("title").asString
            return when {
                obj.has("html") -> HtmlReportData(title, obj.get("html").asString)
                obj.has("table") -> TableReportData(title, context.deserialize(obj.get("table"), Table::class.java))
                obj.has("image") -> ImageReportData(title, context.deserialize(obj.get("image"), TestCaseFile::class.java))
                obj.has("video") -> VideoReportData(title, context.deserialize(obj.get("video"), TestCaseFile::class.java))
                obj.has("file") -> LinkedFileReportData(title, context.deserialize(obj.get("file"), TestCaseFile::class.java))
                else -> throw IllegalStateException("Unknown TestReportData class")
            }
        }

    }

    internal class ForceClassDeserializer<T>(val forcedClass: Class<T>) : JsonDeserializer<T> {
        @Throws(JsonParseException::class)
        override fun deserialize(element: JsonElement, typeOfT: Type, context: JsonDeserializationContext): T {
            return context.deserialize(element, forcedClass)
        }
    }

    companion object {
        private fun checkFileContents(relativeFilePaths: List<String>, expectedResourcesRoot: String, actualFilesRoot: File) {
            relativeFilePaths
                    .filter { it.endsWith("/").not() }
                    .map {
                        val aFile = Paths.get(actualFilesRoot.absolutePath, *it.split("/").toTypedArray())
                                .toFile()
                        val aResource = "$expectedResourcesRoot/$it"
                        aFile to aResource
                    }
                    .forEach { (aFile, aResource) ->
                        val actualBody = aFile.bufferedReader().readLines()
                        val expectedBody = Companion::class.java.getResourceAsStream(aResource).bufferedReader().readLines()
                        Assert.assertEquals("${aFile.path} has expected contents", expectedBody, actualBody)
                    }
        }

        private fun subDirectoriesPaths(base: File): List<String> {
            return base
                    .walkTopDown().toSortedSet()
                    .filter { it.relativeTo(base).invariantSeparatorsPath.startsWith("html") }
                    .map {
                        val relativePath = it.relativeTo(base).invariantSeparatorsPath
                        if (it.isDirectory) {
                            "$relativePath/"
                        } else {
                            relativePath
                        }
                    }
        }

        private fun subResourcesPaths(parentPath: String, itemName: String): List<String> {
            val basePath = "$parentPath/$itemName"
            val firstFile = Companion::class.java.getResourceAsStream("$basePath/")
                    .bufferedReader()
                    .readLine()
            val firstFileStream = Companion::class.java.getResourceAsStream("$basePath/$firstFile")
            if (firstFileStream != null) {
                firstFileStream.close()

                val output = ArrayList<String>().apply {
                    add("$basePath/")
                }
                Companion::class.java.getResourceAsStream("$basePath/")
                        .bufferedReader()
                        .lineSequence()
                        .flatMap {
                            subResourcesPaths(basePath, it).asSequence()
                        }
                        .toCollection(output)
                return output
            } else {
                return listOf(basePath)
            }
        }
    }
}
