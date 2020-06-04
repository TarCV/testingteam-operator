/*
 * Copyright 2019 TarCV
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License.
 *
 * You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */

package com.github.tarcv.tongs.suite

import com.android.ddmlib.Log
import com.android.ddmlib.logcat.LogCatMessage
import com.google.gson.JsonObject

import org.junit.Assert
import org.junit.Test

import java.util.function.BiConsumer
import java.util.function.BinaryOperator
import java.util.function.Function
import java.util.function.Supplier
import java.util.stream.Collector

class TestInfoCatCollectorTest {
    private val collector = JUnitTestSuiteLoader.TestInfoCatCollector()

    @Test
    fun testSingleThread() {
        val bundle = CollectorBundle(collector)

        val finalResult: List<JsonObject> = with(bundle) {
            val result = supplier.get()
            accumulator.accept(result, LogCatMessage(Log.LogLevel.INFO, "0000-1001:{"))
            accumulator.accept(result, LogCatMessage(Log.LogLevel.INFO, "0000-1002:\"a"))
            accumulator.accept(result, LogCatMessage(Log.LogLevel.INFO, "0000-1003:\": 0"))
            accumulator.accept(result, LogCatMessage(Log.LogLevel.INFO, "0000-1004:}"))
            finisher.apply(result)
        }

        Assert.assertEquals(1, finalResult.size.toLong())
        val resultObject = finalResult[0]
        Assert.assertEquals(1, resultObject.entrySet().size.toLong())
        Assert.assertEquals(0, resultObject.get("a").asInt.toLong())
    }

    @Test
    fun testTwoThreadsAndOutOfOrder() {
        val bundle1 = CollectorBundle(collector)
        val bundle2 = CollectorBundle(collector)

        val result1 = with(bundle1) {
            val result = supplier.get()
            accumulator.accept(result, LogCatMessage(Log.LogLevel.INFO, "2000-3001:{"))
            accumulator.accept(result, LogCatMessage(Log.LogLevel.INFO, "2000-3005:\": 1"))
            accumulator.accept(result, LogCatMessage(Log.LogLevel.INFO, "2000-3003:\": 0,"))
            result
        }

        val result2 = with(bundle2) {
            val result = supplier.get()
            accumulator.accept(result, LogCatMessage(Log.LogLevel.INFO, "2000-3002:\"a"))
            accumulator.accept(result, LogCatMessage(Log.LogLevel.INFO, "2000-3004:\"b"))
            accumulator.accept(result, LogCatMessage(Log.LogLevel.INFO, "2000-3006:}"))
            result
        }

        val combined = bundle1.combiner.apply(result1, result2)
        val finalResult = bundle1.finisher.apply(combined)

        Assert.assertEquals(1, finalResult.size.toLong())
        val resultObject = finalResult[0]
        Assert.assertEquals(2, resultObject.entrySet().size.toLong())
        Assert.assertEquals(0, resultObject.get("a").asInt.toLong())
        Assert.assertEquals(1, resultObject.get("b").asInt.toLong())
    }
}

private class CollectorBundle<T, A, R>(
        collector: Collector<T, A, R>
) {
    val supplier: Supplier<A> = collector.supplier()
    val accumulator: BiConsumer<A, T> = collector.accumulator()
    val combiner: BinaryOperator<A> = collector.combiner()
    val finisher: Function<A, R> = collector.finisher()
}
