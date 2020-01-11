/*
 * Copyright 2020 TarCV
 * Copyright 2015 Shazam Entertainment Limited
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License.
 *
 * You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */

package com.github.tarcv.tongs.injector.pooling

import com.github.tarcv.tongs.Configuration
import com.github.tarcv.tongs.injector.BaseRuleManager
import com.github.tarcv.tongs.injector.ConfigurationInjector.configuration
import com.github.tarcv.tongs.plugin.DeviceProvider
import com.github.tarcv.tongs.plugin.DeviceProviderContext
import com.github.tarcv.tongs.plugin.DeviceProviderFactory
import com.github.tarcv.tongs.plugin.android.LocalDeviceProviderFactory
import com.github.tarcv.tongs.pooling.PoolLoader

object PoolLoaderInjector {

    @JvmStatic
    fun poolLoader(): PoolLoader {
        val configuration = configuration()

        val loaders = createProviders(configuration)

        return PoolLoader(configuration, loaders)
    }

}

private fun createProviders(configuration: Configuration): DeviceProviderManager {
    val defaultProviderFactories: List<DeviceProviderFactory<DeviceProvider>> = listOf(
            LocalDeviceProviderFactory()
    )
    return DeviceProviderManager(configuration, configuration.plugins.deviceProviders, defaultProviderFactories)
}

class DeviceProviderManager(
        private val configuration: Configuration,
        private val ruleNames: Collection<String>,
        private val predefinedFactories: Collection<DeviceProviderFactory<DeviceProvider>>
): BaseRuleManager<DeviceProviderContext, DeviceProvider, DeviceProviderFactory<DeviceProvider>>(
        ruleNames,
        predefinedFactories,
        { factory, context -> factory.deviceProviders(context) }
)
