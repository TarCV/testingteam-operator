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
package com.github.tarcv.tongs.api.testcases

import java.util.Collections.emptyMap

class TestCase @JvmOverloads constructor( // TODO: consider splitting into TestIdentifier and TestCase classes
        val typeTag: Class<*>,
        val testMethod: String,
        val testClass: String, // TODO: consider adding 'variation' property
        val properties: Map<String, String> = emptyMap(), // TODO: consider moving to TestCaseEvent, consider changing key type to Enum or Class
        val annotations: List<AnnotationInfo> = emptyList(), // TODO: remove from comparison, consider replacing with properties
        val extra: Any = Any()
        // TODO: add detectedOnDevices property excluded from comparison
) {


    /**
     * Returns a readable string uniquely identifying a test case for use in logs and file names.
     * In current implementation it consists of the testMethod name and the name of the test class
     */
    // TODO: update to include typeTag
    override fun toString(): String = "$testClass#$testMethod"

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as TestCase

        if (testMethod != other.testMethod) return false
        if (testClass != other.testClass) return false
        if (properties != other.properties) return false
        if (annotations != other.annotations) return false
        if (typeTag != other.typeTag) return false

        return true
    }

    override fun hashCode(): Int {
        var result = testMethod.hashCode()
        result = 31 * result + testClass.hashCode()
        result = 31 * result + properties.hashCode()
        result = 31 * result + annotations.hashCode()
        result = 31 * result + typeTag.hashCode()
        return result
    }
}
