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
import com.github.tarcv.tongs.injector.TestCaseRuleManager
import com.github.tarcv.tongs.injector.runner.RemoteAndroidTestRunnerFactoryInjector
import com.github.tarcv.tongs.injector.runner.TestRunFactoryInjector
import com.github.tarcv.tongs.api.devices.Pool
import com.github.tarcv.tongs.api.run.TestCaseEvent
import com.github.tarcv.tongs.pooling.NoDevicesForPoolException
import com.github.tarcv.tongs.pooling.NoPoolLoaderConfiguredException
import com.github.tarcv.tongs.pooling.PoolLoader
import com.github.tarcv.tongs.runner.PoolTestRunnerFactory
import com.github.tarcv.tongs.runner.ProgressReporter
import com.github.tarcv.tongs.api.result.TestCaseRunResult
import com.github.tarcv.tongs.api.run.TestCaseRunnerContext
import com.github.tarcv.tongs.api.testcases.TestCaseRuleContext
import com.github.tarcv.tongs.suite.JUnitTestSuiteLoader
import com.github.tarcv.tongs.api.testcases.NoTestCasesFoundException
import com.github.tarcv.tongs.api.testcases.TestSuiteLoaderContext
import com.github.tarcv.tongs.injector.TestCaseRunnerManager
import com.github.tarcv.tongs.summary.SummaryGeneratorHook
import org.slf4j.LoggerFactory
import java.lang.IllegalStateException
import java.util.*
import java.util.concurrent.CountDownLatch
import java.util.concurrent.ExecutorService

class TongsRunner(private val poolLoader: PoolLoader,
                  private val poolTestRunnerFactory: PoolTestRunnerFactory,
                  private val progressReporter: ProgressReporter,
                  private val summaryGeneratorHook: SummaryGeneratorHook,
                  private val testCaseRuleManager: TestCaseRuleManager,
                  private val testCaseRunnerManager: TestCaseRunnerManager
) {
    fun run(): Boolean {
        var poolExecutor: ExecutorService? = null
        return try {
            val pools = poolLoader.loadPools()
            val numberOfPools = pools.size
            val poolCountDownLatch = CountDownLatch(numberOfPools)
            poolExecutor = Utils.namedExecutor(numberOfPools, "PoolExecutor-%d")

            val poolTestCasesMap: Map<Pool, Collection<TestCaseEvent>> = pools
                    .map { pool ->
                        val testCaseRules = testCaseRuleManager
                                .createRulesFrom {
                                    configuration ->
                                    TestCaseRuleContext(configuration, pool)
                                }
                        val testCases = createTestSuiteLoaderForPool(pool)
                                .map { testCaseEvent: TestCaseEvent ->
                                    testCaseRules.fold(testCaseEvent) { acc, rule -> rule.transform(acc) }
                                }
                                .filter { testCaseEvent: TestCaseEvent ->
                                    testCaseRules.all { rule -> rule.filter(testCaseEvent) }
                                }

                        pool.devices.forEach { device ->
                            testCaseRunnerManager
                                    .createRulesFrom {
                                        configuration ->
                                        TestCaseRunnerContext(
                                                configuration,
                                                pool,
                                                device
                                        )
                                    }
                                    .forEach { runner ->
                                        testCases.forEach {
                                            if ((!it.isEnabledOn(device)).not() && runner.supports(device, it.testCase)) {
                                                it.addDeviceRunner(device, runner)
                                            }
                                        }
                                    }
                        }
                        testCases.forEach { testCase ->
                            val hasCompatibleDevice = pool.devices.any {
                                device -> testCase.isEnabledOn(device) && testCase.runnersFor(device).isNotEmpty()
                            }
                            if (!hasCompatibleDevice) {
                                throw IllegalStateException("No runner found for $testCase")
                            }
                        }

                        pool to testCases
                    }
                    .toMap()

            // TODO: check that different sets of test cases in different pools doesn't fail run
            val allResults: List<TestCaseRunResult> = ArrayList()
            summaryGeneratorHook.registerHook(pools, poolTestCasesMap, allResults)

            progressReporter.start()
            for (pool in pools) {
                val poolTestCases = poolTestCasesMap[pool]
                val poolTestRunner = poolTestRunnerFactory.createPoolTestRunner(pool,
                        poolTestCases, allResults,
                        poolCountDownLatch, progressReporter)
                poolExecutor.execute(poolTestRunner)
            }
            poolCountDownLatch.await()
            progressReporter.stop()

            val overallSuccess = summaryGeneratorHook.defineOutcome()
            summaryGeneratorHook.unregisterHook()
            logger.info("Overall success: $overallSuccess")
            overallSuccess
        } catch (e: NoPoolLoaderConfiguredException) {
            logger.error("Configuring devices and pools failed", e)
            false
        } catch (e: NoDevicesForPoolException) {
            logger.error("Configuring devices and pools failed", e)
            false
        } catch (e: NoTestCasesFoundException) {
            logger.error("Error when trying to find test classes", e)
            false
        } catch (e: Exception) {
            logger.error("Error while Tongs was executing", e)
            false
        } finally {
            poolExecutor?.shutdown()
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(TongsRunner::class.java)

        // TODO: move to a separate file
        @Throws(NoTestCasesFoundException::class)
        private fun createTestSuiteLoaderForPool(pool: Pool): Collection<TestCaseEvent> {
            val configuration = ConfigurationInjector.configuration()
            val testSuiteLoaderContext = TestSuiteLoaderContext(configuration, pool)
            return JUnitTestSuiteLoader(testSuiteLoaderContext, TestRunFactoryInjector.testRunFactory(configuration), RemoteAndroidTestRunnerFactoryInjector.remoteAndroidTestRunnerFactory(configuration))
                    .loadTestSuite()
        }
    }

}