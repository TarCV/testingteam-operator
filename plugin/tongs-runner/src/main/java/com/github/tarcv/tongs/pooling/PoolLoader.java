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

package com.github.tarcv.tongs.pooling;

import com.github.tarcv.tongs.Configuration;
import com.github.tarcv.tongs.PoolingStrategy;
import com.github.tarcv.tongs.model.Device;
import com.github.tarcv.tongs.model.Pool;
import com.github.tarcv.tongs.plugin.DeviceProvider;
import com.github.tarcv.tongs.plugin.DeviceProviderContextImpl;
import com.github.tarcv.tongs.plugin.android.LocalDeviceProvider;
import com.github.tarcv.tongs.plugin.android.StubDeviceProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;

import static com.github.tarcv.tongs.TongsConfiguration.TongsIntegrationTestRunType.STUB_PARALLEL_TESTRUN;
import static java.lang.String.format;

public class PoolLoader {
    private static final Logger logger = LoggerFactory.getLogger(PoolLoader.class);
    private final Configuration configuration;
    private final List<DeviceProvider> deviceProviders;

    public PoolLoader(Configuration configuration, List<DeviceProvider> deviceProviders) {
        this.configuration = configuration;
        this.deviceProviders = deviceProviders;
    }

    public Collection<Pool> loadPools() throws NoDevicesForPoolException, NoPoolLoaderConfiguredException {
        List<Device> devices = deviceProviders.stream()
                .map(deviceProvider -> {
                    ArrayList<Device> deviceList = new ArrayList<>(deviceProvider.provideDevices());
                    logger.info("Got {} devices from {}", deviceList.size(), deviceProvider.getClass().getSimpleName());
                    return deviceList;
                })
                .flatMap(deviceList -> deviceList.stream())
                .collect(Collectors.toList());

        if (devices.isEmpty()) {
            throw new NoDevicesForPoolException("No devices found.");
        }

        DevicePoolLoader devicePoolLoader = pickPoolLoader(configuration);
        logger.info("Picked {}", devicePoolLoader.getClass().getSimpleName());
        Collection<Pool> pools = devicePoolLoader.loadPools(devices);
        if (pools.isEmpty()) {
            throw new IllegalArgumentException("No pools were found with your configuration. Please review connected devices");
        }
        log(pools);
        for (Pool pool : pools) {
            if (pool.isEmpty()) {
                throw new NoDevicesForPoolException(format("Pool %s is empty", pool.getName()));
            }
        }

            return pools;
    }

    private static void log(Collection<Pool> configuredPools) {
        logger.info("Number of device pools: " + configuredPools.size());
        for (Pool pool : configuredPools) {
            logger.debug(pool.toString());
        }
    }

    private static DevicePoolLoader pickPoolLoader(Configuration configuration) throws NoPoolLoaderConfiguredException {
        PoolingStrategy poolingStrategy = configuration.getPoolingStrategy();

        if (poolingStrategy.manual != null) {
            return new SerialBasedDevicePoolLoader(poolingStrategy.manual);
        }

        if (poolingStrategy.splitTablets != null && poolingStrategy.splitTablets) {
            return new DefaultAndTabletDevicePoolLoader();
        }

        if (poolingStrategy.computed != null) {
            return new ComputedDevicePoolLoader(poolingStrategy.computed);
        }

        if (poolingStrategy.eachDevice != null && poolingStrategy.eachDevice) {
            return new EveryoneGetsAPoolLoader();
        }

        throw new NoPoolLoaderConfiguredException("Could not determine which how to load pools to use based on your configuration");
    }
}
