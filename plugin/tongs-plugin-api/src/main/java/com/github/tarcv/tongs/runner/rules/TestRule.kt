package com.github.tarcv.tongs.runner.rules

import com.github.tarcv.tongs.TongsConfiguration
import com.github.tarcv.tongs.model.Device
import com.github.tarcv.tongs.model.Pool
import com.github.tarcv.tongs.model.TestCaseEvent
import com.github.tarcv.tongs.system.io.TestCaseFileManager

class TestRuleContext<T: Device>(
        val configuration: TongsConfiguration,
        val fileManager: TestCaseFileManager,
        val pool: Pool,
        val device: T,
        val testCaseEvent: TestCaseEvent
)

interface TestRuleFactory<D: Device, T: TestRule<D>>: RuleFactory<TestRuleContext<D>, T> {
    override fun create(context: TestRuleContext<D>): T
}

interface TestRule<D: Device> {
    fun before()
    fun after()
}
