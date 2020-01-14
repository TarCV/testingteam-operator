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
package com.github.tarcv.tongs.runner.rules

import com.github.tarcv.tongs.TongsConfiguration
import com.github.tarcv.tongs.model.Device
import com.github.tarcv.tongs.model.Pool
import com.github.tarcv.tongs.system.io.TestCaseFileManager

class DeviceRuleContext<out T: Device>(
        val configuration: TongsConfiguration,
        val fileManager: TestCaseFileManager,
        val pool: Pool,
        val device: T
)

interface DeviceRuleFactory<D: Device, out T: DeviceRule> {
    fun deviceRules(context: DeviceRuleContext<D>): Array<out T>
}

interface DeviceRule {
    fun before()
    fun after()
}
