/*
 * Copyright 2019 TarCV
 * Copyright 2018 Shazam Entertainment Limited
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License.
 *
 * You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */

package com.github.tarcv.tongs.runner.listeners;

import com.github.tarcv.tongs.device.DeviceTestFilesCleaner;
import com.github.tarcv.tongs.model.Device;
import com.github.tarcv.tongs.model.Pool;
import com.github.tarcv.tongs.model.TestCase;
import com.github.tarcv.tongs.model.TestCaseEvent;
import com.github.tarcv.tongs.runner.PreregisteringLatch;
import com.github.tarcv.tongs.runner.TestRetryer;
import com.github.tarcv.tongs.util.TestPipelineEmulator;
import org.jmock.Expectations;
import org.jmock.auto.Mock;
import org.jmock.integration.junit4.JUnitRuleMockery;
import org.junit.Rule;
import org.junit.Test;

import static com.github.tarcv.tongs.model.AndroidDevice.Builder.aDevice;
import static com.github.tarcv.tongs.model.Pool.Builder.aDevicePool;
import static com.github.tarcv.tongs.model.TestCaseEvent.newTestCase;
import static com.github.tarcv.tongs.util.TestPipelineEmulator.Builder.testPipelineEmulator;

public class RetryListenerTest {
    @Rule
    public JUnitRuleMockery mockery = new JUnitRuleMockery();
    @Mock
    private TestRetryer testRetryer;
    @Mock
    private DeviceTestFilesCleaner deviceTestFilesCleaner;

    private final Device device = aDevice().build();
    private final Pool pool = aDevicePool()
            .withName("pool")
            .addDevice(device)
            .build();

    private final TestCase fatalCrashedTest = new TestCase("com.example.FatalCrashedTest", "testMethod");
    private final TestCaseEvent fatalCrashedTestCaseEvent = newTestCase(fatalCrashedTest);

    @Test
    public void reschedulesTestIfTestRunFailedAndDeleteTraceFiles() {
        PreregisteringLatch workCountdownLatch = new PreregisteringLatch();
        RetryListener retryListener =
                new RetryListener(pool, device, fatalCrashedTestCaseEvent, testRetryer, deviceTestFilesCleaner);

        mockery.checking(new Expectations() {{
            oneOf(testRetryer).rescheduleTestExecution(fatalCrashedTestCaseEvent);
            will(returnValue(true));

            oneOf(deviceTestFilesCleaner).deleteTraceFiles(newTestCase(fatalCrashedTest));
        }});

        TestPipelineEmulator emulator = testPipelineEmulator()
                .withFatalErrorMessage("fatal error")
                .build();
        emulator.emulateFor(retryListener, fatalCrashedTest);
    }

    // TODO: Check that total failure count is incremented

    @Test
    public void doesNotDeleteTraceFilesIfCannotRescheduleTestAfterTestRunFailed() {
        PreregisteringLatch workCountdownLatch = new PreregisteringLatch();
        RetryListener retryListener =
                new RetryListener(pool, device, fatalCrashedTestCaseEvent, testRetryer, deviceTestFilesCleaner);

        mockery.checking(new Expectations() {{
            oneOf(testRetryer).rescheduleTestExecution(fatalCrashedTestCaseEvent);
            will(returnValue(false));

            never(deviceTestFilesCleaner).deleteTraceFiles(newTestCase(fatalCrashedTest));
        }});

        TestPipelineEmulator emulator = testPipelineEmulator()
                .withFatalErrorMessage("fatal error")
                .build();
        emulator.emulateFor(retryListener, fatalCrashedTest);
    }
}