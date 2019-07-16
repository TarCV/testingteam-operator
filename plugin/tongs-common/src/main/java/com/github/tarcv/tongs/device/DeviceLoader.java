/*
 * Copyright 2019 TarCV
 * Copyright 2016 Shazam Entertainment Limited
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License.
 *
 * You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */
package com.github.tarcv.tongs.device;

import com.android.ddmlib.IDevice;
import com.github.tarcv.tongs.model.Device;

import static com.github.tarcv.tongs.model.Device.Builder.aDevice;

/**
 * Turns a serial number or an IDevice reference to a Device.
 */
public class DeviceLoader {
    public static Device loadDeviceCharacteristics(IDevice device, DeviceGeometryRetriever deviceGeometryRetriever) {
        return aDevice()
                .withSerial(device.getSerialNumber())
                .withManufacturer(device.getProperty("ro.product.manufacturer"))
                .withModel(device.getProperty("ro.product.model"))
                .withApiLevel(device.getProperty("ro.build.version.sdk"))
                .withDeviceInterface(device)
                .withTabletCharacteristic(device.getProperty("ro.build.characteristics"))
                .withDisplayGeometry(deviceGeometryRetriever.detectGeometry(device)).build();
    }
}
