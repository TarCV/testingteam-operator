package com.github.tarcv.tongs.runner.rules

import com.github.tarcv.tongs.TongsConfiguration
import com.github.tarcv.tongs.model.Device
import com.github.tarcv.tongs.model.Pool
import com.github.tarcv.tongs.system.io.TestCaseFileManager

class DeviceRuleContext<T: Device>(
        val configuration: TongsConfiguration,
        val fileManager: TestCaseFileManager,
        val pool: Pool,
        val device: T
)

interface DeviceRuleFactory<D: Device, T: DeviceRule<D>>: RuleFactory<DeviceRuleContext<D>, T> {
    override fun create(context: DeviceRuleContext<D>): T
}

interface DeviceRule<D: Device> {
    fun before()
    fun after()
}
