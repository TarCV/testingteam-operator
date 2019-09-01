package com.github.tarcv.tongs.runner

import com.github.tarcv.tongs.TongsConfiguration
import com.github.tarcv.tongs.model.Device
import com.github.tarcv.tongs.model.Pool
import com.github.tarcv.tongs.model.TestCaseEvent

class TongsTestCaseContext<T: Device>(
        val configuration: TongsConfiguration,
        val pool: Pool,
        val device: T,
        val testCaseEvent: TestCaseEvent
)

interface TestRuleFactory<D: Device, T: TestRule<D>> {
    fun create(context: TongsTestCaseContext<D>): T
}

interface TestRule<D: Device> {
    fun before()
    fun after()
}
