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
package com.github.tarcv.tongs.injector;

import com.github.tarcv.tongs.TongsRunner;

import com.github.tarcv.tongs.plugin.android.PropertiesTestCaseRuleFactory;
import com.github.tarcv.tongs.runner.rules.RuleFactory;
import com.github.tarcv.tongs.runner.rules.TestCaseRule;
import com.github.tarcv.tongs.runner.rules.TestCaseRuleContext;
import com.github.tarcv.tongs.runner.rules.TestCaseRuleFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

import static com.github.tarcv.tongs.injector.ConfigurationInjector.configuration;
import static com.github.tarcv.tongs.injector.pooling.PoolLoaderInjector.poolLoader;
import static com.github.tarcv.tongs.injector.runner.PoolTestRunnerFactoryInjector.poolTestRunnerFactory;
import static com.github.tarcv.tongs.injector.runner.ProgressReporterInjector.progressReporter;
import static com.github.tarcv.tongs.injector.summary.SummaryGeneratorHookInjector.summaryGeneratorHook;
import static com.github.tarcv.tongs.utils.Utils.millisSinceNanoTime;
import static java.lang.System.nanoTime;
import static java.util.Collections.emptyList;

public class TongsRunnerInjector {

    private static final Logger logger = LoggerFactory.getLogger(TongsRunnerInjector.class);

    private TongsRunnerInjector() {}

    public static TongsRunner tongsRunner() {
        long startNanos = nanoTime();

        TestCaseRuleManager ruleManager = new TestCaseRuleManager(
                configuration().getPlugins().getTestCaseRules(),
                Collections.singletonList(new PropertiesTestCaseRuleFactory())
        );
        TongsRunner tongsRunner = new TongsRunner(
                poolLoader(),
                poolTestRunnerFactory(),
                progressReporter(),
                summaryGeneratorHook(),
                ruleManager
        );

        logger.debug("Bootstrap of TongsRunner took: {} milliseconds", millisSinceNanoTime(startNanos));

        return tongsRunner;
    }

    public static class TestCaseRuleManager
            extends BaseRuleManager<TestCaseRuleContext, TestCaseRule,
            RuleFactory<? super TestCaseRuleContext, ? extends TestCaseRule>> {
        public TestCaseRuleManager(
                Collection<String> ruleClassNames,
                Collection<RuleFactory<? super TestCaseRuleContext, ? extends TestCaseRule>> predefinedFactories) {
            super(ruleClassNames,predefinedFactories);
        }
    }
}
