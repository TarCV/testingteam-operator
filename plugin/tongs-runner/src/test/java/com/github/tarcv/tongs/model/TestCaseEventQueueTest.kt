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
        ), mutableListOf())
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
        ), mutableListOf())
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
        ), mutableListOf())
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
        TestCaseEvent.newTestCase(name, "class", emptyList(), emptyMap(), null, excludes)

private fun createStubDevice(serial: String): Device {
    val api = 20
    val manufacturer = "tongs"
    val model = "Emu-$api"
    val stubDevice = StubDevice(serial, manufacturer, model, serial, api, "", 1)
    return object: Device() {
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
