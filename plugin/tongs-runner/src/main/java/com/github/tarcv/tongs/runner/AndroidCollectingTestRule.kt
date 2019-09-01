package com.github.tarcv.tongs.runner

import com.android.ddmlib.logcat.LogCatMessage
import com.android.ddmlib.testrunner.TestIdentifier
import com.github.tarcv.tongs.device.clearLogcat
import com.github.tarcv.tongs.model.AndroidDevice
import com.github.tarcv.tongs.model.Device
import com.github.tarcv.tongs.runner.listeners.LogcatReceiver
import com.github.tarcv.tongs.suite.JUnitTestSuiteLoader
import com.github.tarcv.tongs.suite.JUnitTestSuiteLoader.Companion.logcatWaiterSleep
import com.github.tarcv.tongs.suite.TestCollectingListener
import com.google.gson.JsonObject
import java.lang.Thread.sleep
import java.util.concurrent.CountDownLatch

internal class AndroidCollectingTestRule(
        private val device: Device,
        private val testCollectingListener: TestCollectingListener,
        private val latch: CountDownLatch
): TestRule<AndroidDevice> {
    var logCatCollector: LogcatReceiver = LogcatReceiver(device)

    override fun before() {
        clearLogcat((device as AndroidDevice).deviceInterface)
        logCatCollector.start("TestSuiteLoader")
    }

    override fun after() {
        try {
            sleep(logcatWaiterSleep) // make sure all logcat messages are read
        } finally {
            logCatCollector.stop()

            val infoMessages = extractTestInfoMessages(logCatCollector.messages)
                    .reversed() // make sure the first entry for duplicate keys is used
                    .associateBy {
                        val testClass = it.get("testClass").asString
                        val testMethod = it.get("testMethod").asString
                        TestIdentifier(testClass, testMethod)
                    }
                    .map { it }
            testCollectingListener.publishTestInfo(infoMessages)
            latch.countDown()
        }
    }
}

fun extractTestInfoMessages(messages: List<LogCatMessage>): List<JsonObject> {
    return messages.stream()
            .filter { logCatMessage -> "Tongs.TestInfo" == logCatMessage.tag }
            .collect(JUnitTestSuiteLoader.TestInfoCatCollector())
}
