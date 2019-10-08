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

import com.github.tarcv.tongs.injector.BaseRuleManager;
import com.github.tarcv.tongs.runner.rules.RuleFactory;
import com.github.tarcv.tongs.runner.rules.RunRule;
import com.github.tarcv.tongs.runner.rules.RunRuleContext;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.Collection;

import static com.github.tarcv.tongs.injector.ConfigurationInjector.configuration;
import static com.github.tarcv.tongs.injector.ConfigurationInjector.setConfiguration;
import static com.github.tarcv.tongs.injector.TongsRunnerInjector.tongsRunner;
import static com.github.tarcv.tongs.utils.Utils.millisSinceNanoTime;
import static java.lang.System.nanoTime;
import static java.util.Collections.emptyList;
import static org.apache.commons.io.FileUtils.deleteDirectory;
import static org.apache.commons.lang3.time.DurationFormatUtils.formatPeriod;

public final class Tongs {
    private static final Logger logger = LoggerFactory.getLogger(Tongs.class);

    private final TongsRunner tongsRunner;
    private final File output;
    private final Collection<String> runRuleNames;

    public Tongs(Configuration configuration) {
        this.output = configuration.getOutput();
        this.runRuleNames = configuration.getPlugins().getRunRules();
        setConfiguration(configuration);
        this.tongsRunner = tongsRunner();
    }

    public boolean run() {
		long startOfTestsMs = nanoTime();
        RunRuleManager runRulesManager = new RunRuleManager(runRuleNames);
        runRulesManager.forEach(runRule -> {
            runRule.before();
            return null;
        });
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
            runRulesManager.forEachReversed(runRule -> {
                runRule.after();
                return null;
            });
        }
	}

    private static class RunRuleManager extends BaseRuleManager<RunRuleContext, RunRule, RuleFactory<RunRuleContext, RunRule>> {
        public RunRuleManager(@NotNull Collection<String> ruleClassNames) {
            super(ruleClassNames);
        }

        @Override
        public RunRuleContext ruleContextFactory() {
            return new RunRuleContext(configuration());
        }
    }
}