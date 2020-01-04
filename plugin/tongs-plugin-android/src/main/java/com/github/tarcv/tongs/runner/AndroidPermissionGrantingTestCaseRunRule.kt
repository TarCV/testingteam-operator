/*
 * Copyright 2020 TarCV
 * Copyright 2016 Shazam Entertainment Limited
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

import com.android.ddmlib.IDevice
import com.github.tarcv.tongs.TongsConfiguration
import com.github.tarcv.tongs.model.AndroidDevice
import com.github.tarcv.tongs.runner.rules.TestCaseRunRule
import com.github.tarcv.tongs.runner.rules.TestCaseRunRuleContext
import com.github.tarcv.tongs.runner.rules.TestCaseRunRuleFactory
import com.github.tarcv.tongs.system.PermissionGrantingManager

class AndroidPermissionGrantingTestCaseRunRuleFactory : TestCaseRunRuleFactory<AndroidDevice, AndroidPermissionGrantingTestCaseRunRule> {
    override fun create(context: TestCaseRunRuleContext<AndroidDevice>): AndroidPermissionGrantingTestCaseRunRule {
        val permissionsToGrant = context.testCaseEvent.testCase.annotations
                .firstOrNull { it.fullyQualifiedName == "com.github.tarcv.tongs.GrantPermission" }
                .let {
                    if (it != null) {
                        it.properties["value"] as List<String>
                    } else {
                        emptyList()
                    }
                }
        return AndroidPermissionGrantingTestCaseRunRule(context.configuration, context.device.deviceInterface, permissionsToGrant)
    }
}

class AndroidPermissionGrantingTestCaseRunRule(
        private val configuration: TongsConfiguration,
        private val deviceInterface: IDevice,
        private val permissionsToGrant: List<String>
) : TestCaseRunRule {
    private val permissionGrantingManager = PermissionGrantingManager(configuration)

    override fun before() {
        permissionGrantingManager.grantPermissions(configuration.applicationPackage,
                deviceInterface, permissionsToGrant)
        permissionGrantingManager.grantPermissions(configuration.instrumentationPackage,
                deviceInterface, permissionsToGrant)
    }

    override fun after() {
        permissionGrantingManager.revokePermissions(configuration.applicationPackage,
                deviceInterface, permissionsToGrant)
    }
}
