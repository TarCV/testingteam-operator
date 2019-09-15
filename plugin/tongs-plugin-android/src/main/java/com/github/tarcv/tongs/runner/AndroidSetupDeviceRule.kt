package com.github.tarcv.tongs.runner

import com.android.ddmlib.DdmPreferences
import com.android.ddmlib.IDevice
import com.github.tarcv.tongs.model.AndroidDevice
import com.github.tarcv.tongs.system.adb.Installer
import com.github.tarcv.tongs.system.io.RemoteFileManager

import com.github.tarcv.tongs.device.clearLogcat
import com.github.tarcv.tongs.injector.system.InstallerInjector.installer

class AndroidSetupDeviceRuleFactory : DeviceRuleFactory<AndroidDevice, AndroidSetupDeviceRule> {
    override fun create(context: TongsDeviceContext<AndroidDevice>): AndroidSetupDeviceRule {
        return AndroidSetupDeviceRule(context.device.deviceInterface, installer(context.configuration))
    }
}

class AndroidSetupDeviceRule(private val deviceInterface: IDevice, private val installer: Installer) : DeviceRule<AndroidDevice> {
    override fun before() {
        DdmPreferences.setTimeOut(30000)
        installer.prepareInstallation(deviceInterface)
        // For when previous run crashed/disconnected and left files behind
        RemoteFileManager.removeRemoteDirectory(deviceInterface)
        RemoteFileManager.createRemoteDirectory(deviceInterface)
        RemoteFileManager.createCoverageDirectory(deviceInterface)
        clearLogcat(deviceInterface)
    }

    override fun after() {
        // no-op
    }
}