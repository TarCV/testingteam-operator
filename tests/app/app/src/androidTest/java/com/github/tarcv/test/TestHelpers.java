package com.github.tarcv.test;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.matcher.ViewMatchers.*;
import static com.github.tarcv.test.Config.TEST_DURATION;
import static org.hamcrest.core.AllOf.allOf;

public class TestHelpers {
    public static void basicTestSteps() {
        onView(withId(R.id.hello_text))
                .check(matches(allOf(
                        isDisplayed(),
                        withText("Hello World!")
                )));

        try {
            Thread.sleep(TEST_DURATION);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
