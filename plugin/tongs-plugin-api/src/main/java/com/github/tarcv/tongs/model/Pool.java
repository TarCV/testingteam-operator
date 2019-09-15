/*
 * Copyright 2018 TarCV
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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.apache.commons.lang3.builder.ToStringBuilder.reflectionToString;
import static org.apache.commons.lang3.builder.ToStringStyle.MULTI_LINE_STYLE;

/**
 * A grouping of {@link com.github.tarcv.tongs.model.Device}s.
 */
public class Pool {
	private final String name;
	private final List<Device> devices;

	public String getName() {
		return name;
	}

	public List<Device> getDevices() {
		return devices;
	}

	public int size() {
		return devices.size();
	}

	public boolean isEmpty() {
		return devices.isEmpty();
	}

	@Override
	public String toString() {
        return reflectionToString(this, MULTI_LINE_STYLE);
	}

	public static class Builder {
		private String name = "";
		private final List<Device> devices = new ArrayList<>();

		public static Builder aDevicePool() {
			return new Builder();
		}

		public Builder withName(String name) {
			this.name = name;
			return this;
		}

		public Builder addDevice(Device device) {
			devices.add(device);
			return this;
		}

		public Pool build() {
			checkNotNull(name, "Pool name cannot be null");
			return new Pool(this);
		}

		public void addIfNotEmpty(Collection<Pool> pools) {
			Pool pool = build();
			if (!pool.isEmpty()) {
				pools.add(pool);
			}
		}
	}

	private Pool(Builder builder) {
		name = builder.name;
		devices = builder.devices;
	}
}
