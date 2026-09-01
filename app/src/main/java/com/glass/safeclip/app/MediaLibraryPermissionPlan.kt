package com.glass.safeclip.app

import android.Manifest
import android.os.Build

enum class MediaLibraryAccessLevel {
    Full,
    Limited,
    Denied
}

object MediaLibraryPermissionPlan {
    fun requiredPermissions(sdkInt: Int): List<String> {
        return when {
            sdkInt >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE -> listOf(
                Manifest.permission.READ_MEDIA_IMAGES,
                Manifest.permission.READ_MEDIA_VIDEO,
                Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED
            )
            sdkInt >= Build.VERSION_CODES.TIRAMISU -> listOf(
                Manifest.permission.READ_MEDIA_IMAGES,
                Manifest.permission.READ_MEDIA_VIDEO
            )
            else -> listOf(Manifest.permission.READ_EXTERNAL_STORAGE)
        }
    }

    fun accessLevel(
        sdkInt: Int,
        hasPermission: (String) -> Boolean
    ): MediaLibraryAccessLevel {
        if (sdkInt < Build.VERSION_CODES.TIRAMISU) {
            return if (hasPermission(Manifest.permission.READ_EXTERNAL_STORAGE)) {
                MediaLibraryAccessLevel.Full
            } else {
                MediaLibraryAccessLevel.Denied
            }
        }

        val hasFullAccess = hasPermission(Manifest.permission.READ_MEDIA_IMAGES) &&
            hasPermission(Manifest.permission.READ_MEDIA_VIDEO)
        if (hasFullAccess) return MediaLibraryAccessLevel.Full

        val hasLimitedAccess = sdkInt >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE &&
            hasPermission(Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED)
        return if (hasLimitedAccess) {
            MediaLibraryAccessLevel.Limited
        } else {
            MediaLibraryAccessLevel.Denied
        }
    }

    fun isGranted(
        sdkInt: Int,
        hasPermission: (String) -> Boolean
    ): Boolean = accessLevel(sdkInt, hasPermission) != MediaLibraryAccessLevel.Denied
}