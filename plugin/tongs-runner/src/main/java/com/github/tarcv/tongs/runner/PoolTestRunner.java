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
package com.github.tarcv.tongs.runner;

import com.github.tarcv.tongs.model.Device;
import com.github.tarcv.tongs.model.Pool;
import com.github.tarcv.tongs.model.TestCaseEvent;

import com.github.tarcv.tongs.model.TestCaseEventQueue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Queue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;

import static com.github.tarcv.tongs.Utils.namedExecutor;

public class PoolTestRunner implements Runnable {
    private final Logger logger = LoggerFactory.getLogger(PoolTestRunner.class);
    public static final String DROPPED_BY = "DroppedBy-";

    private final Pool pool;
    private final TestCaseEventQueue testCases;
    private final CountDownLatch poolCountDownLatch;
    private final DeviceTestRunnerFactory deviceTestRunnerFactory;
    private final ProgressReporter progressReporter;

    public PoolTestRunner(DeviceTestRunnerFactory deviceTestRunnerFactory, Pool pool,
                          TestCaseEventQueue testCases,
                          CountDownLatch poolCountDownLatch,
                          ProgressReporter progressReporter) {
        this.pool = pool;
        this.testCases = testCases;
        this.poolCountDownLatch = poolCountDownLatch;
        this.deviceTestRunnerFactory = deviceTestRunnerFactory;
        this.progressReporter = progressReporter;
    }

    public void run() {
        ExecutorService concurrentDeviceExecutor = null;
        String poolName = pool.getName();
        try {
            int devicesInPool = pool.size();
            concurrentDeviceExecutor = namedExecutor(devicesInPool, "DeviceExecutor-%d");
            CountDownLatch deviceCountDownLatch = new CountDownLatch(devicesInPool);
            logger.info("Pool {} started", poolName);
            for (Device device : pool.getDevices()) {
                Runnable deviceTestRunner = deviceTestRunnerFactory.createDeviceTestRunner(pool, testCases,
                        deviceCountDownLatch, device, progressReporter);
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
}
