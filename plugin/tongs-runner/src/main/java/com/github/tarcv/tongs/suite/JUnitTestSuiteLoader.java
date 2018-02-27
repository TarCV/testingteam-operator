package com.github.tarcv.tongs.suite;

/*
 * Copyright 2018 TarCV
 * Copyright 2016 Shazam Entertainment Limited
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License.
 *
 * You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */

import com.android.ddmlib.AdbCommandRejectedException;
import com.android.ddmlib.DdmPreferences;
import com.android.ddmlib.IDevice;
import com.android.ddmlib.ShellCommandUnresponsiveException;
import com.android.ddmlib.TimeoutException;
import com.android.ddmlib.testrunner.ITestRunListener;
import com.android.ddmlib.testrunner.RemoteAndroidTestRunner;
import com.android.ddmlib.testrunner.TestIdentifier;
import com.github.tarcv.tongs.Configuration;
import com.github.tarcv.tongs.TongsConfiguration;
import com.github.tarcv.tongs.model.Device;
import com.github.tarcv.tongs.model.Pool;
import com.github.tarcv.tongs.model.TestCaseEvent;
import com.github.tarcv.tongs.pooling.NoDevicesForPoolException;
import com.github.tarcv.tongs.pooling.NoPoolLoaderConfiguredException;
import com.github.tarcv.tongs.pooling.PoolLoader;
import com.github.tarcv.tongs.runner.IRemoteAndroidTestRunnerFactory;
import com.github.tarcv.tongs.runner.listeners.RecordingTestRunListener;
import com.github.tarcv.tongs.system.adb.Installer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.stream.Collectors;

import static com.github.tarcv.tongs.Utils.namedExecutor;
import static com.github.tarcv.tongs.injector.ConfigurationInjector.configuration;
import static com.github.tarcv.tongs.injector.system.InstallerInjector.installer;

public class JUnitTestSuiteLoader implements TestSuiteLoader {
    private final Logger logger = LoggerFactory.getLogger(JUnitTestSuiteLoader.class);
    private final PoolLoader poolLoader;
    private final IRemoteAndroidTestRunnerFactory remoteAndroidTestRunnerFactory;

    public JUnitTestSuiteLoader(
            PoolLoader poolLoader,
            IRemoteAndroidTestRunnerFactory remoteAndroidTestRunnerFactory) {
        this.poolLoader = poolLoader;
        this.remoteAndroidTestRunnerFactory = remoteAndroidTestRunnerFactory;
    }

    @Override
    public Collection<TestCaseEvent> loadTestSuite() throws NoTestCasesFoundException {
        // 1. Ask instrumentation runner to provide list of testcases for us
        Set<TestIdentifier> knownTests = askDevicesForTests();

        // TODO: match list of testcases with annotations

        return knownTests.stream()
                .map(TestCaseEvent::newTestCase)
                .collect(Collectors.toList());
    }

    private Set<TestIdentifier> askDevicesForTests() {
        ExecutorService poolExecutor = null;
        try {
            TestCollectingListener testCollector = new TestCollectingListener();
            Collection<Pool> pools = poolLoader.loadPools();
            int numberOfPools = pools.size();
            CountDownLatch poolCountDownLatch = new CountDownLatch(numberOfPools);
            poolExecutor = namedExecutor(numberOfPools, "PoolSuiteLoader-%d");

            for (Pool pool : pools) {
                Runnable poolTestRunner = new Runnable() {
                    @Override
                    public void run() {
                        ExecutorService concurrentDeviceExecutor = null;
                        String poolName = pool.getName();
                        try {
                            int devicesInPool = pool.size();
                            concurrentDeviceExecutor = namedExecutor(devicesInPool, "DeviceExecutor-%d");
                            CountDownLatch deviceCountDownLatch = new CountDownLatch(devicesInPool);
                            logger.info("Pool {} started", poolName);
                            final Installer installer = installer();
                            final Configuration configuration = configuration();
                            for (Device device : pool.getDevices()) {
                                Runnable deviceTestRunner = new Runnable() {
                                    @Override
                                    public void run() {
                                        IDevice deviceInterface = device.getDeviceInterface();
                                        try {
                                            DdmPreferences.setTimeOut(30000);
                                            installer.prepareInstallation(deviceInterface);

                                            RemoteAndroidTestRunner runner =
                                                    remoteAndroidTestRunnerFactory.createRemoteAndroidTestRunner(
                                                            configuration.getInstrumentationPackage(),
                                                            configuration.getTestRunnerClass(),
                                                            deviceInterface);

                                            runner.setRunName(poolName);
                                            runner.setMaxtimeToOutputResponse((int) configuration.getTestOutputTimeout());

                                            runner.addBooleanArg("log", true);

                                            Collection<ITestRunListener> testRunListeners = new ArrayList<>();
                                            testRunListeners.add(testCollector);
                                            if (configuration.getTongsIntegrationTestRunType() == TongsConfiguration.TongsIntegrationTestRunType.RECORD_LISTENER_EVENTS) {
                                                testRunListeners.add(new RecordingTestRunListener(device, true));
                                            }

                                            try {
                                                runner.run(testRunListeners);
                                            } catch (ShellCommandUnresponsiveException | TimeoutException e) {
                                                logger.warn("Test runner got stuck and test list collection was interrupeted. " +
                                                        " Depending on number of available devices some tests will not be run." +
                                                        " You can increase the timeout in settings if it's too strict");
                                            } catch (AdbCommandRejectedException | IOException e) {
                                                throw new RuntimeException("Error while getting list of testcases from the test runner", e);
                                            }
                                        } finally {
                                            logger.info("Device {} from pool {} finished", device.getSerial(), pool.getName());
                                            deviceCountDownLatch.countDown();
                                        }
                                    }
                                };
                                concurrentDeviceExecutor.execute(deviceTestRunner);
                            }
                            deviceCountDownLatch.await();
                        } catch (InterruptedException e) {
                            logger.warn("Pool {} was interrupted while running", poolName);
                        } finally {
                            if (concurrentDeviceExecutor != null) {
                                concurrentDeviceExecutor.shutdown();
                            }
                            logger.info("Pool {} finished", poolName);
                            poolCountDownLatch.countDown();
                            logger.info("Pools remaining: {}", poolCountDownLatch.getCount());
                        }
                    }
                };
                poolExecutor.execute(poolTestRunner);
            }
            poolCountDownLatch.await();
            logger.info("Successfully loaded test cases");
            return testCollector.getTests();
        } catch (NoPoolLoaderConfiguredException | NoDevicesForPoolException e) {
            // TODO: replace with concrete exception
            throw new RuntimeException("Configuring devices and pools failed." +
                    " Suites can not be read without devices", e);
        } catch (InterruptedException e) {
            // TODO: replace with concrete exception
            throw new RuntimeException("Reading suites were interrupted");
        } finally {
            if (poolExecutor != null) {
                poolExecutor.shutdown();
            }
        }
    }

}
