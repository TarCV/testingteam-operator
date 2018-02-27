/*
 * Copyright 2018 TarCV
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

import com.android.ddmlib.testrunner.TestIdentifier;
import com.android.ddmlib.testrunner.XmlTestRunListener;
import com.google.common.collect.ImmutableMap;
import com.github.tarcv.tongs.model.Device;
import com.github.tarcv.tongs.model.Pool;
import com.github.tarcv.tongs.model.TestCaseEvent;
import com.github.tarcv.tongs.runner.ProgressReporter;
import com.github.tarcv.tongs.system.io.FileManager;
import com.github.tarcv.tongs.system.io.FileType;

import java.io.File;
import java.util.Map;

import javax.annotation.Nonnull;

import static com.github.tarcv.tongs.model.TestCaseEvent.newTestCase;
import static com.github.tarcv.tongs.summary.TestResult.SUMMARY_KEY_TOTAL_FAILURE_COUNT;

public class TongsXmlTestRunListener extends XmlTestRunListener {

    private final FileManager fileManager;
    private final Pool pool;
    private final Device device;
    private final TestCaseEvent testCase;

    @Nonnull
    private final ProgressReporter progressReporter;
    private TestIdentifier test;

    public TongsXmlTestRunListener(FileManager fileManager,
                                  Pool pool,
                                  Device device,
                                  TestCaseEvent testCase,
                                  @Nonnull ProgressReporter progressReporter) {
        this.fileManager = fileManager;
        this.pool = pool;
        this.device = device;
        this.testCase = testCase;
        this.progressReporter = progressReporter;
    }

    @Override
    protected File getResultFile(File reportDir) {
        return fileManager.createFile(FileType.TEST, pool, device, testCase);
    }

    @Override
    public void testStarted(TestIdentifier test) {
        this.test = test;
        super.testStarted(test);
    }

    @Override
    protected Map<String, String> getPropertiesAttributes() {
        ImmutableMap.Builder<String, String> mapBuilder = ImmutableMap.<String, String>builder()
                .putAll(super.getPropertiesAttributes());
        if (test != null) {
            int testFailuresCount = progressReporter.getTestFailuresCount(pool, newTestCase(test));
            if (testFailuresCount > 0) {
                mapBuilder.put(SUMMARY_KEY_TOTAL_FAILURE_COUNT, Integer.toString(testFailuresCount));
            }
        }
        if (testCase != null) {
            mapBuilder.putAll(testCase.getProperties());
        }
        return mapBuilder.build();
    }
}
