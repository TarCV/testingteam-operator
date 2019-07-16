package com.github.tarcv.tongs.injector.device

import com.github.tarcv.tongs.injector.ConfigurationInjector.configuration
import com.github.tarcv.tongs.injector.device.DeviceGeometryRetrieverInjector.deviceGeometryReader
import com.github.tarcv.tongs.system.adb.ConnectedDeviceProvider
import org.apache.commons.io.FileUtils

// TODO: make not singleton once Device has proper equals and hashCode implementations
// For now .init() should be called only once to make sure reference comparison returns true for same devices
object ConnectedDeviceProviderInjector {
    private val PROVIDER = createDeviceProvider()

    private fun createDeviceProvider(): ConnectedDeviceProvider {
        val connectedDeviceProvider = ConnectedDeviceProvider(
                deviceGeometryReader(),
                FileUtils.getFile(configuration().androidSdk, "platform-tools", "adb")
        )
        connectedDeviceProvider.init()
        return connectedDeviceProvider
    }

    fun connectedDeviceProvider(): ConnectedDeviceProvider {
        return PROVIDER
    }
}
