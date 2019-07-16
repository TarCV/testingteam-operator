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
package com.github.tarcv.tongs;

import com.android.ddmlib.testrunner.IRemoteAndroidTestRunner;
import com.github.tarcv.tongs.system.axmlparser.InstrumentationInfo;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.Collection;
import java.util.Collections;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.github.tarcv.tongs.TongsConfiguration.TongsIntegrationTestRunType.NONE;
import static com.github.tarcv.tongs.system.axmlparser.InstrumentationInfoFactory.parseFromFile;
import static java.util.Arrays.asList;

public class Configuration implements TongsConfiguration {
    private static final Logger logger = LoggerFactory.getLogger(Configuration.class);

    private final File androidSdk;
    private final File applicationApk;
    private final File instrumentationApk;
    private final String applicationPackage;
    private final String instrumentationPackage;
    private final String testRunnerClass;
    private final File output;
    private final String title;
    private final String subtitle;
    private final String testPackage;
    private final long testOutputTimeout;
    private final IRemoteAndroidTestRunner.TestSize testSize;
    private final Collection<String> excludedSerials;
    private final boolean fallbackToScreenshots;
    private final int totalAllowedRetryQuota;
    private final int retryPerTestCaseQuota;
    private final boolean isCoverageEnabled;
    private final PoolingStrategy poolingStrategy;
    private final String excludedAnnotation;
    private final TongsIntegrationTestRunType tongsIntegrationTestRunType;
    private final boolean terminateDdm;

    private Configuration(Builder builder) {
        androidSdk = builder.androidSdk;
        applicationApk = builder.applicationApk;
        instrumentationApk = builder.instrumentationApk;
        applicationPackage = builder.applicationPackage;
        instrumentationPackage = builder.instrumentationPackage;
        testRunnerClass = builder.testRunnerClass;
        output = builder.output;
        title = builder.title;
        subtitle = builder.subtitle;
        testPackage = builder.testPackage;
        testOutputTimeout = builder.testOutputTimeout;
        testSize = builder.testSize;
        excludedSerials = builder.excludedSerials;
        fallbackToScreenshots = builder.fallbackToScreenshots;
        totalAllowedRetryQuota = builder.totalAllowedRetryQuota;
        retryPerTestCaseQuota = builder.retryPerTestCaseQuota;
        isCoverageEnabled = builder.isCoverageEnabled;
        poolingStrategy = builder.poolingStrategy;
        this.excludedAnnotation = builder.excludedAnnotation;
        this.tongsIntegrationTestRunType = builder.tongsIntegrationTestRunType;
        this.terminateDdm = builder.terminateDdm;
    }

    @Override
    @Nonnull
    public File getAndroidSdk() {
        return androidSdk;
    }

    @Override
    @Nullable
    public File getApplicationApk() {
        return applicationApk;
    }

    @Override
    @Nullable
    public File getInstrumentationApk() {
        return instrumentationApk;
    }

    @Override
    @Nonnull
    public String getApplicationPackage() {
        return applicationPackage;
    }

    @Override
    @Nonnull
    public String getInstrumentationPackage() {
        return instrumentationPackage;
    }

    @Override
    @Nonnull
    public String getTestRunnerClass() {
        return testRunnerClass;
    }

    @Override
    @Nonnull
    public File getOutput() {
        return output;
    }

    @Override
    @Nonnull
    public String getTitle() {
        return title;
    }

    @Override
    @Nonnull
    public String getSubtitle() {
        return subtitle;
    }

    @Override
    @Nonnull
    public String getTestPackage() {
        return testPackage;
    }

    @Override
    public long getTestOutputTimeout() {
        return testOutputTimeout;
    }

    @Override
    @Nullable
    public IRemoteAndroidTestRunner.TestSize getTestSize() {
        return testSize;
    }

    @Override
    @Nonnull
    public Collection<String> getExcludedSerials() {
        return excludedSerials;
    }

    @Override
    public boolean canFallbackToScreenshots() {
        return fallbackToScreenshots;
    }

    @Override
    public int getTotalAllowedRetryQuota() {
        return totalAllowedRetryQuota;
    }

    @Override
    public int getRetryPerTestCaseQuota() {
        return retryPerTestCaseQuota;
    }

    @Override
    public boolean isCoverageEnabled() {
        return isCoverageEnabled;
    }

