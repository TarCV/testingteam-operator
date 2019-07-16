package com.github.tarcv.tongs.model

import java.util.*
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

class TestCaseEventQueue(events: Collection<TestCaseEvent>) {
    private val list = ArrayList<TestCaseEvent>(events)

    private val conditionLock = ReentrantLock()
    private val newItemCondition = conditionLock.newCondition()

    private val numEventsInWork = AtomicInteger()

    fun pollForDevice(device: Device, timeoutSeconds: Long = 0): TestCaseTask? {
        val currentTime = System.currentTimeMillis()
        val timeoutTime = currentTime + timeoutSeconds * 1000
        while (true) {
            conditionLock.withLock {
                val item = tryPollForDevice(device)
                if (item != null) {
                    return TestCaseTask(item)
                }

                newItemCondition.await(1, TimeUnit.SECONDS)
                if (timeoutSeconds > 0 && timeoutTime < System.currentTimeMillis()) {
                    return null
                }
            }
        }
    }

    fun hasNoPotentialEventsFor(device: Device): Boolean {
        conditionLock.withLock {
            return indexOfEventFor(device) == -1 && numEventsInWork.get() == 0
        }
    }

    fun offer(event: TestCaseEvent) {
        conditionLock.withLock {
            if (numEventsInWork.get() < 1) {
                throw IllegalStateException("TestCaseEventQueue.offer can only be called during TestCaseTask.doWork")
            }
            list.add(event)

            newItemCondition.signalAll()
        }
    }

    private fun tryPollForDevice(device: Device): TestCaseEvent? {
        return conditionLock.withLock {
            val itemIndex = indexOfEventFor(device)
            if (itemIndex != -1) {
                list.removeAt(itemIndex)
            } else {
                null
            }
        }
    }

    private fun indexOfEventFor(device: Device) = list.indexOfFirst { !it.isExcluded(device) }

    inner class TestCaseTask(private val testCaseEvent: TestCaseEvent) {
        fun doWork(block: (testCaseEvent: TestCaseEvent) -> Unit) {
            try {
                numEventsInWork.incrementAndGet()
                block.invoke(testCaseEvent)
            } finally {
                val result = numEventsInWork.decrementAndGet()
                if (result < 0) {
                    throw IllegalStateException()
                }
            }
        }
    }
}