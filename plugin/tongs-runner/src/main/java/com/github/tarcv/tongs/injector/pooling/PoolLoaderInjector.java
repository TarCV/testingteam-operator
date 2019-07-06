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

package com.github.tarcv.tongs.injector.pooling;

import com.github.tarcv.tongs.Configuration;
import com.github.tarcv.tongs.injector.device.ConnectedDeviceProviderInjector;
import com.github.tarcv.tongs.pooling.PoolLoader;

import static com.github.tarcv.tongs.TongsConfiguration.TongsIntegrationTestRunType.STUB_PARALLEL_TESTRUN;
import static com.github.tarcv.tongs.injector.ConfigurationInjector.configuration;

public class PoolLoaderInjector {

    private PoolLoaderInjector() {}

    public static PoolLoader poolLoader() {
        Configuration configuration = configuration();
        if (configuration.getTongsIntegrationTestRunType() == STUB_PARALLEL_TESTRUN) {
            return new PoolLoader(null, configuration); // TODO: replace null with stub ConnectedDeviceProvider
        } else {
            return new PoolLoader(ConnectedDeviceProviderInjector.INSTANCE.connectedDeviceProvider(), configuration);
        }
    }
}
