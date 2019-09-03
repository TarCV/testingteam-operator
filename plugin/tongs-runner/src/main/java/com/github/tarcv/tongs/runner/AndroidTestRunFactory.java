/*
 * Copyright 2019 TarCV
 * Copyright 2015 Shazam Entertainment Limited
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License.
 *
 * You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */

package com.github.tarcv.tongs.runner;

import com.github.tarcv.tongs.Configuration;
import com.github.tarcv.tongs.TongsConfiguration;
import com.github.tarcv.tongs.model.*;
import com.github.tarcv.tongs.runner.listeners.BaseListener;
import com.github.tarcv.tongs.runner.listeners.RecordingTestRunListener;
import com.github.tarcv.tongs.runner.listeners.TestRunListenersFactory;
import com.github.tarcv.tongs.suite.TestCollectingListener;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.stream.Collectors;

import static com.github.tarcv.tongs.injector.runner.RemoteAndroidTestRunnerFactoryInjector.remoteAndroidTestRunnerFactory;
import static com.github.tarcv.tongs.runner.TestRunParameters.Builder.testRunParameters;
import static com.github.tarcv.tongs.system.PermissionGrantingManager.permissionGrantingManager;

public class AndroidTestRunFactory {

    private final Configuration configuration;
    private final TestRunListenersFactory testRunListenersFactory;
    public AndroidTestRunFactory(Configuration configuration, TestRunListenersFactory testRunListenersFactory) {
        this.configuration = configuration;
        this.testRunListenersFactory = testRunListenersFactory;
    }

    public AndroidInstrumentedTestRun createTestRun(TestCaseEvent testCase,
                                                    AndroidDevice device,
                                                    Pool pool,
                                                    ProgressReporter progressReporter,
                                                    TestCaseEventQueue queueOfTestsInPool,
                                                    PreregisteringLatch workCountdownLatch) {
        TestRunParameters testRunParameters = createTestParameters(testCase, device, configuration);

        List<BaseListener> testRunListeners = new ArrayList<>();
        testRunListeners.addAll(testRunListenersFactory.createTongsListners(
                testCase,
                device,
                pool,
                progressReporter,
                queueOfTestsInPool,
                workCountdownLatch,
                configuration.getTongsIntegrationTestRunType()));
        testRunListeners.addAll(testRunListenersFactory.createAndroidListeners(
                testCase,
                device,
                pool,
                workCountdownLatch
        ));

        TongsTestCaseContext testRunContext = new TongsTestCaseContext(
                configuration,
                pool,
                device,
                testCase
        );

        List<TestRuleFactory> testRuleFactories = Collections.singletonList(new AndroidCleanupTestRuleFactory());
        List<TestRule> testRules = testRuleFactories.stream()
                .map(factory -> factory.create(testRunContext))
                .collect(Collectors.toList());

        return new AndroidInstrumentedTestRun(
                pool.getName(),
                testRunParameters,
                testRunListeners,
                testRules,
                permissionGrantingManager(),
                remoteAndroidTestRunnerFactory()
        );
    }

    public AndroidInstrumentedTestRun createCollectingRun(AndroidDevice device,
                                                          Pool pool,
                                                          TestCollectingListener testCollectingListener,
                                                          CountDownLatch latch) {
        TestRunParameters testRunParameters = createTestParameters(null, device, configuration);

        List<BaseListener> testRunListeners = new ArrayList<>();
        testRunListeners.add(testCollectingListener);
        if (configuration.getTongsIntegrationTestRunType() == TongsConfiguration.TongsIntegrationTestRunType.RECORD_LISTENER_EVENTS) {
            testRunListeners.add(new RecordingTestRunListener(device, true, null));
        }

        TestRule collectingTestRule = new AndroidCollectingTestRule(device, testCollectingListener, latch);

        return new AndroidInstrumentedTestRun(
                pool.getName(),
                testRunParameters,
                testRunListeners,
                Collections.singletonList(collectingTestRule),
                null,
                remoteAndroidTestRunnerFactory()
        );
    }

    private static TestRunParameters createTestParameters(TestCaseEvent testCase, AndroidDevice device, Configuration configuration) {
        return testRunParameters()
                .withDeviceInterface(device.getDeviceInterface())
                .withTest(testCase)
                .withTestPackage(configuration.getInstrumentationPackage())
                .withApplicationPackage(configuration.getApplicationPackage())
                .withTestRunner(configuration.getTestRunnerClass())
                .withTestRunnerArguments(configuration.getTestRunnerArguments())
                .withTestSize(configuration.getTestSize())
                .withTestOutputTimeout((int) configuration.getTestOutputTimeout())
                .withCoverageEnabled(configuration.isCoverageEnabled())
                .withExcludedAnnotation(configuration.getExcludedAnnotation())
                .build();
    }

}
