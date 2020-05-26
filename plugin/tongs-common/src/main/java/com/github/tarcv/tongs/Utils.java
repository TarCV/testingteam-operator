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
package com.github.tarcv.tongs;

import com.google.common.util.concurrent.ThreadFactoryBuilder;

import javax.annotation.Nullable;
import java.io.File;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadFactory;

import static java.lang.System.nanoTime;
import static java.util.concurrent.Executors.newFixedThreadPool;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.NANOSECONDS;

public class Utils {

    private Utils() {
    }

    public static ExecutorService namedExecutor(int numberOfThreads, String nameFormat) {
        ThreadFactory namedThreadFactory = new ThreadFactoryBuilder().setNameFormat(nameFormat).build();
        return newFixedThreadPool(numberOfThreads, namedThreadFactory);
    }

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
