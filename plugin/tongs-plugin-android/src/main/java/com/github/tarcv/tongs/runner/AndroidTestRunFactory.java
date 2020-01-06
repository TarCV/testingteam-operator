/*
 * Copyright 2020 TarCV
 * Copyright 2015 Shazam Entertainment Limited
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License.
 *
 * You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */

package com.github.tarcv.tongs.runner;

import com.github.tarcv.tongs.TongsConfiguration;
import com.github.tarcv.tongs.injector.runner.RemoteAndroidTestRunnerFactoryInjector;
import com.github.tarcv.tongs.model.AndroidDevice;
import com.github.tarcv.tongs.model.Pool;
import com.github.tarcv.tongs.model.TestCaseEvent;
import com.github.tarcv.tongs.runner.listeners.BaseListener;
import com.github.tarcv.tongs.runner.listeners.ResultProducer;
import com.github.tarcv.tongs.runner.listeners.TestCollectorResultProducer;
import com.github.tarcv.tongs.runner.rules.TestCaseRunRule;
import com.github.tarcv.tongs.runner.rules.TestCaseRunRuleContext;
import com.github.tarcv.tongs.runner.rules.TestCaseRunRuleFactory;
import com.github.tarcv.tongs.suite.TestCollectingListener;
import com.github.tarcv.tongs.summary.ResultStatus;
import com.github.tarcv.tongs.system.PermissionGrantingManager;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

public class AndroidTestRunFactory {

    private final TongsConfiguration configuration;

    public AndroidTestRunFactory(TongsConfiguration configuration) {
        this.configuration = configuration;
    }

    public AndroidInstrumentedTestRun createTestRun(TestCaseRunRuleContext testRunContext, TestCaseEvent testCase,
                                                    AndroidDevice device,
                                                    Pool pool,
                                                    AtomicReference<ResultStatus> testStatus,
                                                    PreregisteringLatch workCountdownLatch) {
        TestRunParameters testRunParameters = createTestParameters(testCase, device, configuration);

        List<BaseListener> testRunListeners = new ArrayList<>();
        ResultProducer resultProducer = new ResultProducer(testRunContext, workCountdownLatch);
        testRunListeners.addAll(resultProducer.requestListeners());

        return new AndroidInstrumentedTestRun(
                pool.getName(),
                testRunParameters,
                testRunListeners,
                resultProducer,
                RemoteAndroidTestRunnerFactoryInjector.remoteAndroidTestRunnerFactory(configuration)
        );
    }

    public AndroidInstrumentedTestRun createCollectingRun(AndroidDevice device,
                                                          Pool pool,
                                                          TestCollectingListener testCollectingListener,
                                                          CountDownLatch latch) {
        TestRunParameters testRunParameters = createTestParameters(null, device, configuration);

        List<BaseListener> testRunListeners = new ArrayList<>();
        testRunListeners.add(testCollectingListener);

        return new AndroidInstrumentedTestRun(
                pool.getName(),
                testRunParameters,
                testRunListeners,
                new TestCollectorResultProducer(pool, device),
                RemoteAndroidTestRunnerFactoryInjector.remoteAndroidTestRunnerFactory(configuration)
        );
    }

    private static TestRunParameters createTestParameters(TestCaseEvent testCase, AndroidDevice device, TongsConfiguration configuration) {
        return TestRunParameters.Builder.testRunParameters()
                .withDeviceInterface(device.getDeviceInterface())
                .withTest(testCase)
                .withTestPackage(configuration.getInstrumentationPackage())
                .withApplicationPackage(configuration.getApplicationPackage())
                .withTestRunner(configuration.getTestRunnerClass())
                .withTestRunnerArguments(configuration.getTestRunnerArguments())
                .withTestOutputTimeout((int) configuration.getTestOutputTimeout())
                .withCoverageEnabled(configuration.isCoverageEnabled())
                .withExcludedAnnotation(configuration.getExcludedAnnotation())
                .build();
    }

}
