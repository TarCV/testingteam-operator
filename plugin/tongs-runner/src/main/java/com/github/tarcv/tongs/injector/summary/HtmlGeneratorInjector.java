/*
 * Copyright 2019 TarCV
 * Copyright 2015 Shazam Entertainment Limited
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License.
 *
 * You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */

package com.github.tarcv.tongs.injector.summary;

import com.github.jknack.handlebars.Handlebars;
import com.github.jknack.handlebars.Helper;
import com.github.jknack.handlebars.HumanizeHelper;
import com.github.jknack.handlebars.cache.ConcurrentMapTemplateCache;
import com.github.jknack.handlebars.helper.ConditionalHelpers;
import com.github.jknack.handlebars.helper.StringHelpers;
import com.github.jknack.handlebars.io.ClassPathTemplateLoader;
import com.github.jknack.handlebars.io.TemplateLoader;
import com.github.tarcv.tongs.io.HtmlGenerator;
import com.github.tarcv.tongs.summary.TongsHelpers;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

public class HtmlGeneratorInjector {
    private final static ConcurrentMapTemplateCache CACHE = new ConcurrentMapTemplateCache();

    private HtmlGeneratorInjector() {}

    public static HtmlGenerator htmlGenerator() {
        TemplateLoader loader = new ClassPathTemplateLoader();
        loader.setSuffix("");

        Handlebars handlebars = new Handlebars(loader).with(CACHE);
        registerHelpers(handlebars);

        return new HtmlGenerator(handlebars);
    }

    private static void registerHelpers(Handlebars handlebars) {
        registerEnumHelpers(handlebars, ConditionalHelpers.values());

        HumanizeHelper[] forcedHumanizeHelpers = new HumanizeHelper[] { HumanizeHelper.wordWrap };
        registerNonConflictingHelpers(handlebars, HumanizeHelper.values(), StringHelpers.values());
        registerNonConflictingHelpers(handlebars, StringHelpers.values(), forcedHumanizeHelpers);
        registerEnumHelpers(handlebars, forcedHumanizeHelpers);

        TongsHelpers.register(handlebars);
    }

    private static void registerNonConflictingHelpers(Handlebars handlebars,  Enum<? extends Helper>[] helpers, Enum<? extends Helper>[] excludeConflictsWith) {
        Set<String> reservedNames = Arrays.stream(excludeConflictsWith)
                .map(helper -> helper.name())
                .collect(Collectors.toSet());
        Arrays.stream(helpers)
                .filter(helper -> {
                    // filter out helpers with conflicting names
                    return !reservedNames.contains(helper.name());
                })
                .forEach(helper -> {
                    registerEnumHelper(handlebars, helper);
                });
    }

    private static Handlebars registerEnumHelper(Handlebars handlebars, Enum<? extends Helper> helper) {
        return handlebars.registerHelper(helper.name(), (Helper) helper);
    }

    private static void registerEnumHelpers(Handlebars handlebars, Enum<? extends Helper>[] helpers) {
        for (Enum<? extends Helper> helper : helpers) {
            registerEnumHelper(handlebars, helper);
        }
    }
}
