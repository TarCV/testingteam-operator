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
package com.github.tarcv.tongs.runner.listeners;

import com.android.ddmlib.logcat.LogCatMessage;
import com.android.ddmlib.testrunner.TestIdentifier;
import com.github.tarcv.tongs.model.Device;
import com.github.tarcv.tongs.model.Pool;
import com.github.tarcv.tongs.runner.PreregisteringLatch;
import com.github.tarcv.tongs.runner.Table;
import com.github.tarcv.tongs.runner.TestCaseFile;
import com.github.tarcv.tongs.system.io.TestCaseFileManager;
import com.google.gson.Gson;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.github.tarcv.tongs.system.io.StandardFileTypes.JSON_LOG;
import static com.github.tarcv.tongs.system.io.StandardFileTypes.RAW_LOG;
import static java.util.Arrays.asList;

class LogCatTestRunListener extends BaseListener {
	private final LogcatReceiver logcatReceiver;
    private final TestCaseFileManager fileManager;
    private final Pool pool;
	private final Device device;
    private final Gson gson;
    private final List<LogCatMessage> messages = Collections.synchronizedList(new ArrayList<LogCatMessage>());

	private final TestCaseFile jsonFile;
	private final TestCaseFile rawFile;

	public LogCatTestRunListener(Gson gson, TestCaseFileManager fileManager, Pool pool, Device device, PreregisteringLatch latch) {
		super(latch);
		this.logcatReceiver = new LogcatReceiver(device);
        this.gson = gson;
        this.fileManager = fileManager;
        this.pool = pool;
		this.device = device;
		this.jsonFile = new TestCaseFile(fileManager, JSON_LOG, "");
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
                new JsonLogCatWriter(gson, fileManager, pool, device, jsonFile.create()),
                new RawLogCatWriter(fileManager, pool, device, rawFile.create()));
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

	public TestCaseFile getJsonFile() {
		return jsonFile;
	}

	public TestCaseFile getRawFile() {
		return rawFile;
	}

	@NotNull
	public Table getAsTable() {
		List<String> headers = asList("appName",
				"logLevel",
				"message",
				"pid",
				"tag",
				"tid",
				"time"
		);
		List<List<String>> rows = messages.stream()
				.map(logCatMessage -> {
					return asList(logCatMessage.getAppName(),
							logCatMessage.getLogLevel().getStringValue(),
							logCatMessage.getMessage(),
							String.valueOf(logCatMessage.getPid()),
							logCatMessage.getTag(),
							String.valueOf(logCatMessage.getTid()),
							logCatMessage.getTimestamp().toString()
					);
				})
				.collect(Collectors.toList());
		return new Table(headers, rows);
	}
}
