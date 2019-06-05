package com.github.tarcv.test;

import android.support.test.rule.ActivityTestRule;
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
