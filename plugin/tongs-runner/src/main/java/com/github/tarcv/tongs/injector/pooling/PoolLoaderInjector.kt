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

import com.github.tarcv.tongs.injector.ConfigurationInjector.configuration
import com.github.tarcv.tongs.injector.RuleManagerFactory
import com.github.tarcv.tongs.plugin.DeviceProvider
import com.github.tarcv.tongs.plugin.DeviceProviderContext
import com.github.tarcv.tongs.plugin.DeviceProviderFactory
import com.github.tarcv.tongs.plugin.android.LocalDeviceProviderFactory
import com.github.tarcv.tongs.pooling.PoolLoader

object PoolLoaderInjector {

    @JvmStatic
    fun poolLoader(ruleManagerFactory: RuleManagerFactory): PoolLoader {
        val loaders = createProviders(ruleManagerFactory)
        return PoolLoader(configuration(), loaders)
    }

}

private fun createProviders(ruleManagerFactory: RuleManagerFactory): DeviceProviderManager {
    val defaultProviderFactories: List<DeviceProviderFactory<DeviceProvider>> = listOf(
            LocalDeviceProviderFactory()
    )
    return ruleManagerFactory.create(
            DeviceProviderFactory::class.java,
            defaultProviderFactories,
            { factory, context: DeviceProviderContext -> factory.deviceProviders(context) }
    )
}

typealias DeviceProviderManager = RuleManagerFactory.RuleManager<
        DeviceProviderContext, DeviceProvider, DeviceProviderFactory<DeviceProvider>>