/*
 * Copyright 2020 TarCV
 * Copyright 2015 Shazam Entertainment Limited
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License.
 *
 * You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */

package com.github.tarcv.tongs.utils;

import java.io.File;

import javax.annotation.Nullable;

import static java.lang.System.nanoTime;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.NANOSECONDS;

public class Utils {

    private Utils() {}
    
    public static long millisSinceNanoTime(long startNanos) {
        return millisBetweenNanoTimes(startNanos, nanoTime());
    }

    public static long millisBetweenNanoTimes(long startNanos, long endNanos) {
        long elapsedNanos = endNanos - startNanos;
        return MILLISECONDS.convert(elapsedNanos, NANOSECONDS);
    }

    @Nullable
    public static File cleanFile(@Nullable String path) {
        if (path == null) {
            return null;
        }
        return new File(path).getAbsoluteFile();
    }

    public static File cleanFileSafe(String path) {
        return new File(path).getAbsoluteFile();
    }
}
