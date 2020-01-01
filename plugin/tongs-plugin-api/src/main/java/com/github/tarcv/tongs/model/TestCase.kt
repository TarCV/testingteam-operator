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
package com.github.tarcv.tongs.model

import java.util.Collections.emptyMap

data class TestCase @JvmOverloads constructor(
        val testMethod: String,
        val testClass: String,
        val properties: Map<String, String> = emptyMap(),
        val annotations: List<AnnotationInfo> = emptyList()
) {
    /**
     * Returns a readable string uniquely identifying a test case for use in logs and file names.
     * In current implementation it consists of the testMethod name and the name of the test class
     */
    override fun toString(): String = "$testClass#$testMethod"
}
