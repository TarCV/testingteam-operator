/*
 * Copyright 2022 TarCV
 * Copyright 2014 Shazam Entertainment Limited
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License.
 *
 * You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package com.github.tarcv.tongs.gradle

import com.android.build.gradle.BaseExtension
import com.github.tarcv.tongs.PoolingStrategy
import com.github.tarcv.tongs.TongsConfigurationJsonExtension
import com.github.tarcv.tongs.api.TongsConfiguration
import groovy.json.JsonGenerator
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.tasks.Classpath
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.VerificationTask
import org.gradle.process.ExecOperations
import org.slf4j.LoggerFactory
import java.io.File
import java.nio.file.Paths
import javax.inject.Inject
import kotlin.properties.Delegates

/**
 * Task for using Tongs.
 */
abstract class TongsRunTask: DefaultTask(), VerificationTask {
    companion object {
        /** Logger. */
        private val LOG = LoggerFactory.getLogger(TongsRunTask::class.java)
    }

    /** If true then test failures do not cause a build failure. */
    private var ignoreFailures: Boolean = false
    override fun setIgnoreFailures(ignoreFailures: Boolean) {
        this.ignoreFailures = ignoreFailures
    }

    override fun getIgnoreFailures(): Boolean = this.ignoreFailures

    /** Output directory. */
    @OutputDirectory
    lateinit var output: File

    var applicationPackage: String? = null

    var instrumentationPackage: String? = null

    lateinit var title: String

    lateinit var subtitle: String

    var testPackage: String? = null

    var testRunnerClass: String? = null

    var testRunnerArguments: Map<String, String>? = null
    lateinit var plugins: List<String>

    lateinit var pluginsConfiguration: Map<String, Any>

    var isCoverageEnabled = false

    var testOutputTimeout by Delegates.notNull<Int>()

    lateinit var excludedSerials: Collection<String>

    var fallbackToScreenshots = false

    var totalAllowedRetryQuota by Delegates.notNull<Int>()

    var retryPerTestCaseQuota by Delegates.notNull<Int>()

    lateinit var poolingStrategy: PoolingStrategy

    var excludedAnnotation: String? = null

    lateinit var tongsIntegrationTestRunType: TongsConfiguration.TongsIntegrationTestRunType

    @get:Inject
    protected abstract val execOperations: ExecOperations

    @get:Classpath
    abstract val classPath: ConfigurableFileCollection

    @TaskAction
    fun runTongs() {
        LOG.debug("Output: $output")
        LOG.debug("Ignore failures: $ignoreFailures")

        val configuration = TongsConfigurationJsonExtension()
        configuration.applicationPackage = applicationPackage
        configuration.instrumentationPackage = instrumentationPackage
        configuration.testPackage = testPackage
        configuration.baseOutputDir = output.toString()
        configuration.title = title
        configuration.subtitle = subtitle
        configuration.plugins = plugins
        configuration.configuration = pluginsConfiguration
        configuration.poolingStrategy = poolingStrategy
        configuration.testOutputTimeout = testOutputTimeout
        configuration.excludedSerials = excludedSerials
        configuration.fallbackToScreenshots = fallbackToScreenshots
        configuration.totalAllowedRetryQuota = totalAllowedRetryQuota
        configuration.retryPerTestCaseQuota = retryPerTestCaseQuota
        configuration.isCoverageEnabled = isCoverageEnabled
        configuration.excludedAnnotation = excludedAnnotation
        configuration.tongsIntegrationTestRunType = tongsIntegrationTestRunType
        configuration.android.testRunnerClass = testRunnerClass
        configuration.android.instrumentationArguments = testRunnerArguments

        // TODO: new ServerSocketReceiver() // import log events via socket from the child JVM

        // Can't use Workers API here as the JVM used by Operator should be shutdown right after a test run
        // to guarantee bridges to devices are shutdown cleanly.
        // (And JVMs started with Workers API process isolation mode might live very long after a task is finished)
        val result = execOperations.javaexec { s ->
            s.classpath(classPath)
            s.mainClass.set("com.github.tarcv.tongs.cli.TongsCli")
            s.args(
                    "--sdk", project.extensions.getByType(BaseExtension::class.java).sdkDirectory,
                    "--config", "-"
            )

            s.standardInput = JsonGenerator.Options()
                .build()
                .toJson(configuration)
                .byteInputStream()
            s.errorOutput = System.err // TODO
            s.standardOutput = System.out // TODO
        }

        if (result.exitValue != 0 && !ignoreFailures) {
            throw GradleException("Tests failed! See ${Paths.get(output.absolutePath, "html", "index.html")}")
        }
    }
}
