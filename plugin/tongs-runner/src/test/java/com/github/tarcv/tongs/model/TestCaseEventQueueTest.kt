/*
 * Copyright 2020 TarCV
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License.
 *
 * You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */
package com.github.tarcv.tongs.model

import com.github.tarcv.tongs.api.devices.Device
import com.github.tarcv.tongs.api.devices.Diagnostics
import com.github.tarcv.tongs.api.devices.DisplayGeometry
import com.github.tarcv.tongs.pooling.StubDevice
import com.github.tarcv.tongs.api.result.TestCaseRunResult
import com.github.tarcv.tongs.api.result.TestCaseRunResult.Companion.NO_TRACE
import com.github.tarcv.tongs.api.run.ResultStatus
import com.github.tarcv.tongs.api.run.TestCaseEvent
import com.github.tarcv.tongs.api.run.TestCaseEvent.Companion.TEST_TYPE_TAG
import org.junit.Assert
import org.junit.Test
import java.lang.Thread.sleep
import java.util.concurrent.atomic.AtomicReference
import kotlin.concurrent.thread

class TestCaseEventQueueTest {
    private val device1 = createStubDevice("dev1")
    private val device2 = createStubDevice("dev2")

    @Test
    fun testNoExcludes() {
        val test1 = createTestCaseEvent("test1", emptyList())
        val test2 = createTestCaseEvent("test2", emptyList())
        val queue = TestCaseEventQueue(listOf(
                test1,
                test2
        ), mutableListOf())
        withTimeout {
            queue.pollForDevice(device1)!!.doWork {
                Assert.assertEquals(test1, it)

                TestCaseRunResult.aTestResult("", "", ResultStatus.PASS, NO_TRACE)
            }
            queue.pollForDevice(device2)!!.doWork {
                Assert.assertEquals(test2, it)

                TestCaseRunResult.aTestResult("", "", ResultStatus.PASS, NO_TRACE)
            }
        }
    }

    @Test
    fun testSimpleExclude() {
        val test1 = createTestCaseEvent("test1", listOf(device1))
        val test2 = createTestCaseEvent("test2", emptyList())
        val test3 = createTestCaseEvent("test3", listOf(device2))
        val test4 = createTestCaseEvent("test4", emptyList())
        val queue = TestCaseEventQueue(listOf(
                test1,
                test2,
                test3,
                test4
        ), mutableListOf())
        withTimeout {
            queue.pollForDevice(device1)!!.doWork {
                Assert.assertEquals(test2, it)

                TestCaseRunResult.aTestResult("", "", ResultStatus.PASS, NO_TRACE)
            }
            queue.pollForDevice(device2)!!.doWork {
                Assert.assertEquals(test1, it)

                TestCaseRunResult.aTestResult("", "", ResultStatus.PASS, NO_TRACE)
            }
        }
    }

    @Test
    fun testWaitingForCompatibleTest() {
        val test1 = createTestCaseEvent("test1", listOf(device1))
        val test2 = createTestCaseEvent("test2", emptyList())
        val queue = TestCaseEventQueue(listOf(
                test1
        ), mutableListOf())
        thread(start = true) {
            sleep(100)

            try {
                queue.offer(test2)
            } catch (_: IllegalStateException) {
                // intentional no-op
            }
        }

        withTimeout {
            queue.pollForDevice(device1)!!.doWork {
                Assert.assertEquals(test2, it)

                TestCaseRunResult.aTestResult("", "", ResultStatus.PASS, NO_TRACE)
            }
        }
    }
}

private fun withTimeout(block: () -> Unit) {
    val exception = AtomicReference<Throwable>()
    thread(start = true, block = {
        try {
            block()
        } catch (t: Throwable) {
            exception.set(t)
        }
    }).join(1000)

    exception.get().let {
        if (it != null) {
            throw it
        }
    }
}

private fun createTestCaseEvent(name: String, excludes: List<Device>) =
        TestCaseEvent.newTestCase(TEST_TYPE_TAG, name, "class", emptyMap(), emptyList(), Any(), excludes)

private fun createStubDevice(serial: String): Device {
    val api = 20
    val manufacturer = "tongs"
    val model = "Emu-$api"
    val stubDevice = StubDevice(serial, manufacturer, model, serial, api, "", 1)
    return object: Device() {
        override fun getHost(): String = "localhost"
        override fun getSerial(): String = serial
        override fun getManufacturer(): String = manufacturer
        override fun getModelName(): String = model
        override fun getOsApiLevel(): Int = api
        override fun getLongName(): String = "${serial} ($model)"
        override fun getDeviceInterface(): Any = stubDevice
        override fun isTablet(): Boolean = false
        override fun getGeometry(): DisplayGeometry? = DisplayGeometry(640)
        override fun getSupportedVisualDiagnostics(): Diagnostics = Diagnostics.VIDEO
        override fun getUniqueIdentifier(): Any = getSerial()
    }
}
