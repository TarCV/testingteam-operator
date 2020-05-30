/*
 * Copyright 2020 TarCV
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License.
 *
 * You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */

package com.github.tarcv.tongs.suite;

import com.android.ddmlib.testrunner.TestIdentifier;
import com.github.tarcv.tongs.runner.TestInfo;
import com.github.tarcv.tongs.runner.listeners.BaseListener;
import com.google.gson.JsonObject;

import java.util.*;

/**
 * Records identifiers of started tests
 *
 * Serves as temporary container for annotation info until farther refactoring
 */
public class TestCollectingListener extends BaseListener {
    private final Set<TestIdentifier> tests = Collections.synchronizedSet(new HashSet<>());
    private final Map<TestIdentifier, TestInfo> infos = Collections.synchronizedMap(new HashMap<TestIdentifier, TestInfo>());

    public TestCollectingListener() {
        super(null);
    }

    public Set<TestIdentifier> getTests() {
        synchronized (tests) {
            return Collections.unmodifiableSet(new HashSet<>(tests));
        }
    }

    public Map<TestIdentifier, TestInfo> getInfos() {
        return infos;
    }

    public void publishTestInfo(Map<TestIdentifier, TestInfo> info) {
        infos.putAll(info);
    }

    @Override
    public void testStarted(TestIdentifier test) {
        tests.add(test);
    }

    @Override
    public void testIgnored(TestIdentifier test) {
    }

    @Override
    public void testRunStarted(String runName, int testCount) {

    }

    @Override
    public void testFailed(TestIdentifier test, String trace) {

    }

    @Override
    public void testAssumptionFailure(TestIdentifier test, String trace) {

    }

    @Override
    public void testEnded(TestIdentifier test, Map<String, String> testMetrics) {

    }

    @Override
    public void testRunFailed(String errorMessage) {

    }

    @Override
    public void testRunStopped(long elapsedTime) {

    }

    @Override
    public void testRunEnded(long elapsedTime, Map<String, String> runMetrics) {

    }
}
