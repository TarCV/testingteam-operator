/*
 * Copyright 2020 TarCV
 * Copyright 2014 Shazam Entertainment Limited
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License.
 *
 * You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package com.github.tarcv.tongs.runner

import com.android.ddmlib.DdmPreferences
import com.android.ddmlib.IDevice
import com.github.tarcv.tongs.model.AndroidDevice
import com.github.tarcv.tongs.system.adb.Installer
import com.github.tarcv.tongs.system.io.RemoteFileManager

import com.github.tarcv.tongs.device.clearLogcat
import com.github.tarcv.tongs.injector.system.InstallerInjector.installer
import com.github.tarcv.tongs.runner.rules.DeviceRule
import com.github.tarcv.tongs.runner.rules.DeviceRuleContext
import com.github.tarcv.tongs.runner.rules.DeviceRuleFactory

class AndroidSetupDeviceRuleFactory : DeviceRuleFactory<AndroidDevice, AndroidSetupDeviceRule> {
    override fun deviceRules(context: DeviceRuleContext<AndroidDevice>): Array<out AndroidSetupDeviceRule> {
        return arrayOf(
                AndroidSetupDeviceRule(context.device.deviceInterface, installer(context.configuration))
        )
    }
}

class AndroidSetupDeviceRule(private val deviceInterface: IDevice, private val installer: Installer) : DeviceRule {
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