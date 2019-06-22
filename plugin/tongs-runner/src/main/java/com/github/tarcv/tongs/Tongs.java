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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;

import static com.github.tarcv.tongs.injector.ConfigurationInjector.setConfiguration;
import static com.github.tarcv.tongs.injector.TongsRunnerInjector.tongsRunner;
import static com.github.tarcv.tongs.utils.Utils.millisSinceNanoTime;
import static java.lang.System.nanoTime;
import static org.apache.commons.io.FileUtils.deleteDirectory;
import static org.apache.commons.lang3.time.DurationFormatUtils.formatPeriod;

public final class Tongs {
    private static final Logger logger = LoggerFactory.getLogger(Tongs.class);

    private final TongsRunner tongsRunner;
    private final File output;

    public Tongs(Configuration configuration) {
        this.output = configuration.getOutput();
        setConfiguration(configuration);
        this.tongsRunner = tongsRunner();
    }

    public boolean run() {
		long startOfTestsMs = nanoTime();
		try {
            deleteDirectory(output);
            //noinspection ResultOfMethodCallIgnored
            output.mkdirs();
            return tongsRunner.run();
		} catch (Exception e) {
            logger.error("Error while running Tongs", e);
			return false;
		} finally {
            long duration = millisSinceNanoTime(startOfTestsMs);
            logger.info(formatPeriod(0, duration, "'Total time taken:' H 'hours' m 'minutes' s 'seconds'"));
		}
	}
}
