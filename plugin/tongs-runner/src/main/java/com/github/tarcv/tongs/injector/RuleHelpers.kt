package com.github.tarcv.tongs.injector

import com.github.tarcv.tongs.runner.rules.RuleFactory
import java.lang.reflect.InvocationTargetException

abstract class BaseRuleManager<C, out R, T: RuleFactory<C, R>> @JvmOverloads constructor(
        ruleClassNames: Collection<String>,
        predefinedFactories: Collection<T> = emptyList()
) {
    val ruleInstances: List<R> by lazy {
        val instances = ArrayList<R>()

        ruleInstancesFromFactories(predefinedFactories.asSequence()).toCollection(instances)
        factoryInstancesForRuleNames(ruleClassNames)
                .let {
                    ruleInstancesFromFactories(it as Sequence<T>)
                }
                .toCollection(instances)

        instances
    }

    abstract protected fun ruleContextFactory(): C

    inline fun forEach(block: (R) -> Unit) {
        ruleInstances.forEach {
            // TODO: add defensive code
            block(it)
        }
    }

    inline fun forEachReversed(block: (R) -> Unit) {
        ruleInstances
                .reversed()
                .forEach {
                    // TODO: add defensive code
                    block(it)
                }
    }

    fun <RR>mapSequence(block: (R) -> RR): Sequence<RR> {
        return ruleInstances
                .asSequence()
                .map(block)
    }

    fun ruleInstancesFromFactories(
            factories: Sequence<T>
    ): Sequence<R> {
        return factories
                .map { factory ->
                    try {
                        val ruleContext = ruleContextFactory()
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