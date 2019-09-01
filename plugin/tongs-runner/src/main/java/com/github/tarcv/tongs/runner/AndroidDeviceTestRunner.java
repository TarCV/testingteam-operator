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

import com.android.ddmlib.DdmPreferences;
import com.android.ddmlib.IDevice;
import com.github.tarcv.tongs.model.AndroidDevice;
import com.github.tarcv.tongs.model.Device;
import com.github.tarcv.tongs.model.Pool;
import com.github.tarcv.tongs.model.TestCaseEventQueue;
import com.github.tarcv.tongs.system.adb.Installer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static com.github.tarcv.tongs.device.DeviceUtilsKt.clearLogcat;
import static com.github.tarcv.tongs.system.io.RemoteFileManager.*;

public class AndroidDeviceTestRunner implements Runnable {
    private static final Logger logger = LoggerFactory.getLogger(AndroidDeviceTestRunner.class);

    private final Installer installer;
    private final Pool pool;
    private final AndroidDevice device;
    private final TestCaseEventQueue queueOfTestsInPool;
    private final CountDownLatch deviceCountDownLatch;
    private final ProgressReporter progressReporter;
    private final AndroidTestRunFactory androidTestRunFactory;

    public AndroidDeviceTestRunner(Installer installer,
                                   Pool pool,
                                   Device device,
                                   TestCaseEventQueue queueOfTestsInPool,
                                   CountDownLatch deviceCountDownLatch,
                                   ProgressReporter progressReporter,
                                   AndroidTestRunFactory androidTestRunFactory) {
        this.installer = installer;
        this.pool = pool;
        this.device = (AndroidDevice) device;
        this.queueOfTestsInPool = queueOfTestsInPool;
        this.deviceCountDownLatch = deviceCountDownLatch;
        this.progressReporter = progressReporter;
        this.androidTestRunFactory = androidTestRunFactory;
    }

    @Override
    public void run() {
        IDevice deviceInterface = device.getDeviceInterface();
        try {
            DdmPreferences.setTimeOut(30000);
            installer.prepareInstallation(deviceInterface);
            // For when previous run crashed/disconnected and left files behind
            removeRemoteDirectory(deviceInterface);
            createRemoteDirectory(deviceInterface);
            createCoverageDirectory(deviceInterface);
            clearLogcat(deviceInterface);

            while (true) {
                TestCaseEventQueue.TestCaseTask testCaseTask = queueOfTestsInPool.pollForDevice(device, 10);
                PreregisteringLatch workCountdownLatch = new PreregisteringLatch();
                if (testCaseTask != null) {
                    testCaseTask.doWork(testCaseEvent -> {
                        try {
                            AndroidInstrumentedTestRun testRun = androidTestRunFactory.createTestRun(testCaseEvent,
                                    device,
                                    pool,
                                    progressReporter,
                                    queueOfTestsInPool,
                                    workCountdownLatch);
                            workCountdownLatch.finalizeRegistering();
                            testRun.execute();
                        } finally {
                            workCountdownLatch.await(15, TimeUnit.SECONDS);
                        }
                        return null;
                    });
                } else if (queueOfTestsInPool.hasNoPotentialEventsFor(device)) {
                    break;
                }
            }
        } finally {
            logger.info("Device {} from pool {} finished", device.getSerial(), pool.getName());
            deviceCountDownLatch.countDown();
        }
    }


}
