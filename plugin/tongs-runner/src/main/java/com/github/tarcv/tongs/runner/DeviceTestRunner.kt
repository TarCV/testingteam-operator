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

import com.github.tarcv.tongs.injector.BaseRuleManager
import com.github.tarcv.tongs.injector.ConfigurationInjector
import com.github.tarcv.tongs.injector.ConfigurationInjector.configuration
import com.github.tarcv.tongs.injector.listeners.TestRunListenersTongsFactoryInjector
import com.github.tarcv.tongs.injector.runner.TestRunFactoryInjector
import com.github.tarcv.tongs.injector.system.FileManagerInjector
import com.github.tarcv.tongs.model.*
import com.github.tarcv.tongs.runner.rules.RuleFactory
import com.github.tarcv.tongs.runner.rules.TestCaseRunRule
import com.github.tarcv.tongs.runner.rules.TestCaseRunRuleAfterArguments
import com.github.tarcv.tongs.runner.rules.TestCaseRunRuleContext
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

                        // TODO: Add some defensive code
                        testRunListeners.forEach { baseListener -> baseListener.onTestStarted() }

                        val ruleManager = TestCaseRunRuleManager(
                                configuration().plugins.runRules,
                                listOf(
                                        AndroidCleanupTestCaseRunRuleFactory(),
                                        AndroidPermissionGrantingTestCaseRunRuleFactory() // must be executed AFTER the clean rule
                                )
                        )
                        val testCaseRunRules = ruleManager.createRulesFrom { context }

                        testCaseRunRules.forEach { it.before() }

                        val result = executeTestCase(context)
                                .let {
                                    fixRunResult(it, testCaseEvent.testCase, "Test case runner")
                                }
                                .let {
                                    val transormedArgs =
                                            applyRulesAfters(TestCaseRunRuleAfterArguments(it), testCaseRunRules)
                                    transormedArgs.result
                                }

                        testRunListeners.forEach { baseListener ->
                            val status = result.status
                            if (status == ResultStatus.PASS) {
                                baseListener.onTestSuccessful()
                            } else if (status == ResultStatus.IGNORED) {
                                baseListener.onTestSkipped(result)
                            } else if (status == ResultStatus.ASSUMPTION_FAILED) {
                                baseListener.onTestAssumptionFailure(result)
                            } else if (status == ResultStatus.FAIL || status == ResultStatus.ERROR) {
                                baseListener.onTestFailed(result)
                            } else {
                                throw IllegalStateException("Got unknown status:$status")
                            }
                        }
                        result
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
        var fixedStatus = result.status

        if (result.pool != pool
                || result.device != device
                || result.testCase != testCase) {
            throw RuntimeException(
                    "$changer attempted to change pool, device or testCase field of a run result")
        }

        if (fixedStatus == ResultStatus.UNKNOWN) { // TODO: Report as a fatal crashed test
            fixedStatus = ResultStatus.ERROR
        }

        val fixedResult = result.copy(pool, device, testCase, fixedStatus)
        return fixedResult
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

class TestCaseRunRuleManager(ruleClassNames: Collection<String>, predefinedFactories: Collection<RuleFactory<TestCaseRunRuleContext, TestCaseRunRule>>)
    : BaseRuleManager<TestCaseRunRuleContext, TestCaseRunRule, RuleFactory<TestCaseRunRuleContext, TestCaseRunRule>>(
        ruleClassNames,
        predefinedFactories
)