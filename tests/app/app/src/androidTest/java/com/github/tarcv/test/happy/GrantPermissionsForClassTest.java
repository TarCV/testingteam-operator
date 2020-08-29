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

import android.content.pm.PackageManager;
import androidx.test.rule.ActivityTestRule;
import androidx.core.content.ContextCompat;
import com.github.tarcv.test.MainActivity;
import com.github.tarcv.tongs.GrantPermission;
import org.junit.Rule;
import org.junit.Test;

import static android.Manifest.permission;
import static org.junit.Assert.assertEquals;

@GrantPermission({permission.WRITE_CALENDAR})
public class GrantPermissionsForClassTest {
    @Rule
    public ActivityTestRule<MainActivity> rule = new ActivityTestRule<>(
            MainActivity.class, true, true);

    @Test
    public void testPermissionGranted1() {
        assertEquals(PackageManager.PERMISSION_GRANTED,
                ContextCompat.checkSelfPermission(rule.getActivity(), permission.WRITE_CALENDAR));

        TestHelpers.basicTestSteps();
    }

    @Test
    public void testPermissionGranted2() {
        assertEquals(PackageManager.PERMISSION_GRANTED,
                ContextCompat.checkSelfPermission(rule.getActivity(), permission.WRITE_CALENDAR));

        TestHelpers.basicTestSteps();
    }
}
