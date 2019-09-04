/*
 * Copyright 2019 TarCV
 * Copyright 2018 Shazam Entertainment Limited
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License.
 *
 * You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */

package com.github.tarcv.tongs.device;

import com.github.tarcv.tongs.model.Device;
import com.github.tarcv.tongs.model.Pool;
import com.github.tarcv.tongs.model.TestCaseEvent;
import com.github.tarcv.tongs.system.io.FileManager;
import com.github.tarcv.tongs.system.io.FileType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;

public class DeviceTestFilesCleanerImpl implements DeviceTestFilesCleaner {
    private static final Logger logger = LoggerFactory.getLogger(DeviceTestFilesCleanerImpl.class);
    private final FileManager fileManager;
    private final Pool pool;
    private final Device device;

    public DeviceTestFilesCleanerImpl(FileManager fileManager, Pool pool, Device device) {
        this.fileManager = fileManager;
        this.pool = pool;
        this.device = device;
    }

    @Override
    public boolean deleteTraceFiles(TestCaseEvent testIdentifier) {
        File file = fileManager.getFile(FileType.TEST, pool.getName(), device.getSafeSerial(), testIdentifier);
        boolean isDeleted = file.delete();
        if (!isDeleted) {
            logger.warn("Failed to delete a file %s", file.getAbsolutePath());
        }
        return isDeleted;
    }
}
