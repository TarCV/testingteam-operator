package com.github.tarcv.test;

import android.content.pm.PackageManager;
import android.support.test.rule.ActivityTestRule;
import android.support.v4.content.ContextCompat;
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
