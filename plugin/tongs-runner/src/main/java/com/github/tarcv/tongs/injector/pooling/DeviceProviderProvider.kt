/*
 * Copyright 2019 TarCV
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License.
 *
 * You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */

package com.github.tarcv.tongs.injector.pooling

import com.github.tarcv.tongs.Configuration
import com.github.tarcv.tongs.TongsConfiguration
import com.github.tarcv.tongs.plugin.DeviceProvider
import com.github.tarcv.tongs.plugin.DeviceProviderContext
import com.github.tarcv.tongs.plugin.DeviceProviderContextImpl
import com.github.tarcv.tongs.plugin.android.LocalDeviceProvider

import java.lang.reflect.Constructor
import java.lang.reflect.InvocationTargetException
import java.util.ArrayList
import java.util.stream.Collectors

class DeviceProviderProvider internal constructor(private val configuration: Configuration) {

    fun createProviders(): List<DeviceProvider> {
        val deviceProviders = ArrayList<DeviceProvider>()

        addDefaultProviders(deviceProviders)
        addPluginProviders(deviceProviders)

        return deviceProviders
    }

    private fun addPluginProviders(deviceProviders: MutableList<DeviceProvider>) {
        configuration.plugins.deviceProviders.asSequence()
                .map { className -> Class.forName(className) }
                .map { clazz ->
                    try {
                        val deviceProviderContext = DeviceProviderContextImpl(configuration)

                        val ctor = clazz.getConstructor(DeviceProviderContext::class.java)
                        ctor.newInstance(deviceProviderContext) as DeviceProvider
                    } catch (e: InvocationTargetException) {
                        throw RuntimeException(e.targetException) //TODO
                    }
                }
                .toCollection(deviceProviders)
    }

    private fun addDefaultProviders(deviceProviders: MutableList<DeviceProvider>) {
        deviceProviders.add(LocalDeviceProvider(DeviceProviderContextImpl(configuration)))
    }
}
