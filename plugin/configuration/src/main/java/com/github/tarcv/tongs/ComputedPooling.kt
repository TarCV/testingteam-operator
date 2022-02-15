/*
 * Copyright 2022 TarCV
 * Copyright 2016 Shazam Entertainment Limited
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License.
 *
 * You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */
package com.github.tarcv.tongs

import com.github.tarcv.tongs.api.devices.Device

open class ComputedPooling(
    var characteristic: Characteristic?,
    var groups: Map<String, Int>
) {
    enum class Characteristic : DeviceCharacteristicReader {
        sw {
            override fun canPool(device: Device): Boolean {
                return device.geometry != null
            }

            override fun getParameter(device: Device): Int {
                val geometry = device.geometry
                return geometry?.swDp ?: 0
            }

            override fun getBaseName(): String {
                return name
            }
        },
        api {
            override fun canPool(device: Device): Boolean {
                return true
            }

            override fun getParameter(device: Device): Int {
                return device.osApiLevel
            }

            override fun getBaseName(): String {
                return name
            }
        }
    }
}