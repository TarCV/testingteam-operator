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

import groovy.lang.Closure

// Gradle requires this class to be open
internal open class TongsConfigurationGradleExtension : TongsConfigurationExtension() {
    // TODO: Move all Gradle task default values to this class or its superclass

    /**
     * The strategy that will be used to calculate the grouping of devices to pools.
     */
    lateinit var poolingStrategy: GradlePoolingStrategy

    fun poolingStrategy(poolingStrategyClosure: Closure<*>) {
        poolingStrategy = GradlePoolingStrategy()
        poolingStrategyClosure.delegate = poolingStrategy
        poolingStrategyClosure.call()
    }
}