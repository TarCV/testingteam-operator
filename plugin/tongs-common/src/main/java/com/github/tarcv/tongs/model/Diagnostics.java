/*
 * Copyright 2019 TarCV
 * Copyright 2015 Shazam Entertainment Limited
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License.
 *
 * You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */
package com.github.tarcv.tongs.model;

import com.android.ddmlib.IDevice;

import static com.android.ddmlib.IDevice.Feature.SCREEN_RECORD;

public enum Diagnostics {
    VIDEO,
    SCREENSHOTS,
    NONE;

    public static Diagnostics computeDiagnostics(IDevice deviceInterface, int apiLevel) {
        if (deviceInterface == null) {
            return NONE;
        }

        boolean supportsScreenRecord =
                deviceInterface.supportsFeature(SCREEN_RECORD) &&
                !"Genymotion".equals(deviceInterface.getProperty("ro.product.manufacturer"));
        if (supportsScreenRecord) {
            return VIDEO;
        }

        if (apiLevel >= 16) {
            return SCREENSHOTS;
        }

        return NONE;
    }
}
