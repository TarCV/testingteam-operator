/*
 * Copyright 2019 TarCV
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License.
 *
 * You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */
package com.github.tarcv.tongs.runner.listeners

import com.android.ddmlib.testrunner.ITestRunListener
import com.github.tarcv.tongs.runner.PreregisteringLatch

/**
 * Base run listener class
 *  It either should signal completing its work by calling this class methods or pass null as a latch to this class
 */
abstract class BaseListener(private val latch: PreregisteringLatch?) : ITestRunListener {
    var finished = false

    init {
        latch?.register()
    }

    fun onWorkFinished() {
        if (!finished) {
            latch?.countDown()
            finished = true
        }
    }

    override fun testRunEnded(elapsedTime: Long, runMetrics: Map<String, String>?) {
        onWorkFinished()
    }
}

class BaseListenerWrapper(latch: PreregisteringLatch?, private val wrapped: ITestRunListener) : ITestRunListener by wrapped, BaseListener(latch) {
    override fun testRunEnded(elapsedTime: Long, runMetrics: Map<String, String>?) {
        wrapped.testRunEnded(elapsedTime, runMetrics)
        onWorkFinished()
    }
}