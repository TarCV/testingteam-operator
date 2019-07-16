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

import com.android.ddmlib.IDevice;

import javax.annotation.Nullable;

import java.util.Objects;

import static com.google.common.base.Strings.isNullOrEmpty;

/**
 * Representation of a device and its details.
 */
public class Device {
	private final String serial;
	private final String manufacturer;
	private final String model;
	private final String apiLevel;
	private final transient IDevice deviceInterface;
	private final boolean isTablet;
	private final DisplayGeometry geometry;
    private final Diagnostics diagnostics;
	private String nameSuffix;

	public String getSerial() {
		return serial;
	}

	public String getSafeSerial() {
		return serial.replaceAll(":", "-");
	}

	public String getManufacturer() {
		return manufacturer;
	}

	public String getModelName() {
		return model;
	}

	public String getApiLevel() {
		return apiLevel;
	}

	public String getLongName() {
		return serial + " (" + model + ")";
	}

	public IDevice getDeviceInterface() {
		return deviceInterface;
	}

	public boolean isTablet() {
		return isTablet;
	}

	@Nullable
    public DisplayGeometry getGeometry() {
        return geometry;
    }

    public Diagnostics getSupportedDiagnostics() {
        return diagnostics;
    }

	public String getName() {
		return model + nameSuffix;
	}

	public void setNameSuffix(String suffix) {
		nameSuffix = suffix;
	}

	/**
	 * Returns an object that uniquely identify underlying device and which has equals and hashCode implementations
	 *  that are reproducible when a new instance of Device is created from the same device
	 *
	 * @return object uniquely identifying underlying device
	 */
	private Object getUniqueIdentifier() {
		return serial;
	}

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

	public static class Builder {
        private String serial = "Unspecified serial";
        private String manufacturer = "Unspecified manufacturer";
        private String model = "Unspecified model";
        private String apiLevel;
		private IDevice deviceInterface;
		private boolean isTablet = false;
		private DisplayGeometry geometry;

        public static Builder aDevice() {
			return new Builder();
		}

		public Builder withSerial(String serial) {
			this.serial = serial;
			return this;
		}

		public Builder withManufacturer(String manufacturer) {
			if (!isNullOrEmpty(manufacturer)) {
				this.manufacturer = manufacturer;
			}
			return this;
		}

		public Builder withModel(String model) {
			if (!isNullOrEmpty(model)) {
				this.model = model;
			}
			return this;
		}

		public Builder withApiLevel(String apiLevel) {
			if (!isNullOrEmpty(apiLevel)) {
				this.apiLevel = apiLevel;
			}
			return this;
		}

		public Builder withDeviceInterface(IDevice deviceInterface) {
			this.deviceInterface = deviceInterface;
			return this;
		}

        /**
         * Tablets seem to have property [ro.build.characteristics = tablet], but not all tablets respect that.
         * @param characteristics the characteristics field as reported by the device
         * @return this builder
         */
		public Builder withTabletCharacteristic(String characteristics) {
			if (!isNullOrEmpty(characteristics) && characteristics.contains("tablet")) {
				isTablet = true;
			}
			return this;
		}

		public Builder withDisplayGeometry(@Nullable DisplayGeometry geometry) {
			this.geometry = geometry;
			return this;
		}

		public Device build() {
			return new Device(this);
		}
	}

	private Device(Builder builder) {
		serial = builder.serial;
		manufacturer = builder.manufacturer;
		model = builder.model;
		apiLevel = builder.apiLevel;
		deviceInterface = builder.deviceInterface;
		isTablet = builder.isTablet;
		geometry = builder.geometry;
        diagnostics = Diagnostics.computeDiagnostics(deviceInterface, apiLevel);
	}
}
