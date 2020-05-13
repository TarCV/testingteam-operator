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
package com.github.tarcv.tongs.runner

import com.github.tarcv.tongs.injector.*
import com.github.tarcv.tongs.injector.listeners.TestRunListenersTongsFactoryInjector
import com.github.tarcv.tongs.injector.runner.TestRunFactoryInjector
import com.github.tarcv.tongs.injector.system.FileManagerInjector
import com.github.tarcv.tongs.model.*
import com.github.tarcv.tongs.runner.rules.*
import com.github.tarcv.tongs.summary.ResultStatus
import com.github.tarcv.tongs.system.io.TestCaseFileManager
import com.github.tarcv.tongs.system.io.TestCaseFileManagerImpl
import org.slf4j.LoggerFactory
import java.io.BufferedOutputStream
import java.io.ByteArrayOutputStream
import java.io.PrintStream
import java.lang.System.lineSeparator
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference

class DeviceTestRunner(private val pool: Pool,
                       private val device: Device,
                       private val queueOfTestsInPool: TestCaseEventQueue,
                       private val deviceCountDownLatch: CountDownLatch,
                       private val progressReporter: ProgressReporter,
                       private val ruleManagerFactory: RuleManagerFactory
) : Runnable {
    override fun run() {
        try {
            val rules = ruleManagerFactory.create(DeviceRunRuleFactory::class.java,
                    listOf(AndroidSetupDeviceRuleFactory()),
                    { factory, context: DeviceRunRuleContext -> factory.deviceRules(context) }
            ).createRulesFrom { configuration -> DeviceRunRuleContext(configuration, pool, device) }

            try {
                rules.forEach { it.before() }

                while (true) {
                    val testCaseTask = queueOfTestsInPool.pollForDevice(device, 10)
                    if (testCaseTask != null) {
                        testCaseTask.doWork { testCaseEvent: TestCaseEvent ->
                            val testCaseFileManager: TestCaseFileManager = TestCaseFileManagerImpl(FileManagerInjector.fileManager(), pool, device, testCaseEvent.testCase)
                            val configuration = ConfigurationInjector.configuration()

                            val testRunListeners = TestRunListenersTongsFactoryInjector.testRunListenersTongsFactory(configuration).createTongsListners(
                                    testCaseEvent,
                                    device,
                                    pool,
                                    progressReporter,
                                    queueOfTestsInPool,
                                    configuration.tongsIntegrationTestRunType)
                                    .toList()

                            val ruleManager = ruleManagerFactory.create(
                                    TestCaseRunRuleFactory::class.java,
                                    listOf(
                                            AndroidCleanupTestCaseRunRuleFactory(),
                                            AndroidPermissionGrantingTestCaseRunRuleFactory() // must be executed AFTER the clean rule
                                    ),
                                    { factory, context: TestCaseRunRuleContext -> factory.testCaseRunRules(context) }
                            )
                            val testCaseRunRules = ruleManager.createRulesFrom { pluginConfiguration ->
                                TestCaseRunRuleContext(
                                        pluginConfiguration, testCaseFileManager,
                                        pool, device, testCaseEvent)
                            }

                            val inRuleText = "while executing a test case run rule"

                            val (allowedAfterRules, eitherResult) = withRulesWithoutAfter(
                                    logger,
                                    inRuleText,
                                    "while executing a test case",
                                    (testRunListeners + testCaseRunRules),
                                    { it.before() },
                                    {
                                        val executeContext = TestCaseRunRuleContext(
                                                ActualConfiguration(configuration), testCaseFileManager,
                                                pool, device, testCaseEvent)

                                        executeTestCase(executeContext)
                                                .fixRunResult(testCaseEvent.testCase, "Test case runner")
                                    }
                            )

                            val fixedResult = eitherResult
                                    .getOrElse { e ->
                                        logger.error("Exception while executing a test case", e)
                                        val stackTrace = traceAsString(e)
                                        TestCaseRunResult(
                                                pool, device,
                                                testCaseEvent.testCase, ResultStatus.ERROR,
                                                stackTrace,
                                                0f,
                                                0, emptyMap(),
                                                null,
                                                emptyList())
                                    }

                            allowedAfterRules
                                    .asReversed()
                                    .fold(fixedResult) { acc, rule ->
                                        try {
                                            val args = TestCaseRunRuleAfterArguments(acc)

                                            rule.after(args)
                                            args.result
                                                    .fixRunResult(
                                                            testCaseEvent.testCase,
                                                            "Rule ${rule.javaClass.name}"
                                                    )
                                        } catch (e: Exception) {
                                            val emptyPrefix = if (acc.stackTrace.isBlank()) {
                                                """|
                                                |
                                                |---Rule exceptions---""".trimMargin()
                                            } else {
                                                ""
                                            }
                                            val newStackTrace = """${acc.stackTrace}${emptyPrefix}
                                                |
                                                |Exception ${inRuleText} (after): ${traceAsString(e)}"""
                                                .trimMargin()
                                            acc.copy(
                                                    status = ResultStatus.ERROR,
                                                    stackTrace = newStackTrace
                                            )
                                        }
                                    }
                        }
                    } else if (queueOfTestsInPool.hasNoPotentialEventsFor(device)) {
                        break
                    }
                }
            } finally {
                rules
                        .asReversed()
                        .forEach { it.after() }
            }
        } finally {
            logger.info("Device {} from pool {} finished", device.serial, pool.name)
            deviceCountDownLatch.countDown()
        }
    }

    private fun TestCaseRunResult.fixRunResult(testCase: TestCase, changer: String): TestCaseRunResult {
        if (pool != this@DeviceTestRunner.pool
                || device != this@DeviceTestRunner.device
                || this.testCase != testCase) {
            throw RuntimeException(
                    "$changer attempted to change pool, device or testCase field of a run result")
        }

        return if (status == ResultStatus.UNKNOWN) { // TODO: Report as a fatal crashed test
            copy(
                    pool = this@DeviceTestRunner.pool,
                    device = this@DeviceTestRunner.device,
                    testCase = testCase,
                    status = ResultStatus.ERROR
            )
        } else {
            this
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(DeviceTestRunner::class.java)

        private fun executeTestCase(context: TestCaseRunRuleContext): TestCaseRunResult {
            val androidTestRunFactory = TestRunFactoryInjector.testRunFactory(context.configuration)
            val workCountdownLatch = PreregisteringLatch()
            val testStatus = AtomicReference(ResultStatus.UNKNOWN)

            return try {
                val testRun = androidTestRunFactory.createTestRun(context, context.testCaseEvent,
                        context.device as AndroidDevice,
                        context.pool,
                        testStatus,
                        workCountdownLatch)
                workCountdownLatch.finalizeRegistering()
                testRun.execute()
            } finally {
                workCountdownLatch.await(15, TimeUnit.SECONDS)
            }
        }

        private fun traceAsString(e: Throwable): String {
            val byteStream = ByteArrayOutputStream()
            PrintStream(BufferedOutputStream(byteStream), false, Charsets.UTF_8.name()).use { printStream ->
                e.printStackTrace(printStream)
            }
            return byteStream.toString()
        }
    }

}
