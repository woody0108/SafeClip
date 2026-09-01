package com.glass.safeclip.app

import android.Manifest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MediaLibraryPermissionPlanTest {
    @Test
    fun android13AndLaterRequiresImageAndVideoReadPermissions() {
        assertEquals(
            listOf(
                Manifest.permission.READ_MEDIA_IMAGES,
                Manifest.permission.READ_MEDIA_VIDEO
            ),
            MediaLibraryPermissionPlan.requiredPermissions(sdkInt = 33)
        )
    }

    @Test
    fun android14AndLaterAlsoRequestsSelectedMediaPermission() {
        assertEquals(
            listOf(
                Manifest.permission.READ_MEDIA_IMAGES,
                Manifest.permission.READ_MEDIA_VIDEO,
                Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED
            ),
            MediaLibraryPermissionPlan.requiredPermissions(sdkInt = 34)
        )
    }

    @Test
    fun selectedMediaPermissionIsRecognizedAsLimitedAccess() {
        val access = MediaLibraryPermissionPlan.accessLevel(sdkInt = 34) { permission ->
            permission == Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED
        }

        assertEquals(MediaLibraryAccessLevel.Limited, access)
        assertTrue(MediaLibraryPermissionPlan.isGranted(sdkInt = 34) { permission ->
            permission == Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED
        })
    }

    @Test
    fun imageAndVideoPermissionsAreRecognizedAsFullAccess() {
        val access = MediaLibraryPermissionPlan.accessLevel(sdkInt = 34) { permission ->
            permission == Manifest.permission.READ_MEDIA_IMAGES ||
                permission == Manifest.permission.READ_MEDIA_VIDEO
        }

        assertEquals(MediaLibraryAccessLevel.Full, access)
    }

    @Test
    fun android12AndEarlierRequiresSharedStorageReadPermission() {
        assertEquals(
            listOf(Manifest.permission.READ_EXTERNAL_STORAGE),
            MediaLibraryPermissionPlan.requiredPermissions(sdkInt = 32)
        )
    }

    @Test
    fun mediaLibraryIsGrantedOnlyWhenEveryRequiredPermissionIsGranted() {
        assertTrue(
            MediaLibraryPermissionPlan.isGranted(sdkInt = 33) { true }
        )
        assertFalse(
            MediaLibraryPermissionPlan.isGranted(sdkInt = 33) { permission ->
                permission == Manifest.permission.READ_MEDIA_VIDEO
            }
        )
    }
}
