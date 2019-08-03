package com.github.tarcv.test

import android.support.test.filters.SdkSuppress
import android.support.test.rule.ActivityTestRule
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@RunWith(Parameterized::class)
class FilteredTest(param: Int) {
    @get:Rule
    var rule: ActivityTestRule<MainActivity> = ActivityTestRule(
            MainActivity::class.java, true, true)

    @Test
    @SdkSuppress(minSdkVersion = 22, maxSdkVersion = 22)
    fun api22Only() {
        TestHelpers.basicTestSteps()
    }

    @Test
    fun filteredByF2Filter() {
        TestHelpers.basicTestSteps()
    }

/*
    @Test
    @SdkSuppress(minSdkVersion = 18, maxSdkVersion = 18)
    fun notExecuted() {
        TestHelpers.basicTestSteps()
    }

    @Test
    fun filterUsingAssumption() {
        Assume.assumeTrue(Build.VERSION.SDK_INT == 22)
    }

    @Test
    fun assumptionFailedIsMarkedSkipped() {
        Assume.assumeTrue(false)
    }
*/
    companion object {
        @Parameterized.Parameters
        @JvmStatic
        fun data(): Array<Any> {
            return arrayOf(1, 2, 3, 4)
        }
    }
}