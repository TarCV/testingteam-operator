package com.github.tarcv.tongs.injector.device

import com.github.tarcv.tongs.injector.ConfigurationInjector.configuration
import com.github.tarcv.tongs.injector.device.DeviceGeometryRetrieverInjector.deviceGeometryReader
import com.github.tarcv.tongs.system.adb.ConnectedDeviceProvider
import org.apache.commons.io.FileUtils

fun connectedDeviceProvider(): ConnectedDeviceProvider {
    return ConnectedDeviceProvider(
            deviceGeometryReader(),
            FileUtils.getFile(configuration().androidSdk, "platform-tools", "adb")
    )
}