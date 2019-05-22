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

import com.beust.jcommander.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;

import static com.github.tarcv.tongs.Configuration.Builder.configuration;
import static com.github.tarcv.tongs.injector.GsonInjector.gson;
import static com.github.tarcv.tongs.utils.Utils.cleanFile;

public class TongsCli {

    private static final Logger logger = LoggerFactory.getLogger(TongsCli.class);

    private TongsCli() {}

    public static class CommandLineArgs {

        @Parameter(names = { "--sdk" }, description = "Path to Android SDK", converter = FileConverter.class)
        public File sdk;

        @Parameter(names = { "--apk" }, description = "Application APK", converter = FileConverter.class,
                required = true)
        public File apk;

        @Parameter(names = { "--test-apk" }, description = "Test application APK", converter = FileConverter.class,
                required = true)
        public File testApk;

        @Parameter(names = { "--config" }, description = "Path of JSON config file", converter = FileConverter.class,
                required = true)
        public File configurationFile;

        @Parameter(names = { "-h", "--help" }, description = "Command help", help = true, hidden = true)
        public boolean help;
    }

    /* JCommander deems it necessary that this class be public. Lame. */
    public static class FileConverter implements IStringConverter<File> {

        @Override
        public File convert(String s) {
            return cleanFile(s);
        }
    }

    public static void main(String... args) {
        CommandLineArgs parsedArgs = new CommandLineArgs();
        JCommander jc = new JCommander(parsedArgs);

        try {
            jc.parse(args);
        } catch (ParameterException e) {
            StringBuilder out = new StringBuilder(e.getLocalizedMessage()).append("\n\n");
            jc.usage(out);
            logger.error(out.toString());
            System.exit(1);
            return;
        }
        if (parsedArgs.help) {
            jc.usage();
            return;
        }

        try {
            Reader configFileReader = new FileReader(parsedArgs.configurationFile);
            TongsConfigurationJsonExtension tongsConfiguration = gson().fromJson(configFileReader, TongsConfigurationJsonExtension.class);

            Configuration configuration = configuration()
                    .withAndroidSdk(parsedArgs.sdk != null ? parsedArgs.sdk : cleanFile(CommonDefaults.ANDROID_SDK))
                    .withApplicationApk(parsedArgs.apk)
                    .withInstrumentationApk(parsedArgs.testApk)
                    .withOutput(tongsConfiguration.baseOutputDir != null ? cleanFile(tongsConfiguration.baseOutputDir) : cleanFile(Defaults.TONGS_OUTPUT))
                    .withTitle(tongsConfiguration.title)
                    .withSubtitle(tongsConfiguration.subtitle)
                    .withTestClassRegex(tongsConfiguration.testClassRegex)
                    .withTestPackage(tongsConfiguration.testPackage)
                    .withTestOutputTimeout(tongsConfiguration.testOutputTimeout)
                    .withTestSize(tongsConfiguration.testSize)
                    .withExcludedSerials(tongsConfiguration.excludedSerials)
                    .withFallbackToScreenshots(tongsConfiguration.fallbackToScreenshots)
                    .withTotalAllowedRetryQuota(tongsConfiguration.totalAllowedRetryQuota)
                    .withRetryPerTestCaseQuota(tongsConfiguration.retryPerTestCaseQuota)
                    .withCoverageEnabled(tongsConfiguration.isCoverageEnabled)
                    .withPoolingStrategy(tongsConfiguration.poolingStrategy)
                    .withAutoGrantPermissions(tongsConfiguration.autoGrantPermissions)
                    .withExcludedAnnotation(tongsConfiguration.excludedAnnotation)
                    .build();

            Tongs tongs = new Tongs(configuration);
            if (!tongs.run() && !tongsConfiguration.ignoreFailures) {
                System.exit(1);
            }
        } catch (FileNotFoundException e) {
            logger.error("Could not find configuration file", e);
            System.exit(1);
        }
    }
}
