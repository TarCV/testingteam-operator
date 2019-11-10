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

package com.github.tarcv.tongs.io;

import com.github.jknack.handlebars.Context;
import com.github.jknack.handlebars.Handlebars;
import com.github.jknack.handlebars.Template;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import static org.apache.commons.io.IOUtils.closeQuietly;

public class HtmlGenerator {
    private final Handlebars templateFactory;

    public HtmlGenerator(Handlebars handlebars) {
        this.templateFactory = handlebars;
    }

    public void generateHtml(String htmlTemplateResource, File output, String filename, Object... htmlModels) {
        FileWriter writer = null;
        try {
            Template template = templateFactory.compile(htmlTemplateResource);
            Context context = buildCombinedContext(htmlModels);

            File indexFile = new File(output, filename);
            writer = new FileWriter(indexFile);
            template.apply(context, writer);
        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            closeQuietly(writer);
        }
    }

    public String generateHtmlFromInline(String inlineTemplate, Object... htmlModels) {
        try {
            Template template = templateFactory.compileInline(inlineTemplate);
            Context context = buildCombinedContext(htmlModels);
            return template.apply(context);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static Context buildCombinedContext(Object[] htmlModels) {
        Context lastContext = null;
        for (Object model : htmlModels) {
            if (lastContext == null) {
                lastContext = Context.newContext(model);
            } else {
                lastContext = Context.newContext(lastContext, model);
            }
        }
        return lastContext;
    }

}
