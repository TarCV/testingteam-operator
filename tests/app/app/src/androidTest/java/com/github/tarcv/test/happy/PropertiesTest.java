/*
 * Copyright 2020 TarCV
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License.
 *
 * You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */

package com.github.tarcv.test.happy;

import androidx.test.rule.ActivityTestRule;
import com.github.tarcv.test.MainActivity;
import com.github.tarcv.tongs.TestProperties;
import com.github.tarcv.tongs.TestPropertyPairs;
import org.junit.Rule;
import org.junit.Test;

public class PropertiesTest {
    @Rule
    public ActivityTestRule<MainActivity> rule = new ActivityTestRule<>(
            MainActivity.class, true, true);

    @Test
    @TestProperties(keys = {"x", "y"}, values = {"1", "2"})
    public void normalPropertiesTest() {
        TestHelpers.basicTestSteps();
    }

    @Test
    @TestPropertyPairs({"v", "1", "w", "2"})
    public void normalPropertyPairsTest() {
        TestHelpers.basicTestSteps();
    }
}