    @Override
    public PoolingStrategy getPoolingStrategy() {
        return poolingStrategy;
    }

    @Override
    public String getExcludedAnnotation() {
        return excludedAnnotation;
    }

    @Override
    public boolean shouldTerminateDdm() {
        return terminateDdm;
    }

    @Override
    public TongsIntegrationTestRunType getTongsIntegrationTestRunType() {
        return tongsIntegrationTestRunType;
    }

    public static class Builder {
        private File androidSdk;
        private File applicationApk;
        private File instrumentationApk;
        private String applicationPackage;
        private String instrumentationPackage;
        private String testRunnerClass;
        private File output;
        private String title;
        private String subtitle;
        private String testPackage;
        private long testOutputTimeout;
        private IRemoteAndroidTestRunner.TestSize testSize;
        private Collection<String> excludedSerials;
        private boolean fallbackToScreenshots;
        private int totalAllowedRetryQuota;
        private int retryPerTestCaseQuota;
        private boolean isCoverageEnabled;
        private PoolingStrategy poolingStrategy;
        private String excludedAnnotation;
        private TongsIntegrationTestRunType tongsIntegrationTestRunType = NONE;
        private boolean terminateDdm = true;

        public static Builder configuration() {
            return new Builder();
        }

        public Builder withAndroidSdk(@Nonnull File androidSdk) {
            this.androidSdk = androidSdk;
            return this;
        }

        public Builder withApplicationApk(@Nonnull File applicationApk) {
            this.applicationApk = applicationApk;
            return this;
        }

        public Builder withInstrumentationApk(@Nonnull File instrumentationApk) {
            this.instrumentationApk = instrumentationApk;
            return this;
        }

        public Builder withOutput(@Nonnull File output) {
            this.output = output;
            return this;
        }

        public Builder withTitle(String title) {
            this.title = title;
            return this;
        }

        public Builder withSubtitle(String subtitle) {
            this.subtitle = subtitle;
            return this;
        }

        public Builder withApplicationPackage(String applicationPackage) {
            this.applicationPackage = applicationPackage;
            return this;
        }

        public Builder withInstrumentationPackage(String instrumentationPackage) {
            this.instrumentationPackage = instrumentationPackage;
            return this;
        }

        public Builder withTestPackage(String testPackage) {
            this.testPackage = testPackage;
            return this;
        }

        public Builder withTestRunnerClass(String testRunnerClass) {
            this.testRunnerClass = testRunnerClass;
            return this;
        }

        public Builder withTestOutputTimeout(int testOutputTimeout) {
            this.testOutputTimeout = testOutputTimeout;
            return this;
        }

        public Builder withTestSize(String testSize) {
            this.testSize = (testSize == null ? null : IRemoteAndroidTestRunner.TestSize.getTestSize(testSize));
            return this;
        }

        public Builder withExcludedSerials(Collection<String> excludedSerials) {
            this.excludedSerials = excludedSerials;
            return this;
        }

        public Builder withFallbackToScreenshots(boolean fallbackToScreenshots) {
            this.fallbackToScreenshots = fallbackToScreenshots;
            return this;
        }

        public Builder withTotalAllowedRetryQuota(int totalAllowedRetryQuota) {
            this.totalAllowedRetryQuota = totalAllowedRetryQuota;
            return this;
        }

        public Builder withRetryPerTestCaseQuota(int retryPerTestCaseQuota) {
            this.retryPerTestCaseQuota = retryPerTestCaseQuota;
            return this;
        }

        public Builder withCoverageEnabled(boolean isCoverageEnabled) {
            this.isCoverageEnabled = isCoverageEnabled;
            return this;
        }

        public Builder withPoolingStrategy(@Nullable PoolingStrategy poolingStrategy) {
            this.poolingStrategy = poolingStrategy;
            return this;
        }

        public Builder withExcludedAnnotation(String excludedAnnotation) {
            this.excludedAnnotation = excludedAnnotation;
            return this;
        }

        public Builder withTongsIntegrationTestRunType(TongsIntegrationTestRunType tongsIntegrationTestRunType) {
            this.tongsIntegrationTestRunType = tongsIntegrationTestRunType;
            return this;
        }

        public Builder withDdmTermination(boolean terminateDdm) {
            this.terminateDdm = terminateDdm;
            return this;
        }

