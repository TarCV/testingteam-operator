/*
 * Copyright 2020 TarCV
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License.
 *
 * You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */

package com.github.tarcv.test;

import androidx.test.filters.SdkSuppress;
import androidx.test.rule.ActivityTestRule;
import com.github.tarcv.test.happy.TestHelpers;
import org.junit.After;
import org.junit.Assume;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.*;

@RunWith(Parameterized.class)
public class ResultTest {
    @Rule
    public ActivityTestRule<MainActivity> rule = new ActivityTestRule<>(
            MainActivity.class, true, true);

    private final boolean failAfter;

    public ResultTest(boolean failAfter) {
        this.failAfter = failAfter;
    }

    @After
    public void afterMethod() {
        if (failAfter) {
            throw new RuntimeException("Exception from afterMethod");
        }
    }

    @Test
    public void successful() {
        successfulStep();
    }

    @Test
    @SdkSuppress(minSdkVersion = 23) // screenrecord on emulators only supported since 23
    public void failureFromEspresso() {
        TestHelpers.basicTestSteps();
        onView(withResourceName("non_existing_id"))
                .check(matches(isDisplayed()));
    }

    @Test
    public void assumptionFailure() {
        successfulStep();

        //noinspection ConstantConditions
        Assume.assumeTrue(false);
    }

    @Parameterized.Parameters(name = "failAfter = {0}")
    public static Object[] data() {
        return new Object[] { false, true };
    }

    private void successfulStep() {
        onView(withId(R.id.hello_text))
                .check(matches(isDisplayed()));
    }
}
