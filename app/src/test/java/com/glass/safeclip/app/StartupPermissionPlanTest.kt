package com.glass.safeclip.app

import android.os.Build
import org.junit.Assert.assertTrue
import org.junit.Test

class StartupPermissionPlanTest {
    @Test
    fun currentAndroidDoesNotRequestPermissionsDuringStartup() {
        val permissions = StartupPermissionPlan.missingPermissions(
            sdkInt = Build.VERSION_CODES.VANILLA_ICE_CREAM,
            isGranted = { false }
        )

        assertTrue(permissions.isEmpty())
    }

    @Test
    fun legacyAndroidDoesNotRequestPermissionsDuringStartup() {
        val permissions = StartupPermissionPlan.missingPermissions(
            sdkInt = Build.VERSION_CODES.P,
            isGranted = { false }
        )

        assertTrue(permissions.isEmpty())
    }
}
