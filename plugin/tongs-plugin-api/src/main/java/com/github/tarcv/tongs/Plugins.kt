/*
 * Copyright 2020 TarCV
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License.
 *
 * You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */

package com.github.tarcv.tongs

class Plugins(
    /**
     * DeviceProvider classes that should be loaded and used during test runs
     */
    var deviceProviders: Collection<String> = emptyList(),

    /**
     * Rules that are executed before and after a test case execution
     */
    var runRules: Collection<String> = emptyList(),

    /**
     * Rules that modify test cases loading
     */
    var testCaseRules: Collection<String> = emptyList()
)