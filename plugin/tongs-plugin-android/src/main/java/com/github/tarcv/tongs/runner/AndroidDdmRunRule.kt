package com.github.tarcv.tongs.runner

import com.android.ddmlib.AndroidDebugBridge
import com.github.tarcv.tongs.runner.rules.RunRule
import com.github.tarcv.tongs.runner.rules.RunRuleContext
import com.github.tarcv.tongs.runner.rules.RunRuleFactory

class AndroidDdmRunRuleFactory: RunRuleFactory<AndroidDdmRunRule> {
    override fun create(context: RunRuleContext): AndroidDdmRunRule {
        return AndroidDdmRunRule(context.configuration.shouldTerminateDdm());
    }
}

class AndroidDdmRunRule(private val enabled: Boolean): RunRule {

    override fun before() {
        // no op
    }

    override fun after() {
        if (enabled) {
            AndroidDebugBridge.terminate();
        }
    }

}