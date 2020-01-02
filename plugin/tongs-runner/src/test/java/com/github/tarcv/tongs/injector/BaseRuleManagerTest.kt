/*
 * Copyright 2020 TarCV
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License.
 *
 * You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package com.github.tarcv.tongs.injector

import com.github.tarcv.tongs.runner.rules.RuleFactory
import org.junit.Assert
import org.junit.Test

class BaseRuleManagerTest {
    @Test
    fun example() {
        (0..1).forEach { num ->
            val ruleManager = RunRuleManager(
                    listOf(DefaultActualRule::class.java.name),
                    listOf(PredefinedActualRuleFactory()))
            val ruleNames = ruleManager
                    .createRulesFrom { ActualRuleContext(num) }
                    .map {
                        it.javaClass.name
                    }
            Assert.assertEquals(
                    listOf(
                            PredefinedActualRule::class.java.name,
                            DefaultActualRule::class.java.name),
                    ruleNames)
        }
    }

    private class RunRuleManager(ruleClassNames: Collection<String>, predefinedFactories: Collection<RuleFactory<ActualRuleContext, ActualRule>>)
        : BaseRuleManager<
            ActualRuleContext,
            ActualRule,
            RuleFactory<ActualRuleContext, ActualRule>>(ruleClassNames, predefinedFactories)
}

class PredefinedActualRuleFactory: ActualRuleFactory<PredefinedActualRule> {
    override fun create(context: ActualRuleContext): PredefinedActualRule {
        return PredefinedActualRule()
    }

}
class PredefinedActualRule: ActualRule

class DefaultActualRuleFactory: ActualRuleFactory<DefaultActualRule> {
    override fun create(context: ActualRuleContext): DefaultActualRule {
        return DefaultActualRule()
    }

}
class DefaultActualRule: ActualRule

class ActualRuleContext(someDependency: Int)

private interface ActualRuleFactory<T: ActualRule>: RuleFactory<ActualRuleContext, T> {
    override fun create(context: ActualRuleContext): T
}

private interface ActualRule