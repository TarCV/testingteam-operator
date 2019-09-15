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

package com.github.tarcv.tongs.runner.listeners;

import com.android.ddmlib.*;
import com.android.ddmlib.testrunner.TestIdentifier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;

import static com.github.tarcv.tongs.utils.Utils.millisSinceNanoTime;
import static com.github.tarcv.tongs.system.io.RemoteFileManager.remoteVideoForTest;
import static com.github.tarcv.tongs.system.io.RemoteFileManager.removeRemotePath;
import static java.lang.System.nanoTime;
import static java.util.concurrent.TimeUnit.SECONDS;

class ScreenRecorder implements Runnable {
    private static final Logger logger = LoggerFactory.getLogger(ScreenRecorder.class);
    private static final int DURATION = 60;
    private static final int BIT_RATE_MBPS = 1;
    private static final ScreenRecorderOptions RECORDER_OPTIONS = new ScreenRecorderOptions.Builder()
            .setTimeLimit(DURATION, SECONDS)
            .setBitRate(BIT_RATE_MBPS)
            .build();

    private final String remoteFilePath;
    private final File localVideoFile;
    private final IDevice deviceInterface;
    private final ScreenRecorderStopper screenRecorderStopper;

    public ScreenRecorder(TestIdentifier test, ScreenRecorderStopper screenRecorderStopper, File localVideoFile,
                          IDevice deviceInterface) {
        remoteFilePath = remoteVideoForTest(test);
        this.screenRecorderStopper = screenRecorderStopper;
        this.localVideoFile = localVideoFile;
        this.deviceInterface = deviceInterface;
    }

    @Override
    public void run() {
        try {
            startRecordingTestVideo();
            if (screenRecorderStopper.hasFailed()) {
                pullTestVideo();
            }
            removeTestVideo();
        } catch (Exception e) {
            logger.error("Something went wrong while screen recording", e);
        }
    }

    private void startRecordingTestVideo() throws TimeoutException, AdbCommandRejectedException, IOException,
            ShellCommandUnresponsiveException {
        NullOutputReceiver outputReceiver = new NullOutputReceiver();
        logger.trace("Started recording video at: {}", remoteFilePath);
        long startNanos = nanoTime();
        deviceInterface.startScreenRecorder(remoteFilePath, RECORDER_OPTIONS, outputReceiver);
        logger.trace("Recording finished in {}ms {}", millisSinceNanoTime(startNanos), remoteFilePath);
    }

    private void pullTestVideo() throws IOException, AdbCommandRejectedException, TimeoutException, SyncException {
        logger.trace("Started pulling file {} to {}", remoteFilePath, localVideoFile);
        long startNanos = nanoTime();
        deviceInterface.pullFile(remoteFilePath, localVideoFile.toString());
        logger.trace("Pulling finished in {}ms {}", millisSinceNanoTime(startNanos), remoteFilePath);
    }

    private void removeTestVideo() {
        logger.trace("Started removing file {}", remoteFilePath);
        long startNanos = nanoTime();
        removeRemotePath(deviceInterface, remoteFilePath);
        logger.trace("Removed file in {}ms {}", millisSinceNanoTime(startNanos), remoteFilePath);
    }
}
