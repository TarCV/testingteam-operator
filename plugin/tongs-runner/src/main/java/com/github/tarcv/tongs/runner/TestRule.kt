package com.github.tarcv.tongs.runner

import com.github.tarcv.tongs.TongsConfiguration
import com.github.tarcv.tongs.model.Device
import com.github.tarcv.tongs.model.Pool
import com.github.tarcv.tongs.model.TestCaseEvent

class TongsTestCaseContext(
        val configuration: TongsConfiguration,
        val pool: Pool,
        val device: Device,
        val testCaseEvent: TestCaseEvent
)

interface TestRuleFactory<T : TestRule> {
    fun create(context: TongsTestCaseContext): T
}

interface TestRule {
    fun before()
    fun after()
}
