/*
 * Copyright 2020 TarCV
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License.
 *
 * You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */

package com.github.tarcv.test.happy

import androidx.test.filters.SdkSuppress
import androidx.test.rule.ActivityTestRule
import com.github.tarcv.test.MainActivity
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
*/
    companion object {
        @Parameterized.Parameters
        @JvmStatic
        fun data(): Array<Any> {
            return arrayOf(1, 2, 3, 4)
        }
    }
}