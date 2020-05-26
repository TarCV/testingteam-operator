/*
 * Copyright 2019 TarCV
 * Copyright 2015 Shazam Entertainment Limited
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
package com.github.tarcv.tongs.summary

import com.github.jknack.handlebars.Handlebars
import com.github.jknack.handlebars.Helper
import com.github.jknack.handlebars.Options
import com.github.tarcv.tongs.api.testcases.TestCase
import com.github.tarcv.tongs.api.result.TestCaseRunResult
import com.github.tarcv.tongs.system.io.FileUtils
import com.github.tarcv.tongs.api.result.StandardFileTypes
import org.apache.commons.lang3.text.WordUtils.capitalizeFully
import java.nio.file.Paths

public enum class TongsHelpers: Helper<Any> {
    size {
        override fun apply(context: Any, options: Options): Any? {
            return if (context is Collection<*>) {
                context.size
            } else if (context is Map<*, *>) {
                context.size
            } else {
                throw RuntimeException("'size' helper is only applicable to collections")
            }
        }
    },
    unixPath {
        override fun apply(context: Any, options: Options): Any? {
            return Paths.get(context.toString()).joinToString("/")
        }
    },
    simpleClassName {
        override fun apply(context: Any, options: Options): Any? {
            return context.toString()
                    .split('.')
                    .last()
        }
    },
    readableMethodName {
        override fun apply(context: Any, options: Options): Any? {
            return context.toString()
                    .replaceFirst("test".toRegex(), "")
                    .replace("_".toRegex(), ", ")
                    .replace("(\\p{Ll})(\\p{Lu})".toRegex(), "$1 $2")
                    .replace("(\\p{Lu})(\\p{Lu})".toRegex(), "$1 $2")
                    .let { capitalizeFully(it) }
        }
    },
    filenameForTest { // TODO: Add a test case for this helper
        override fun apply(context: Any, options: Options?): Any {
            return when (context) {
                is TestCaseRunResult -> createFileName(context.testCase)
                is TestCase -> createFileName(context)
                else -> throw IllegalStateException("filenameForTest is not supported for a ${context::class.java.simpleName}")
            }
        }

        private fun createFileName(testCase: TestCase): String {
            return FileUtils.createFilenameForTest(testCase, StandardFileTypes.DOT_WITHOUT_EXTENSION)
        }

    }
    ;

    companion object {
        @JvmStatic
        fun register(handlebars: Handlebars) {
            for (helper in TongsHelpers.values()) {
                handlebars.registerHelper(helper.name, helper)
            }
        }
    }
}