        public Configuration build() {
            checkNotNull(androidSdk, "SDK is required.");
            checkArgument(androidSdk.exists(), "SDK directory does not exist.");
            if (instrumentationApk != null) {
                checkNotNull(applicationApk, "Application APK is required when instrumentation APK is set.");
                checkArgument(applicationApk.exists(), "Application APK file does not exist.");
            }
            if (applicationApk != null) {
                checkNotNull(instrumentationApk, "Instrumentation APK is required when application APK is set.");
                checkArgument(instrumentationApk.exists(), "Instrumentation APK file does not exist.");
            }
            if (instrumentationApk != null) {
                InstrumentationInfo instrumentationInfo = parseFromFile(instrumentationApk);
                checkNotNull(instrumentationInfo.getApplicationPackage(), "Application package was not found in test APK");
                applicationPackage = instrumentationInfo.getApplicationPackage();
                checkNotNull(instrumentationInfo.getInstrumentationPackage(), "Instrumentation package was not found in test APK");
                instrumentationPackage = instrumentationInfo.getInstrumentationPackage();
                checkNotNull(instrumentationInfo.getTestRunnerClass(), "Test runner class was not found in test APK");
                testRunnerClass = instrumentationInfo.getTestRunnerClass();
            } else {
                checkNotNull(applicationPackage, "Application package name is required when instrumentation APK is not set");
                checkNotNull(instrumentationPackage, "Instrumentation package name is required when instrumentation APK is not set");
                checkNotNull(testRunnerClass, "Test runner class is required when instrumentation APK is not set");
            }
            testPackage = assignValueOrDefaultIfNull(testPackage, applicationPackage);

            checkNotNull(output, "Output path is required.");

            title = assignValueOrDefaultIfNull(title, Defaults.TITLE);
            subtitle = assignValueOrDefaultIfNull(subtitle, Defaults.SUBTITLE);
            testOutputTimeout = assignValueOrDefaultIfZero(testOutputTimeout, Defaults.TEST_OUTPUT_TIMEOUT_MILLIS);
            excludedSerials = assignValueOrDefaultIfNull(excludedSerials, Collections.<String>emptyList());
            checkArgument(totalAllowedRetryQuota >= 0, "Total allowed retry quota should not be negative.");
            checkArgument(retryPerTestCaseQuota >= 0, "Retry per test case quota should not be negative.");
            retryPerTestCaseQuota = assignValueOrDefaultIfZero(retryPerTestCaseQuota, Defaults.RETRY_QUOTA_PER_TEST_CASE);
            logArgumentsBadInteractions();
            poolingStrategy = validatePoolingStrategy(poolingStrategy);
            return new Configuration(this);
        }

        private static <T> T assignValueOrDefaultIfNull(T value, T defaultValue) {
            return value != null ? value : defaultValue;
        }

        private static <T extends Number> T assignValueOrDefaultIfZero(T value, T defaultValue) {
            return value.longValue() != 0 ? value : defaultValue;
        }

        private void logArgumentsBadInteractions() {
            if (totalAllowedRetryQuota > 0 && totalAllowedRetryQuota < retryPerTestCaseQuota) {
                logger.warn("Total allowed retry quota [" + totalAllowedRetryQuota + "] " +
                        "is smaller than Retry per test case quota [" + retryPerTestCaseQuota + "]. " +
                        "This is suspicious as the first mentioned parameter is an overall cap.");
            }
        }

        /**
         * We need to make sure zero or one strategy has been passed. If zero default to pool per device. If more than one
         * we throw an exception.
         */
        private PoolingStrategy validatePoolingStrategy(PoolingStrategy poolingStrategy) {
            if (poolingStrategy == null) {
                logger.warn("No strategy was chosen in configuration, so defaulting to one pool per device");
                poolingStrategy = new PoolingStrategy();
                poolingStrategy.eachDevice = true;
            } else {
                long selectedStrategies = asList(
                        poolingStrategy.eachDevice,
                        poolingStrategy.splitTablets,
                        poolingStrategy.computed,
                        poolingStrategy.manual)
                        .stream()
                        .filter(p -> p != null)
                        .count();
                if (selectedStrategies > Defaults.STRATEGY_LIMIT) {
                    throw new IllegalArgumentException("You have selected more than one strategies in configuration. " +
                            "You can only select up to one.");
                }
            }

            return poolingStrategy;
        }
    }
}
