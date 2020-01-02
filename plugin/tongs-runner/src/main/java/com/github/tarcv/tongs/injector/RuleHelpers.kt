/*
 * Copyright 2020 TarCV
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License.
 *
 * You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */
package com.github.tarcv.tongs.injector

import com.github.tarcv.tongs.runner.rules.RuleFactory
import java.lang.reflect.InvocationTargetException

abstract class BaseRuleManager<C, out R, T: RuleFactory<C, R>> @JvmOverloads constructor(
        private val ruleClassNames: Collection<String>,
        private val predefinedFactories: Collection<T> = emptyList()
) {
    fun createRulesFrom(contextProvider: () -> C): List<R> {
        val instances = ArrayList<R>()

        ruleInstancesFromFactories(predefinedFactories.asSequence(), contextProvider).toCollection(instances)
        factoryInstancesForRuleNames(ruleClassNames)
                .let {
                    ruleInstancesFromFactories(it as Sequence<T>, contextProvider)
                }
                .toCollection(instances)

        return instances
    }

    fun ruleInstancesFromFactories(
            factories: Sequence<T>,
            contextProvider: () -> C
    ): Sequence<R> {
        return factories
                .map { factory ->
                    try {
                        val ruleContext = contextProvider()
                        factory.create(ruleContext)
                    } catch (e: InvocationTargetException) {
                        throw RuntimeException(e.targetException) //TODO
                    }
                }
    }

    private fun factoryInstancesForRuleNames(ruleClassNames: Collection<String>): Sequence<T> {
        return ruleClassNames
                .asSequence()
                .map { className ->
                    Class.forName(className + "Factory") as Class<T>
                }
                .map { clazz ->
                    try {
                        val ctor = clazz.getConstructor()
                        ctor.newInstance()
                    } catch (e: InvocationTargetException) {
                        throw RuntimeException(e.targetException) //TODO
                    }
                }
    }
}

class RuleCollection<out R>(val rules: Collection<R>) {
    inline fun forEach(block: (R) -> Unit) {
        rules.forEach {
            // TODO: add defensive code
            block(it)
        }
    }

    inline fun forEachReversed(block: (R) -> Unit) {
        rules
                .reversed()
                .forEach {
                    // TODO: add defensive code
                    block(it)
                }
    }

    fun <RR>mapSequence(block: (R) -> RR): Sequence<RR> {
        return rules
                .asSequence()
                .map(block)
    }
}