/*
 * Copyright 2020 TarCV
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
package com.github.tarcv.tongs.runner.listeners;

import com.android.ddmlib.logcat.LogCatMessage;
import com.android.ddmlib.testrunner.TestIdentifier;
import com.github.tarcv.tongs.model.AndroidDevice;
import com.github.tarcv.tongs.api.devices.Pool;
import com.github.tarcv.tongs.runner.PreregisteringLatch;
import com.github.tarcv.tongs.api.result.TestCaseFile;
import com.github.tarcv.tongs.api.result.TestCaseFileManager;
import com.google.gson.Gson;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static com.github.tarcv.tongs.api.result.StandardFileTypes.JSON_LOG;
import static com.github.tarcv.tongs.api.result.StandardFileTypes.RAW_LOG;

class LogCatTestRunListener extends BaseListener {
	private final LogcatReceiver logcatReceiver;
    private final TestCaseFileManager fileManager;
    private final Pool pool;
	private final AndroidDevice device;
    private final Gson gson;
    private final List<LogCatMessage> messages = Collections.synchronizedList(new ArrayList<LogCatMessage>());

	private final TestCaseFile tableFile;
	private final TestCaseFile rawFile;

	public LogCatTestRunListener(Gson gson, TestCaseFileManager fileManager, Pool pool, AndroidDevice device, PreregisteringLatch latch) {
		super(latch);
		this.logcatReceiver = new LogcatReceiver(device);
        this.gson = gson;
        this.fileManager = fileManager;
        this.pool = pool;
		this.device = device;
		this.tableFile = new TestCaseFile(fileManager, JSON_LOG, "");
		this.rawFile = new TestCaseFile(fileManager, RAW_LOG, "");
	}

	@Override
	public void testRunStarted(String runName, int testCount) {
		logcatReceiver.start(runName);
	}

	@Override
	public void testStarted(TestIdentifier test) {
	}

	@Override
	public void testFailed(TestIdentifier test, String trace) {
	}

    @Override
    public void testAssumptionFailure(TestIdentifier test, String trace) {
    }

    @Override
    public void testIgnored(TestIdentifier test) {
    }

    @Override
	public void testEnded(TestIdentifier test, Map<String, String> testMetrics) {
		List<LogCatMessage> copyOfLogCatMessages = logcatReceiver.getMessages();
		synchronized (messages) {
			messages.clear();
			messages.addAll(copyOfLogCatMessages);
		}
		LogCatWriter logCatWriter = new CompositeLogCatWriter(
                new TableLogCatWriter(gson, tableFile),
                new RawLogCatWriter(fileManager, pool, device, rawFile));
        LogCatSerializer logCatSerializer = new LogCatSerializer(test, logCatWriter);
		logCatSerializer.serializeLogs(copyOfLogCatMessages);
	}

	@Override
	public void testRunFailed(String errorMessage) {
	}

	@Override
	public void testRunStopped(long elapsedTime) {
	}

	@Override
	public void testRunEnded(long elapsedTime, Map<String, String> runMetrics) {
    	try {
			logcatReceiver.stop();
		} finally {
    		onWorkFinished();
		}
	}

	public TestCaseFile getTableFile() {
		return tableFile;
	}

	public TestCaseFile getRawFile() {
		return rawFile;
	}
}
