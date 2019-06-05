/*
 * Copyright 2019 TarCV
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
import com.android.build.gradle.BaseExtension
import com.android.build.gradle.LibraryPlugin
import com.android.build.gradle.api.ApkVariant
import com.android.build.gradle.api.BaseVariantOutput
import com.android.build.gradle.api.TestVariant
import com.github.tarcv.tongs.TongsConfigurationGradleExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.JavaBasePlugin

/**
 * Gradle plugin for Tongs.
 */
class TongsPlugin implements Plugin<Project> {

    /** Task name prefix. */
    private static final String TASK_PREFIX = "tongs"

    @Override
    void apply(final Project project) {

        if (!project.plugins.findPlugin(AppPlugin) && !project.plugins.findPlugin(LibraryPlugin)) {
            throw new IllegalStateException("Android plugin is not found")
        }

        project.extensions.add "tongs", TongsConfigurationGradleExtension
        project.dependencies {
            androidTestImplementation "com.github.tarcv.tongs:tongs-ondevice:${BuildConfig.PLUGIN_VERSION}"
        }

        def tongsTask = project.task(TASK_PREFIX) {
            group = JavaBasePlugin.VERIFICATION_GROUP
            description = "Runs all the instrumentation test variations on all the connected devices"
        }

        BaseExtension android = project.android
        android.testVariants.all { TestVariant variant ->
            TongsRunTask tongsTaskForTestVariant = createTask(variant, project)
            tongsTask.dependsOn tongsTaskForTestVariant
        }
    }

    private static TongsRunTask createTask(final TestVariant variant, final Project project) {
        checkTestVariants(variant)

        def tongsTask = project.tasks.create("${TASK_PREFIX}${variant.name.capitalize()}", TongsRunTask)

        def testedVariant = (ApkVariant) variant.testedVariant
        testedVariant.outputs.all { BaseVariantOutput baseVariantOutput ->
            checkTestedVariants(baseVariantOutput)
            tongsTask.configure {
                TongsConfigurationGradleExtension config = project.tongs

                description = "Runs instrumentation tests on all the connected devices for '${variant.name}' variation and generates a report with screenshots"
                group = JavaBasePlugin.VERIFICATION_GROUP

                instrumentationApk = variant.outputs[0].outputFile

                title = config.title
                subtitle = config.subtitle
                testPackage = config.testPackage
                testOutputTimeout = config.testOutputTimeout
                testSize = config.testSize
                excludedSerials = config.excludedSerials
                fallbackToScreenshots = config.fallbackToScreenshots
                totalAllowedRetryQuota = config.totalAllowedRetryQuota
                retryPerTestCaseQuota = config.retryPerTestCaseQuota
                isCoverageEnabled = config.isCoverageEnabled
                poolingStrategy = config.poolingStrategy
                ignoreFailures = config.ignoreFailures
                excludedAnnotation = config.excludedAnnotation
                tongsIntegrationTestRunType = config.tongsIntegrationTestRunType

                applicationApk = baseVariantOutput.outputFile

                String baseOutputDir = config.baseOutputDir
                File outputBase
                if (baseOutputDir) {
                    outputBase = new File(baseOutputDir)
                } else {
                    outputBase = new File(project.buildDir, "reports/tongs")
                }
                output = new File(outputBase, variant.name)

                dependsOn testedVariant.assembleProvider.get(), variant.assembleProvider.get()
            }
            tongsTask.outputs.upToDateWhen { false }
        }
        return tongsTask
    }

    private static checkTestVariants(TestVariant testVariant) {
        if (testVariant.outputs.size() > 1) {
            throw new UnsupportedOperationException("The Tongs plugin does not support abi/density splits for test APKs")
        }
    }

    /**
     * Checks that if the base variant contains more than one outputs (and has therefore splits), it is the universal APK.
     * Otherwise, we can test the single output. This is a workaround until Tongs supports test & app splits properly.
     *
     * @param baseVariant the tested variant
     */
    private static checkTestedVariants(BaseVariantOutput baseVariantOutput) {
        if (baseVariantOutput.outputs.size() > 1) {
            throw new UnsupportedOperationException(
                    "The Tongs plugin does not support abi splits for app APKs, but supports testing via a universal APK. " +
                            "Add the flag \"universalApk true\" in the android.splits.abi configuration."
            )
        }
    }
}
