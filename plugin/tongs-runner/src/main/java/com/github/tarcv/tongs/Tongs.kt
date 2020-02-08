/*
 * Copyright 2020 TarCV
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
package com.github.tarcv.tongs

import com.github.tarcv.tongs.injector.ConfigurationInjector
import com.github.tarcv.tongs.injector.TongsRunnerInjector.tongsRunner
import com.github.tarcv.tongs.injector.ruleManagerFactory
import com.github.tarcv.tongs.injector.withRules
import com.github.tarcv.tongs.runner.rules.RunConfiguration
import com.github.tarcv.tongs.runner.rules.RunRule
import com.github.tarcv.tongs.runner.rules.RunRuleContext
import com.github.tarcv.tongs.runner.rules.RunRuleFactory
import com.github.tarcv.tongs.utils.Utils
import org.apache.commons.io.FileUtils
import org.apache.commons.lang3.time.DurationFormatUtils
import org.slf4j.LoggerFactory
import java.io.File

class Tongs(configuration: Configuration) {
    init {
        ConfigurationInjector.setConfiguration(configuration)
    }
    private val tongsRunner: TongsRunner = tongsRunner()

    fun run(): Boolean {
        val startOfTestsMs = System.nanoTime()
        val predefinedRulesFactories = listOf(
                PrepareOutputDirectoryRuleFactory()
        )
        val runRules = ruleManagerFactory
                .create<RunRuleContext, RunRule, RunRuleFactory<RunRule>>(
                        RunRuleFactory::class.java,
                        predefinedRulesFactories
                ) { runRuleFactory: RunRuleFactory<RunRule>, runRuleContext: RunRuleContext ->
                    runRuleFactory.runRules(runRuleContext)
                }
                .createRulesFrom { configuration: RunConfiguration ->
                    RunRuleContext(configuration)
                }

        return try {
            withRules(
                    logger,
                    "while executing a run rule",
                    "while running Tongs",
                    runRules,
                    { it.before() },
                    { it, ret ->
                        it.after()
                        ret
                    }
            ) {
                tongsRunner.run()
            }
        } catch (e: Exception) {
            // the exception is already logged inside withRules call
            false
        } finally {
            val duration = Utils.millisSinceNanoTime(startOfTestsMs)
            logger.info(DurationFormatUtils.formatPeriod(0, duration,
                    "'Total time taken:' H 'hours' m 'minutes' s 'seconds'"))
        }
    }

    class PrepareOutputDirectoryRuleFactory(): RunRuleFactory<PrepareOutputDirectoryRule> {
        override fun runRules(context: RunRuleContext): Array<out PrepareOutputDirectoryRule> {
            return arrayOf(PrepareOutputDirectoryRule(context.configuration.output))
        }
    }

    class PrepareOutputDirectoryRule(private val output: File): RunRule {
        override fun before() {
            FileUtils.deleteDirectory(output)
            //noinspection ResultOfMethodCallIgnored
            output.mkdirs()
        }

        override fun after() {
            // no-op
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(Tongs::class.java)
    }
}