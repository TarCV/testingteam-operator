/*
 * Copyright 2021 TarCV
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
package com.github.tarcv.tongs

import org.junit.rules.ExternalResource
import org.junit.rules.TestRule
import org.koin.core.context.KoinContextHandler
import org.koin.core.context.stopKoin

fun koinRule(configurationProvider: () -> Configuration = { aConfigurationBuilder().build(true) })
: TestRule {
    return object : ExternalResource() {
        override fun before() {
            super.before()
            stopKoinIfNeeded()
            Tongs.injectAll(configurationProvider())
        }

        override fun after() {
            try {
                stopKoinIfNeeded()
            } finally {
                super.after()
            }
        }

        private fun stopKoinIfNeeded() {
            if (KoinContextHandler.getOrNull() != null) {
                stopKoin()
            }
        }
    }
}
