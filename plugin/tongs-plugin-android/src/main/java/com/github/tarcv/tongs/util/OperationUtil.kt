/*
 * Copyright 2020 TarCV
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
package com.github.tarcv.tongs.util

inline fun <reified T: Throwable>repeatUntilSuccessful(
    maxAttempts: Int = 5,
    onError: (T) -> Unit = {},
    block: () -> Unit
) {
    var attempt = 0
    while (true) {
        attempt++
        try {
            block()
            return
        } catch (e: Throwable) {
            if (attempt < maxAttempts && T::class.java.isInstance(e)) {
                onError(e as T)
            } else {
                throw e
            }
        }
    }
}