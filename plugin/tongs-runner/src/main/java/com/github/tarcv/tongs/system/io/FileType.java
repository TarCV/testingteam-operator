/*
 * Copyright 2019 TarCV
 * Copyright 2018 Shazam Entertainment Limited
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License.
 *
 * You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */

package com.github.tarcv.tongs.system.io;

public enum FileType {
    TEST ("tests", "xml"),
    RAW_LOG("logcat", "log"),
    JSON_LOG("logcat_json", "json"),
    SCREENSHOT ("screenshot", "png"),
    ANIMATION ("animation", "gif"),
    SCREENRECORD ("screenrecord", "mp4"),
    COVERAGE ("coverage", "ec"),
    HTML("html", "html"),

    /**
     * Generates filename like `filename.` (with the dot in the end)
     */
    DOT_WITHOUT_EXTENSION("", "")
    ;

    private final String directory;
    private final String suffix;

    FileType(String directory, String suffix) {
        this.directory = directory;
        this.suffix = suffix;
    }

    public String getDirectory() {
        return directory;
    }

    public String getSuffix() {
        return suffix;
    }
}
