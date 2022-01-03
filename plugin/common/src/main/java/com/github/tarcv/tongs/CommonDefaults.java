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

package com.github.tarcv.tongs;

public class CommonDefaults {

    private CommonDefaults() {
    }

    public static final String ANDROID_SDK = System.getenv("ANDROID_HOME");
    public static final String TONGS = "tongs-";
    public static final String JSON = "json";
    public static final String TONGS_SUMMARY_FILENAME_FORMAT = TONGS + "%s." + JSON;
    public static final String TONGS_SUMMARY_FILENAME_REGEX = TONGS + ".*\\." + JSON;
    public static final String BUILD_ID_TOKEN = "{BUILD_ID}";
}
