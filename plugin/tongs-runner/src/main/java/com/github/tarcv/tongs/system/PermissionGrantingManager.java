/*
 * Copyright 2018 TarCV
 * Copyright 2018 Shazam Entertainment Limited
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License.
 *
 * You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */

package com.github.tarcv.tongs.system;

import com.android.ddmlib.AdbCommandRejectedException;
import com.android.ddmlib.IDevice;
import com.android.ddmlib.NullOutputReceiver;
import com.android.ddmlib.ShellCommandUnresponsiveException;
import com.android.ddmlib.TimeoutException;
import com.github.tarcv.tongs.Configuration;
import com.github.tarcv.tongs.model.Permission;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nonnull;

import static com.github.tarcv.tongs.injector.ConfigurationInjector.configuration;
import static java.lang.String.format;

public class PermissionGrantingManager {

    private static final NullOutputReceiver NO_OP_RECEIVER = new NullOutputReceiver();

    private final Logger logger = LoggerFactory.getLogger(PermissionGrantingManager.class);
    private final Configuration configuration;

    private PermissionGrantingManager(@Nonnull Configuration configuration) {
        this.configuration = configuration;
    }

    public static PermissionGrantingManager permissionGrantingManager() {
        return new PermissionGrantingManager(configuration());
    }

    public void revokePermissions(@Nonnull String applicationPackage,
                                  @Nonnull IDevice device,
                                  @Nonnull List<String> permissionsToRevoke) {
        if (!permissionsToRevoke.isEmpty()) {
            long start = System.currentTimeMillis();
            for (String permissionToRevoke : permissionsToRevoke) {
                try {
                    device.executeShellCommand(format("pm revoke %s %s",
                            applicationPackage, permissionToRevoke), NO_OP_RECEIVER);
                } catch (TimeoutException | AdbCommandRejectedException | ShellCommandUnresponsiveException | IOException e) {
                    throw new UnsupportedOperationException(format("Unable to revoke permission %s", permissionToRevoke), e);
                }
            }

            logger.debug("Revoking permissions: {} (took {}ms)", permissionsToRevoke, (System.currentTimeMillis() - start));
        }
    }

    public void restorePermissions(@Nonnull String applicationPackage,
                                   @Nonnull IDevice device,
                                   @Nonnull List<String> permissionsToRestore) {
        if (!permissionsToRestore.isEmpty() && configuration.isAutoGrantingPermissions()) {
            List<String> permissionsToGrant = new ArrayList<>(permissionsToRestore.size());

            List<Permission> permissions = configuration.getApplicationInfo().getPermissions();
            for (Permission permission : permissions) {
                if (deviceApiLevelInRange(device, permission) && permissionsToRestore.contains(permission.getPermissionName())) {
                    permissionsToGrant.add(permission.getPermissionName());
                }
            }

            grantPermissions(applicationPackage, device, permissionsToGrant);
        }
    }

    private void grantPermissions(@Nonnull String applicationPackage,
                                  @Nonnull IDevice device,
                                  @Nonnull List<String> permissionsToGrant) {
        if (!permissionsToGrant.isEmpty()) {
            long start = System.currentTimeMillis();
            for (String permissionToGrant : permissionsToGrant) {
                try {
                    device.executeShellCommand(format("pm grant %s %s",
                            applicationPackage, permissionToGrant), NO_OP_RECEIVER);
                } catch (TimeoutException | AdbCommandRejectedException | ShellCommandUnresponsiveException | IOException e) {
                    throw new UnsupportedOperationException(format("Unable to grant permission %s", permissionToGrant), e);
                }
            }

            logger.debug("Granting permissions: {} (took {}ms)", permissionsToGrant, (System.currentTimeMillis() - start));
        }
    }

    private static boolean deviceApiLevelInRange(@Nonnull IDevice device, @Nonnull Permission permission) {
        int featureLevel = device.getVersion().getFeatureLevel();
        return permission.getMaxSdkVersion() >= featureLevel && permission.getMinSdkVersion() <= featureLevel;
    }

}
