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

package com.github.tarcv.tongs.summary;

import com.github.tarcv.tongs.model.Device;
import com.github.tarcv.tongs.model.Pool;

import javax.annotation.Nonnull;
import java.util.Collection;

import static java.util.Collections.emptyList;

public class FakeDeviceTestFilesRetriever implements DeviceTestFilesRetriever {
    private Collection<TestResult> testResults = emptyList();

    private FakeDeviceTestFilesRetriever() {
    }

    public static FakeDeviceTestFilesRetriever aFakeDeviceTestFilesRetriever() {
        return new FakeDeviceTestFilesRetriever();
    }

    public FakeDeviceTestFilesRetriever thatReturns(@Nonnull Collection<TestResult> testResults) {
        this.testResults = testResults;
        return this;
    }

    @Nonnull
    @Override
    public Collection<TestResult> getTestResultsForDevice(Pool pool, Device device) {
        return testResults;
    }
}
