package com.github.tarcv.tongs.model

import com.github.tarcv.tongs.pooling.StubDevice
import org.junit.Assert
import org.junit.Test
import java.lang.Thread.sleep
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
        ))
        withTimeout {
            Assert.assertEquals(test1, queue.pollForDevice(device1))
            Assert.assertEquals(test2, queue.pollForDevice(device2))
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
        ))
        withTimeout {
            Assert.assertEquals(test2, queue.pollForDevice(device1))
            Assert.assertEquals(test1, queue.pollForDevice(device2))
        }
    }

    @Test
    fun testWaitingForCompatibleTest() {
        val test1 = createTestCaseEvent("test1", listOf(device1))
        val test2 = createTestCaseEvent("test2", emptyList())
        val queue = TestCaseEventQueue(listOf(
                test1
        ))
        thread(start = true) {
            sleep(100)
            queue.offer(test2)
        }

        withTimeout {
            Assert.assertEquals(test2, queue.pollForDevice(device1))
        }
    }
}

private fun withTimeout(block: () -> Unit) {
    thread(start = true, block = block).join(1000)
}

private fun createTestCaseEvent(name: String, excludes: List<Device>) =
        TestCaseEvent.newTestCase("class", name, false, emptyList(), emptyMap(), null, excludes)

private fun createStubDevice(serial: String): Device {
    val api = 20
    val manufacturer = "tongs"
    val model = "Emu-$api"
    val stubDevice = StubDevice(serial, manufacturer, model, serial, api, "", 1)
    return Device.Builder()
            .withApiLevel(api.toString())
            .withDisplayGeometry(DisplayGeometry(640))
            .withManufacturer(manufacturer)
            .withModel(model)
            .withSerial(serial)
            .withDeviceInterface(stubDevice)
            .build()
}
