package com.github.tarcv.test;

import android.support.test.filters.SdkSuppress;
import android.support.test.rule.ActivityTestRule;
import com.github.tarcv.test.happy.TestHelpers;
import org.junit.After;
import org.junit.Assume;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.matcher.ViewMatchers.*;

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
