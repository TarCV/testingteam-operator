/*
 * Copyright 2020 TarCV
 * Copyright 2018 Shazam Entertainment Limited
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License.
 *
 * You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */
package com.github.tarcv.tongs.api.run

import com.github.tarcv.tongs.api.devices.Device
import com.github.tarcv.tongs.api.testcases.TestCase

fun TestCaseEvent.Companion.aTestCaseEvent(testIdentifier: TestCase): TestCaseEvent {
    return TestCaseEvent(testIdentifier, emptyList())
}

fun aTestCaseEvent(testIdentifier: TestCase): TestCaseEvent = TestCaseEvent.Companion.aTestCaseEvent(testIdentifier)

fun aTestEvent(testIdentifier: TestCase, excludes: List<Device>, failureCount: Int): TestCaseEvent {
    return TestCaseEvent(testIdentifier, excludes, failureCount)
}
fun TestCaseEvent.Companion.aTestEvent(testIdentifier: TestCase, excludes: List<Device>, failureCount: Int): TestCaseEvent {
    return TestCaseEvent(testIdentifier, excludes, failureCount)
}