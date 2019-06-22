/*
 * Copyright 2019 TarCV
 * Copyright (C) 2013 The Android Open Source Project
 *
 * Based on com/android/builder/testing/ConnectedDeviceProvider.java from Android Gradle Plugin 3.3.2 source code
 *
 * The "NOTICE" text file relevant to this source file and this source file only is
 * com/github/tarcv/tongs/system/adb/ConnectedDeviceProvider-NOTICE.txt
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.github.tarcv.tongs.system.adb;

import com.android.ddmlib.AndroidDebugBridge;
import com.android.ddmlib.DdmPreferences;
import com.android.ddmlib.IDevice;
import com.android.ddmlib.Log;
import com.github.tarcv.tongs.device.DeviceGeometryRetriever;
import com.github.tarcv.tongs.device.DeviceLoader;
import com.github.tarcv.tongs.model.Device;
import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.io.File;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * DeviceProvider for locally connected devices. Basically returns the list of devices that
 * are currently connected at the time {@link #init()} is called.
 */
public class ConnectedDeviceProvider {

    private final File adbLocation;

    private static final Logger logger = LoggerFactory.getLogger(ConnectedDeviceProvider.class);

    private final DeviceGeometryRetriever deviceGeometryRetriever;

    private final List<Device> localDevices = Lists.newArrayList();

    @Nullable
    private LogAdapter logAdapter;

    public ConnectedDeviceProvider(DeviceGeometryRetriever deviceGeometryRetriever, File adbLocation) {
        this.adbLocation = adbLocation;
        this.deviceGeometryRetriever = deviceGeometryRetriever;
    }

    public List<Device> getDevices() {
        return localDevices;
    }

    public void init() {
        int timeOut = 30000;
        DdmPreferences.setTimeOut(timeOut);

        logAdapter = new LogAdapter(logger);
        Log.addLogger(logAdapter);
        DdmPreferences.setLogLevel(Log.LogLevel.VERBOSE.getStringValue());
        AndroidDebugBridge.initIfNeeded(false /*clientSupport*/);

        try {

            AndroidDebugBridge bridge =
                    AndroidDebugBridge.createBridge(
                            adbLocation.getAbsolutePath(), false /*forceNewBridge*/);

            if (bridge == null) {
                throw new RuntimeException(
                        "Could not create ADB Bridge. "
                                + "ADB location: "
                                + adbLocation.getAbsolutePath());
            }

            int getDevicesCountdown = timeOut;
            final int sleepTime = 1000;
            while (!bridge.hasInitialDeviceList() && getDevicesCountdown >= 0) {
                try {
                    Thread.sleep(sleepTime);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }

                getDevicesCountdown -= sleepTime;
            }

            if (!bridge.hasInitialDeviceList()) {
                throw new RuntimeException("Timeout getting device list.");
            }

            IDevice[] devices = bridge.getDevices();

            if (devices.length == 0) {
                throw new RuntimeException("No connected devices!");
            }

            final String androidSerialsEnv = System.getenv("ANDROID_SERIAL");
            final boolean isValidSerial = androidSerialsEnv != null && !androidSerialsEnv.isEmpty();

            final Set<String> serials;
            if (isValidSerial) {
                serials = Sets.newHashSet(Splitter.on(',').split(androidSerialsEnv));
            } else {
                serials = Sets.newHashSet();
            }

            final List<IDevice> filteredDevices = Lists.newArrayListWithCapacity(devices.length);
            for (IDevice iDevice : devices) {
                if (!isValidSerial || serials.contains(iDevice.getSerialNumber())) {
                    serials.remove(iDevice.getSerialNumber());
                    filteredDevices.add(iDevice);
                }
            }

            if (!serials.isEmpty()) {
                throw new RuntimeException(
                        String.format(
                                "Connected device with serial%s '%s' not found!",
                                serials.size() == 1 ? "" : "s", Joiner.on("', '").join(serials)));
            }

            for (IDevice device : filteredDevices) {
                if (device.getState() == IDevice.DeviceState.ONLINE) {
                    Device deviceInfo = DeviceLoader.loadDeviceCharacteristics(device, deviceGeometryRetriever);
                    localDevices.add(deviceInfo);
                } else {
                    logger.info(
                            "Skipping device '{}' ({}): Device is {}{}.",
                            device.getName(),
                            device.getSerialNumber(),
                            device.getState(),
                            device.getState() == IDevice.DeviceState.UNAUTHORIZED
                                    ? ",\n"
                                            + "    see http://d.android.com/tools/help/adb.html#Enabling"
                                    : "");
                }
            }

            if (localDevices.isEmpty()) {
                if (isValidSerial) {
                    throw new RuntimeException(
                            String.format(
                                    "Connected device with serial $1%s is not online.",
                                    androidSerialsEnv));
                } else {
                    throw new RuntimeException("No online devices found.");
                }
            }
            // ensure device names are unique since many reports are keyed off of names.
            makeDeviceNamesUnique();
        } finally {
            terminateLogger();
        }
    }

    private boolean hasDevicesWithDuplicateName() {
        Set<String> deviceNames = new HashSet<String>();
        for (Device device : localDevices) {
            if (!deviceNames.add(device.getName())) {
                return true;
            }
        }
        return false;
    }

    private void makeDeviceNamesUnique() {
        if (hasDevicesWithDuplicateName()) {
            for (Device device : localDevices) {
                device.setNameSuffix(device.getSerial());
            }
        }
        if (hasDevicesWithDuplicateName()) {
            // still have duplicates :/ just use a counter.
            int counter = 0;
            for (Device device : localDevices) {
                device.setNameSuffix(device.getSerial() + "-" + counter);
                counter ++;
            }
        }

    }

    private void terminateLogger() {
        if (logAdapter != null) {
            Log.removeLogger(logAdapter);
            logAdapter = null;
        }
    }

    private static final class LogAdapter implements Log.ILogOutput {

        private final Logger logger;

        private LogAdapter(Logger logger) {
            this.logger = logger;
        }

        @Override
        public void printLog(Log.LogLevel logLevel, String tag, String message) {
            switch (logLevel) {
                case VERBOSE:
                case DEBUG:
                    break;
                case INFO:
                    logger.info("[{}]: {}", tag, message);
                    break;
                case WARN:
                    logger.warn("[{}]: {}", tag, message);
                    break;
                case ERROR:
                case ASSERT:
                    logger.error(null, "[{}]: {}", tag, message);
                    break;
                default:
                    throw new IllegalArgumentException("Unknown log level " + logLevel);
            }
        }

        @Override
        public void printAndPromptLog(Log.LogLevel logLevel, String tag, String message) {
            printLog(logLevel, tag, message);
        }
    }
}
