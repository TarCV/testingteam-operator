/*
 * Copyright 2018 TarCV
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
import com.github.tarcv.tongs.model.DisplayGeometry;
import com.github.tarcv.tongs.model.Pool;
import com.github.tarcv.tongs.system.adb.ConnectedDeviceProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static com.github.tarcv.tongs.TongsConfiguration.TongsIntegrationTestRunType.STUB_PARALLEL_TESTRUN;
import static com.github.tarcv.tongs.runner.TestAndroidTestRunnerFactory.functionalTestTestcaseDuration;
import static java.lang.String.format;

public class PoolLoader {
    private static final Logger logger = LoggerFactory.getLogger(PoolLoader.class);
    private final ConnectedDeviceProvider deviceLoader;
    private final Configuration configuration;

    public PoolLoader(ConnectedDeviceProvider deviceLoader, Configuration configuration) {
        this.deviceLoader = deviceLoader;
        this.configuration = configuration;
    }

    public Collection<Pool> loadPools() throws NoDevicesForPoolException, NoPoolLoaderConfiguredException {
        if (configuration.getTongsIntegrationTestRunType() == STUB_PARALLEL_TESTRUN) {
            Device device1 = createStubDevice("tongs-5554", 25);
            Device device2 = createStubDevice("tongs-5556", 25);

            Pool pool = new Pool.Builder()
                    .withName("Stub 2 device pool")
                    .addDevice(device1)
                    .addDevice(device2)
                    .build();

            return Collections.singletonList(pool);
        } else {
            List<Device> devices;

            deviceLoader.init();
            devices = deviceLoader.getDevices().stream()
                    .filter(d -> !configuration.getExcludedSerials().contains(d.getSerial()))
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
    }

    private Device createStubDevice(String serial, int api) {
        String manufacturer = "tongs";
        String model = "Emu-" + api;
        StubDevice stubDevice = new StubDevice(serial, manufacturer, model, serial, api, "",
                functionalTestTestcaseDuration);
        return new Device.Builder()
                .withApiLevel(String.valueOf(api))
                .withDisplayGeometry(new DisplayGeometry(640))
                .withManufacturer(manufacturer)
                .withModel(model)
                .withSerial(serial)
                .withDeviceInterface(stubDevice)
                .build();
    }

    private void log(Collection<Pool> configuredPools) {
        logger.info("Number of device pools: " + configuredPools.size());
        for (Pool pool : configuredPools) {
            logger.debug(pool.toString());
        }
    }

    private DevicePoolLoader pickPoolLoader(Configuration configuration) throws NoPoolLoaderConfiguredException {
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
