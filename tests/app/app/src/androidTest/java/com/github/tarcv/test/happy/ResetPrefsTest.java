/*
 * Copyright 2018 TarCV
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License.
 *
 * You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */

package com.github.tarcv.test.happy;

import android.support.test.InstrumentationRegistry;
import android.support.test.rule.ActivityTestRule;
import com.github.tarcv.test.MainActivity;
import junit.framework.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import static android.content.Context.MODE_PRIVATE;

@RunWith(Parameterized.class)
public class ResetPrefsTest {
    @Rule
    public ActivityTestRule<MainActivity> rule = new ActivityTestRule<>(
            MainActivity.class, true, true);

    public ResetPrefsTest(int param) {

    }

    @Test
    public void testPrefsAreClearedBetweenTests() {
        boolean prefNotPresent = InstrumentationRegistry.getTargetContext()
                .getSharedPreferences(this.getClass().getName(), MODE_PRIVATE)
                .getAll()
                .isEmpty();
        Assert.assertTrue("Prefs should be empty", prefNotPresent);

        TestHelpers.basicTestSteps();

        InstrumentationRegistry.getTargetContext()
                .getSharedPreferences(this.getClass().getName(), MODE_PRIVATE)
                .edit()
                .putBoolean("TEST_KEY", true)
                .commit();
    }

    @Parameters
    public static Object[] data() {
        return new Object[] { 1, 2, 3, 4 };
    }
}
