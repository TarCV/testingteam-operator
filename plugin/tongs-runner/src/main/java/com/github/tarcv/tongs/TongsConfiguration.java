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

package com.github.tarcv.tongs;

import com.android.ddmlib.testrunner.IRemoteAndroidTestRunner;

import java.io.File;
import java.util.Collection;
import java.util.Map;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public interface TongsConfiguration {
    @Nonnull
    File getAndroidSdk();

    @Nullable
    File getApplicationApk();

    @Nullable
    File getInstrumentationApk();

    @Nonnull
    String getApplicationPackage();

    @Nonnull
    String getInstrumentationPackage();

    @Nonnull
    String getTestRunnerClass();

    @Nonnull
    Map<String, String> getTestRunnerArguments();

    @Nonnull
    File getOutput();

    @Nonnull
    String getTitle();

    @Nonnull
    String getSubtitle();

    @Nonnull
    String getTestPackage();

    long getTestOutputTimeout();

    @Nullable
    IRemoteAndroidTestRunner.TestSize getTestSize();

    @Nonnull
    Collection<String> getExcludedSerials();

    boolean canFallbackToScreenshots();

    int getTotalAllowedRetryQuota();

    int getRetryPerTestCaseQuota();

    boolean isCoverageEnabled();

    PoolingStrategy getPoolingStrategy();

    String getExcludedAnnotation();

    TongsIntegrationTestRunType getTongsIntegrationTestRunType();

    boolean shouldTerminateDdm();

    Plugins getPlugins();

    enum TongsIntegrationTestRunType {
        NONE,
        STUB_PARALLEL_TESTRUN,
        RECORD_LISTENER_EVENTS
    }
}
