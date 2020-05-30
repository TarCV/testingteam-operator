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
package com.github.tarcv.tongs.runner

import com.android.ddmlib.testrunner.TestIdentifier
import com.github.tarcv.tongs.api.testcases.AnnotationInfo
import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonObject

class JsonInfoDecorder {
    internal fun decodeStructure(rawMessages: List<JsonObject>): List<TestInfo> {
        class RawItem(
                val uniqueId: Int,
                val semiUniqueId: Int,
                val readableName: String,
                var json: JsonObject? = null,
                val parent: RawItem? = null
        ) {
            val children: ArrayList<RawItem> = ArrayList()
        }

        fun buildPathFor(item: RawItem): List<String> {
            var parent: RawItem? = item
            val names = ArrayList<String>()
            while (parent != null) {
                names += parent.readableName
                parent = parent.parent
            }
            return names.reversed()
        }

        val unboundItems = ArrayList<RawItem>()
        return rawMessages
                .mapNotNull {
                    val partialItem = RawItem(
                            it.get("sId1").asInt,
                            it.get("sId2").asInt,
                            it.get("sName").asString
                    )

                    val unboundIndex = unboundItems.indexOfLast {unbound ->
                        partialItem.uniqueId == unbound.uniqueId ||
                                partialItem.semiUniqueId == unbound.semiUniqueId ||
                                partialItem.readableName == unbound.readableName
                    }
                    val actualItem = if (unboundIndex >= 0) {
                        val item = unboundItems.removeAt(unboundIndex)
                        item
                    } else {
                        partialItem
                    }

                    val children = it.get("sChildren").asJsonArray
                            .map {
                                val (uniqueId, semiUniqueId, readableName) =
                                        it.asString.split(Regex("(?<=\\d)-"), limit = 3) // an ID might be negative
                                RawItem(uniqueId.toInt(), semiUniqueId.toInt(), readableName, null, actualItem)
                            }
                    unboundItems.addAll(children)

                    actualItem.children.addAll(children)

                    if (it.has("testClass")) {
                        TestInfo(
                                TestIdentifier(it.get("testClass").asString, it.get("testMethod").asString),
                                it.get("testPackage").asString,
                                buildPathFor(actualItem),
                                deserializeAnnotations(it.get("annotations")?.asJsonArray)
                        )
                    } else {
                        null
                    }
                }
    }

    internal fun deserializeAnnotations(annotations: JsonArray?): List<AnnotationInfo> {
        return if (annotations != null) {
            val classNameKey = "annotationType"

            annotations.asJsonArray.map { annotationElement ->
                val annotation = annotationElement.asJsonObject
                val annotationType = annotation.get(classNameKey).asString
                val props = (convertToJava(annotation) as Map<String, Any?>)
                        .filter { it.key != classNameKey }
                AnnotationInfo(
                        annotationType,
                        props
                )
            }
        } else {
            emptyList<AnnotationInfo>()
        }
    }

    private fun convertToJava(value: JsonElement?): Any? {
        return when {
            value == null || value.isJsonNull -> {
                null
            }
            value.isJsonArray -> {
                value.asJsonArray
                        .map { convertToJava(it) }
                        .toList()
            }
            value.isJsonObject -> {
                value.asJsonObject
                        .entrySet()
                        .associateBy({ it.key }, { convertToJava(it.value) })
            }
            value.isJsonPrimitive -> {
                val primitive = value.asJsonPrimitive
                return if (primitive.isNumber) {
                    primitive.asNumber
                } else if (primitive.isString) {
                    primitive.asString
                } else {
                    throw IllegalStateException("Got unknown type of JSON Element: ${value}")
                }
            }
            else -> {
                throw IllegalStateException("Got unknown type of JSON Element: ${value}")
            }
        }
    }
}

class TestInfo(
        val identifier: TestIdentifier,
        val `package`: String,
        val readablePath: List<String>,
        val annotations: List<AnnotationInfo>
)
