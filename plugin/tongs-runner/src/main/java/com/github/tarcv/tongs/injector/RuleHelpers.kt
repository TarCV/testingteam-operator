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

import java.lang.reflect.InvocationTargetException

open class RuleManager<C, out R, F> @JvmOverloads constructor(
        private val factoryClass: Class<F>,
        private val predefinedFactories: List<F> = emptyList(),
        allUserFactories: List<Any>,
        private val factoryInvoker: (F, C) -> Array<out R>
) {
    private val userFactories = allUserFactories.filterIsInstance(factoryClass)

    fun createRulesFrom(contextProvider: () -> C): List<R> {
        val instances = ArrayList<R>()

        ruleInstancesFromFactories(predefinedFactories, contextProvider).toCollection(instances)
        ruleInstancesFromFactories(userFactories, contextProvider).toCollection(instances)

        return instances
    }

    private fun ruleInstancesFromFactories(
            factories: List<F>,
            contextProvider: () -> C
    ): Sequence<R> {
        return factories.asSequence()
                .flatMap { factory ->
                    try {
                        val ruleContext = contextProvider()
                        factoryInvoker(factory, ruleContext).asSequence()
                    } catch (e: InvocationTargetException) {
                        throw RuntimeException(e.targetException) //TODO
                    }
                }
    }

    companion object {
        @JvmStatic
        fun factoryInstancesForRuleNames(ruleClassNames: Collection<String>): List<Any> {
            return ruleClassNames
                    .asSequence()
                    .map { className ->
                        Class.forName(className) as Class<Any>
                    }
                    .map { clazz ->
                        try {
                            val ctor = clazz.getConstructor()
                            ctor.newInstance()
                        } catch (e: InvocationTargetException) {
                            throw RuntimeException(e.targetException) //TODO
                        }
                    }
                    .toList()
        }

        @JvmStatic
        fun <T> fixGenericClass(clazz: Class<in T>): Class<T> {
            return clazz as Class<T>
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