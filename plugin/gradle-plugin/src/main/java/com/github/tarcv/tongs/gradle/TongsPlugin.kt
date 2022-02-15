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

import com.android.build.gradle.AppPlugin
import com.android.build.gradle.LibraryPlugin
import com.android.build.gradle.TestedExtension
import com.android.build.gradle.api.InstallableVariant
import com.android.build.gradle.api.TestVariant
import com.github.tarcv.tongs.TongsConfigurationGradleExtension
import com.github.tarcv.tongs.api.TongsConfiguration.TongsIntegrationTestRunType.STUB_PARALLEL_TESTRUN
import groovy.lang.Closure
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.plugins.JavaBasePlugin
import org.gradle.api.tasks.TaskProvider
import java.io.File

/**
 * Gradle plugin for Tongs.
 */
class TongsPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        if (project.plugins.findPlugin(AppPlugin::class.java) == null && project.plugins.findPlugin(LibraryPlugin::class.java) == null) {
            throw IllegalStateException("Android plugin is not found")
        }

        project.extensions.add("tongs", TongsConfigurationGradleExtension::class.java)

        val configuration = project.configurations.create("testingOperator") { c ->
            c.description = "TestingOperator and its plugins dependencies"
            c.isVisible = false
            c.isCanBeConsumed = false
            c.isCanBeResolved = true

            c.withDependencies { dependencies ->
                dependencies.add(
                    project.dependencies.create(
                        "com.github.TarCV.testingteam-operator:cli:${BuildConfig.PLUGIN_VERSION}"
                    )
                )
            }
        }

        project.configurations.all {
            it.resolutionStrategy.eachDependency { details ->
                if (details.requested.version.isNullOrEmpty() &&
                    // TODO: get module name from root gradle.properties
                    "com.github.TarCV.testingteam-operator:plugin-android-ondevice"
                        .equals("${details.requested.group}:${details.requested.name}", ignoreCase = true)
                ) {
                    details.useVersion(BuildConfig.PLUGIN_VERSION)
                    details.because("Default version provided by Testing Team Operator plugin")
                }
            }
        }

        val tongsTask = project.task(TASK_PREFIX) {
            it.group = JavaBasePlugin.VERIFICATION_GROUP
            it.description = "Runs all the instrumentation test variations on all the connected devices"
        }

        val android = project.extensions.findByType(TestedExtension::class.java)
        android?.let {
            it.testVariants.all { variant: TestVariant ->
                val tongsTaskForTestVariant: TaskProvider<TongsRunTask> = registerTask(variant, project, configuration)
                tongsTask.dependsOn(tongsTaskForTestVariant)
            }
        }
    }
}
/** Task name prefix. */
private const val TASK_PREFIX = "tongs"

private fun registerTask(variant: TestVariant, project: Project, configuration: Configuration): TaskProvider<TongsRunTask> {
        return project.tasks.register("${TASK_PREFIX}${variant.name.capitalize()}", TongsRunTask::class.java) { task ->
            val testedVariant = variant.testedVariant
            task.configure(object : Closure<TongsRunTask>(Any()) {
                fun doCall(it: TongsRunTask) {
                    it.classPath.from(configuration)
                    val config = project.extensions.getByType(TongsConfigurationGradleExtension::class.java)

                    it.description =
                        "Runs instrumentation tests on all the connected devices for '${variant.name}' variation and generates a report with screenshots"
                    it.group = JavaBasePlugin.VERIFICATION_GROUP

                    it.title = config.title
                    it.subtitle = config.subtitle
                    it.applicationPackage = testedVariant.applicationId
                    it.instrumentationPackage = variant.applicationId
                    it.testPackage = config.testPackage
                    it.testOutputTimeout = config.testOutputTimeout
                    it.testRunnerClass = variant.mergedFlavor.testInstrumentationRunner
                    it.testRunnerArguments = variant.mergedFlavor.testInstrumentationRunnerArguments
                    it.plugins = config.plugins
                    it.pluginsConfiguration = config.configuration
                    it.excludedSerials = config.excludedSerials
                    it.fallbackToScreenshots = config.fallbackToScreenshots
                    it.totalAllowedRetryQuota = config.totalAllowedRetryQuota
                    it.retryPerTestCaseQuota = config.retryPerTestCaseQuota
                    it.isCoverageEnabled = config.isCoverageEnabled
                    it.poolingStrategy = config.poolingStrategy
                    it.ignoreFailures = config.ignoreFailures
                    it.excludedAnnotation = config.excludedAnnotation
                    it.tongsIntegrationTestRunType = config.tongsIntegrationTestRunType

                    val baseOutputDir = config.baseOutputDir
                    val outputBase: File = if (baseOutputDir.isNullOrEmpty()) {
                        File (project.buildDir, "reports/tongs")
                    } else {
                        File (baseOutputDir)
                    }
                    it.output = File (outputBase, variant.name)

                    if (config.tongsIntegrationTestRunType != STUB_PARALLEL_TESTRUN) {
                        it.dependsOn(
                            (testedVariant as InstallableVariant).installProvider,
                            (variant as InstallableVariant).installProvider
                        )
                    }
                }
            })
            task.outputs.upToDateWhen { false }
        }
}

    private fun checkTestVariants(testVariant: TestVariant) {
        if (testVariant.outputs.size > 1) {
            throw UnsupportedOperationException("The Tongs plugin does not support abi/density splits for test APKs")
        }
    }
