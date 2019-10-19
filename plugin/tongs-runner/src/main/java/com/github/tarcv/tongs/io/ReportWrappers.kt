package com.github.tarcv.tongs.io

import org.slf4j.LoggerFactory
import java.beans.Introspector
import java.nio.file.Paths
import kotlin.reflect.KClass
import kotlin.reflect.full.memberProperties

class ReportWrapperEntry(override val key: String, override val value: Any) : Map.Entry<String, Any>

class ReportWrapper<T: Any>(private val wrapped: T): Map<String, Any> {
    companion object {
        private val logger = LoggerFactory.getLogger(ReportWrapper::class.java)
        val lowerCaseSuffix = "lowerCase"
        val upperCaseSuffix = "upperCase"
        val despacedUnderscoreSuffix = "despaced_"
        val unixPathSuffix = "unixPath"
        val equalsSuffix = "equals"

        val suffixes = listOf("", lowerCaseSuffix, upperCaseSuffix,
                despacedUnderscoreSuffix, unixPathSuffix, equalsSuffix)
    }

    @Suppress("UNCHECKED_CAST")
    val memberProperties = (wrapped::class as KClass<T>).memberProperties

    override val entries: Set<Map.Entry<String, Any>>
        get() {
            return keys.asSequence()
                    .map { key -> key to this[key] }
                    .filter { it.second != null }
                    .map { ReportWrapperEntry(it.first, it.second!!) }
                    .toSet()
        }
    override val keys: Set<String>
        get() = entries.map { it.key }.toSet()
    override val size: Int
        get() = entries.size
    override val values: Collection<Any>
        get() = entries.map { it.value }

    // TODO: test that non property methods of wrapper are never called
    override fun get(key: String): Any? {
        val parts = key.split("-", limit = 2)
        val propertyName = parts[0]
        val suffix = parts.getOrNull(1) ?: ""
        val beanInfo = Introspector.getBeanInfo(wrapped::class.java, Object::class.java)
        return beanInfo.propertyDescriptors.asSequence()
                .singleOrNull { it.name == propertyName }
                .let {
                    if (it == null) {
                        logger.warn("Missing property '$propertyName' (class ${wrapped::class.simpleName}) is used in a template")
                        null
                    } else {
                        it.readMethod
                    }
                }
                ?.let {
                    try {
                        it.invoke(wrapped)
                    } catch (e: java.lang.Exception) {
                        logger.warn("Failed to read $propertyName (class ${wrapped::class.simpleName})", e)
                        null
                    }
                }
                ?.let {
                        when (suffix) {
                            lowerCaseSuffix -> it.toString().toLowerCase()
                            upperCaseSuffix -> it.toString().toUpperCase()
                            despacedUnderscoreSuffix -> it.toString().replace(" ", "_")
                            equalsSuffix -> mapOf(it.toString() to true)
                            unixPathSuffix -> try {
                                Paths.get(it.toString()).joinToString("/")
                            } catch (e: Exception) {
                                logger.warn("Failed to convert $it to a unix path", e)
                            }
                            else -> wrap(it)
                        }
                }
    }

    override fun toString(): String = wrapped.toString()

    private fun wrap(it: Any?): Any? {
        return if (it == null) {
            null
        } else if (it is Map<*, *>) {
            it.entries.asSequence()
                    .map { (k, v) -> k to wrap(v) }
                    .toMap()
        } else if (it is Iterable<*>) {
            it.asSequence()
                    .map { item -> wrap(item) }
                    .toList()
        } else {
            ReportWrapper(it)
        }
    }

    override fun containsKey(key: String): Boolean = get(key) != null

    override fun isEmpty(): Boolean = false

    override fun containsValue(value: Any): Boolean = throw UnsupportedOperationException()
}