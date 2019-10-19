/*
 * Copyright 2019 TarCV
 * Copyright 2018 Shazam Entertainment Limited
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License.
 *
 * You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */

package com.github.tarcv.tongs.model

import com.google.common.base.Objects
import com.google.gson.JsonObject
import java.util.*
import java.util.Collections.emptyList

class TestCaseEvent private constructor(
        val testCase: TestCase,
        val permissionsToGrant: List<String>,
        excludedDevices: Collection<Device>) {

    val testMethod: String
        get() = testCase.testMethod
    val testClass: String
        get() = testCase.testClass
    val properties: Map<String, String>
        get() = testCase.properties

    private val excludedDevices: HashSet<Device>

    init {
        this.excludedDevices = HashSet(excludedDevices)
    }

    fun isExcluded(device: Device): Boolean {
        return excludedDevices.contains(device)
    }

    override fun hashCode(): Int {
        return Objects.hashCode(this.testMethod, this.testClass)
    }

    override fun equals(obj: Any?): Boolean {
        if (obj == null) {
            return false
        }
        if (javaClass != obj.javaClass) {
            return false
        }
        val other = obj as TestCaseEvent?
        return Objects.equal(this.testMethod, other!!.testMethod) && Objects.equal(this.testClass, other.testClass)
    }

    override fun toString(): String {
        return this.testClass + "#" + this.testMethod
    }

    companion object {

        fun newTestCase(testMethod: String, testClass: String, permissionsToGrant: List<String>, properties: Map<String, String>, info: JsonObject?, excludedDevices: Collection<Device>): TestCaseEvent {
            val testCase = TestCase(testMethod, testClass, properties)
            return TestCaseEvent(testCase, permissionsToGrant, excludedDevices)
        }

        @JvmStatic
        fun newTestCase(testIdentifier: TestCase): TestCaseEvent {
            return TestCaseEvent(testIdentifier, emptyList(), emptyList())
        }
    }
}
