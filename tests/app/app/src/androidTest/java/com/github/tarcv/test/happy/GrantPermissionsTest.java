package com.github.tarcv.test.happy;

import android.content.pm.PackageManager;
import android.os.Build;
import android.support.test.rule.ActivityTestRule;
import android.support.v4.content.ContextCompat;
import com.github.tarcv.test.MainActivity;
import com.github.tarcv.tongs.GrantPermission;
import org.junit.Rule;
import org.junit.Test;

import static android.Manifest.permission;
import static android.os.Build.VERSION_CODES.M;
import static android.test.MoreAsserts.assertNotEqual;
import static org.junit.Assert.assertEquals;

public class GrantPermissionsTest {
    @Rule
    public ActivityTestRule<MainActivity> rule = new ActivityTestRule<>(
            MainActivity.class, true, true);

    @Test
    @GrantPermission({permission.WRITE_CALENDAR})
    public void testPermissionGranted() {
        assertEquals(PackageManager.PERMISSION_GRANTED,
                ContextCompat.checkSelfPermission(rule.getActivity(), permission.WRITE_CALENDAR));

        TestHelpers.basicTestSteps();
    }

    @Test
    public void testNoPermissionByDefault() {
        if (Build.VERSION.SDK_INT >= M) {
            assertNotEqual(PackageManager.PERMISSION_GRANTED,
                    ContextCompat.checkSelfPermission(rule.getActivity(), permission.WRITE_CALENDAR));
        }
        TestHelpers.basicTestSteps();
    }
}
