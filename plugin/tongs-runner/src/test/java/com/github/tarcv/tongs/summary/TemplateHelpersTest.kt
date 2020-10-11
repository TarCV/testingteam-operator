/*
 * Copyright 2019 TarCV
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

import com.github.tarcv.tongs.injector.summary.HtmlGeneratorInjector.htmlGenerator
import org.hamcrest.Matchers.*
import org.junit.Assert
import org.junit.Test
import java.io.File

class TemplateHelpersTest {
    @Test
    fun collectionsSizeIsAccesibleViaHelper() {
        val html = htmlGenerator().generateHtmlFromInline("{{size collection}}", Model(collection = listOf("foobar")))
        Assert.assertEquals("1", html)
    }

    @Test
    fun lowerCaseIsAccesibleViaHelper() {
        val html = htmlGenerator().generateHtmlFromInline("{{lower string}}", Model(string = "FooBar"))
        Assert.assertEquals("foobar", html)
    }

    @Test
    fun despaceReplacementIsAccesibleViaHelper() {
        val html = htmlGenerator().generateHtmlFromInline("{{replace string ' ' '_'}}", Model(string = "foo bar  foobar"))
        Assert.assertEquals("foo_bar__foobar", html)
    }

    @Test
    fun equalsIsAccesibleViaHelper() {
        val html = htmlGenerator().generateHtmlFromInline("{{#if (eq string 'foo')}}bar{{/if}}", Model(string = "foo"))
        Assert.assertEquals("bar", html)
    }

    @Test
    fun unixPathIsAccesibleViaHelper() {
        val html = htmlGenerator().generateHtmlFromInline("{{unixPath string}}", Model(string = File(".").absolutePath))
        Assert.assertThat(html, containsString("/"))
        Assert.assertThat(html, not(containsString("\\")))
        Assert.assertThat(html, not(`is`("")))
    }

    @Test
    fun simpleClassNameIsAccesibleViaHelper() {
        val html = htmlGenerator().generateHtmlFromInline("{{simpleClassName string}}", Model(string = "foo.bar.Foo.Bar"))
        Assert.assertEquals("Bar", html)
    }

    @Test
    fun readableMethodNameIsAccesibleViaHelper() {
        val html = htmlGenerator().generateHtmlFromInline("{{readableMethodName string}}", Model(string = "testFo56oBar[123]_bar ASC"))
        Assert.assertEquals("Fo 56 O Bar[123] Bar Asc", html)
    }

    class Model(
            val string: String = "",
            val collection: List<String> = emptyList()
    )
}