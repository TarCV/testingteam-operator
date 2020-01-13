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

import com.github.tarcv.tongs.injector.RuleManager
import com.github.tarcv.tongs.injector.ConfigurationInjector
import com.github.tarcv.tongs.injector.ConfigurationInjector.configuration
import com.github.tarcv.tongs.injector.listeners.TestRunListenersTongsFactoryInjector
import com.github.tarcv.tongs.injector.runner.TestRunFactoryInjector
import com.github.tarcv.tongs.injector.system.FileManagerInjector
import com.github.tarcv.tongs.model.*
import com.github.tarcv.tongs.runner.rules.TestCaseRunRule
import com.github.tarcv.tongs.runner.rules.TestCaseRunRuleAfterArguments
import com.github.tarcv.tongs.runner.rules.TestCaseRunRuleContext
import com.github.tarcv.tongs.runner.rules.TestCaseRunRuleFactory
import com.github.tarcv.tongs.summary.ResultStatus
import com.github.tarcv.tongs.system.io.TestCaseFileManager
import com.github.tarcv.tongs.system.io.TestCaseFileManagerImpl
import org.slf4j.LoggerFactory
import java.io.BufferedOutputStream
import java.io.ByteArrayOutputStream
import java.io.PrintStream
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference

class DeviceTestRunner(private val pool: Pool,
                       private val device: Device,
                       private val queueOfTestsInPool: TestCaseEventQueue,
                       private val deviceCountDownLatch: CountDownLatch,
                       private val progressReporter: ProgressReporter) : Runnable {
    override fun run() {
        try {
            // TODO: call DeviceRule incl. AndroidSetupDeviceRule

            while (true) {
                val testCaseTask = queueOfTestsInPool.pollForDevice(device, 10)
                if (testCaseTask != null) {
                    testCaseTask.doWork { testCaseEvent: TestCaseEvent ->
                        val testCaseFileManager: TestCaseFileManager = TestCaseFileManagerImpl(FileManagerInjector.fileManager(), pool, device, testCaseEvent.testCase)
                        val configuration = ConfigurationInjector.configuration()
                        val context = TestCaseRunRuleContext(
                                configuration, testCaseFileManager,
                                pool, device, testCaseEvent)

                        val testRunListeners = TestRunListenersTongsFactoryInjector.testRunListenersTongsFactory(configuration).createTongsListners(
                                testCaseEvent,
                                device,
                                pool,
                                progressReporter,
                                queueOfTestsInPool,
                                configuration.tongsIntegrationTestRunType)
                                .toList()

                        val ruleManager = RuleManager(
                                TestCaseRunRuleFactory::class.java,
                                listOf(
                                        AndroidCleanupTestCaseRunRuleFactory(),
                                        AndroidPermissionGrantingTestCaseRunRuleFactory() // must be executed AFTER the clean rule
                                ),
                                configuration().pluginsInstances,
                                { factory, context: TestCaseRunRuleContext -> factory.testCaseRunRules(context) }
                        )
                        val testCaseRunRules = ruleManager.createRulesFrom { context }

                        // TODO: Add some defensive code
                        testRunListeners.forEach { it.before() }

                        testCaseRunRules.forEach { it.before() }

                        val initialResult = executeTestCase(context)

                        return@doWork fixRunResult(initialResult, testCaseEvent.testCase, "Test case runner")
                                .let {
                                    val transormedArgs =
                                            applyRulesAfters(TestCaseRunRuleAfterArguments(it), testCaseRunRules)
                                    transormedArgs.result
                                }
                                .let {
                                    val transormedArgs =
                                            applyRulesAfters(TestCaseRunRuleAfterArguments(it), testRunListeners)
                                    transormedArgs.result
                                }
                    }
                } else if (queueOfTestsInPool.hasNoPotentialEventsFor(device)) {
                    break
                }
            }
        } finally {
            logger.info("Device {} from pool {} finished", device.serial, pool.name)
            deviceCountDownLatch.countDown()
        }
    }

    private fun applyRulesAfters(arguments: TestCaseRunRuleAfterArguments, rules: List<TestCaseRunRule>): TestCaseRunRuleAfterArguments {
        val originalResult = arguments.result

        val finalArgs = rules
                .asReversed()
                .fold(arguments) { args, rule ->
                    rule.after(args)

                    args.result = fixRunResult(args.result, originalResult.testCase, "Rule ${rule.javaClass.name}")

                    args
                }
        return finalArgs
    }

    private fun fixRunResult(result: TestCaseRunResult, testCase: TestCase, changer: String): TestCaseRunResult {
        if (result.pool != pool
                || result.device != device
                || result.testCase != testCase) {
            throw RuntimeException(
                    "$changer attempted to change pool, device or testCase field of a run result")
        }

        return if (result.status == ResultStatus.UNKNOWN) { // TODO: Report as a fatal crashed test
            result.copy(pool, device, testCase, status = ResultStatus.ERROR)
        } else {
            result
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(DeviceTestRunner::class.java)

        private fun executeTestCase(context: TestCaseRunRuleContext): TestCaseRunResult {
            val androidTestRunFactory = TestRunFactoryInjector.testRunFactory(context.configuration)
            val workCountdownLatch = PreregisteringLatch()
            return try {
                val testStatus = AtomicReference(ResultStatus.UNKNOWN)

                try {
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
            } catch (e: Throwable) {
                logger.error("Exception during test case execution", e)
                val stackTrace = traceAsStream(e)
                TestCaseRunResult(
                        context.pool, context.device,
                        context.testCaseEvent.testCase, ResultStatus.ERROR,
                        stackTrace,
                        0f,
                        0, emptyMap(),
                        null,
                        emptyList())
            }
        }

        private fun traceAsStream(e: Throwable): String {
            val byteStream = ByteArrayOutputStream()
            val printStream = PrintStream(BufferedOutputStream(byteStream))
            e.printStackTrace(printStream)
            return byteStream.toString()
        }
    }

}
