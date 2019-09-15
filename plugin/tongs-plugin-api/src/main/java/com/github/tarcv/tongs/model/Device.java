/*
 * Copyright 2019 TarCV
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
package com.github.tarcv.tongs.model;

import javax.annotation.Nullable;
import java.util.Objects;

public abstract class Device {
    private String nameSuffix;

    public abstract String getSerial();

    public String getSafeSerial() {
        return getSerial().replaceAll(":", "-");
    }

    public abstract String getManufacturer();

    public abstract String getModelName();

    public abstract int getOsApiLevel();

    public abstract String getLongName();

    public abstract Object getDeviceInterface();

    public abstract boolean isTablet();

    @Nullable
public abstract DisplayGeometry getGeometry();

    public abstract Diagnostics getSupportedVisualDiagnostics();

    public String getName() {
        return getModelName() + nameSuffix;
    }

    public void setNameSuffix(String suffix) {
        nameSuffix = suffix;
    }

    protected abstract Object getUniqueIdentifier();

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Device)) return false;
        Device device = (Device) o;
        return getUniqueIdentifier().equals(device.getUniqueIdentifier());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getUniqueIdentifier());
    }
}
