package com.github.tarcv.tongs.runner.rules

import com.github.tarcv.tongs.TongsConfiguration
import com.github.tarcv.tongs.model.Device
import com.github.tarcv.tongs.model.Pool
import com.github.tarcv.tongs.system.io.TestCaseFileManager

class PoolRuleContext (
        val configuration: TongsConfiguration,
        val fileManager: TestCaseFileManager,
        val pool: Pool
)
interface PoolRuleFactory<D: Device, T: PoolRule>: RuleFactory<PoolRuleContext, T> {
    override fun create(context: PoolRuleContext): T
}
interface PoolRule {
    fun before()
    fun after()
}
