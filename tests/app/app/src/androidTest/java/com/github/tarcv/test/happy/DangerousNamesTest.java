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
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

@RunWith(Parameterized.class)
public class DangerousNamesTest {
    @Rule
    public ActivityTestRule<MainActivity> rule = new ActivityTestRule<>(
            MainActivity.class, true, true);

    public DangerousNamesTest(String param) {

    }

    @Test
    public void test() {
        TestHelpers.basicTestSteps();
    }

    @Parameters(name = "param = {0}")
    public static Object[] data() {
        return new Object[] {
                "$THIS_IS_NOT_A_VAR",
                "       1       ",
                "#######",
                "!!!!!!!",
                "'''''''",
                "\"\"\"\"\"\"\"\"",
                "()$(echo)`echo`()$(echo)`echo`()$(echo)`echo`()$(echo)`echo`()$(echo)`echo`" +
                        "()$(echo)`echo`()$(echo)`echo`",
                "* *.* * *.* * *.* * *.* * *.* * *.* * *.* * *.* *",
                ". .. . .. . .. . .. . .. . .. . .. . .. . .. . ..",
                "|&;<>()$`?[]#~=%|&;<>()$`?[]#~=%|&;<>()$`?[]#~=%|&;<>()$`?[]#~=%|&;<>()$`?[]#~=%" +
                        "|&;<>()$`?[]#~=%|&;<>()$`?[]#~=%",
                "Non-ASCII: ° © ± ¶ ½ » ѱ ∆",
                "; function {}; while {}; for {}; do {}; done {}; exit"
        };
    }
}
