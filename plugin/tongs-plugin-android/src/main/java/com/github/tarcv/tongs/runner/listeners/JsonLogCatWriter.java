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
import com.github.tarcv.tongs.system.io.TestCaseFileManager;
import com.google.gson.Gson;
import com.github.tarcv.tongs.model.Device;
import com.github.tarcv.tongs.model.Pool;

import java.io.*;
import java.util.List;

import static org.apache.commons.io.IOUtils.closeQuietly;

class JsonLogCatWriter implements LogCatWriter {
    private final Gson gson;
    private final TestCaseFileManager fileManager;
    private final Pool pool;
    private final Device device;
	private final File file;

	JsonLogCatWriter(Gson gson, TestCaseFileManager fileManager, Pool pool, Device device, File file) {
        this.gson = gson;
        this.fileManager = fileManager;
		this.pool = pool;
		this.device = device;
		this.file = file;
	}

	@Override
	public void writeLogs(List<LogCatMessage> logCatMessages) {
		FileWriter fileWriter = null;
		try {
			fileWriter = new FileWriter(file);
			gson.toJson(logCatMessages, fileWriter);
		} catch (IOException e) {
			throw new RuntimeException(e);
		} finally {
			closeQuietly(fileWriter);
		}

	}
}
