/*
 * Copyright 2018 TarCV
 * Copyright 2016 Shazam Entertainment Limited
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License.
 *
 * You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */
package com.github.tarcv.tongs.injector.device;

import com.github.tarcv.tongs.device.DeviceLoader;

import static com.github.tarcv.tongs.injector.ConfigurationInjector.configuration;
import static com.github.tarcv.tongs.injector.device.DeviceGeometryRetrieverInjector.deviceGeometryReader;
import static com.github.tarcv.tongs.injector.system.AdbInjector.adb;

public class DeviceLoaderInjector {

    private DeviceLoaderInjector() {}

    public static DeviceLoader deviceLoader() {
        return new DeviceLoader(adb(), deviceGeometryReader(), configuration().getExcludedSerials());
    }
}
