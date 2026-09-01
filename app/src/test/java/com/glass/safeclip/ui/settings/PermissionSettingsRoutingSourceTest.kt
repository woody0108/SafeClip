package com.glass.safeclip.ui.settings

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PermissionSettingsRoutingSourceTest {
    private val activitySource = File("src/main/java/com/glass/safeclip/MainActivity.kt").readText()
    private val homeRoute = File("src/main/java/com/glass/safeclip/MainActivity.kt")
        .readText()
        .substringAfter("SafeClipScreen.Home -> MainHomeScreen(")
        .substringBefore("SafeClipScreen.LiveRecording ->")

    @Test
    fun folderPermissionActionOpensSettingsInsteadOfPicker() {
        val action = homeRoute.substringAfter("onOpenFolderPermissionSettings = {")
            .substringBefore("},")

        assertTrue(action.contains("screen = SafeClipScreen.Settings()"))
        assertFalse(action.contains("folderPicker.launch"))
    }

    @Test
    fun homeDoesNotExposeCameraPermissionCardAction() {
        assertFalse(homeRoute.contains("onOpenCameraPermissionSettings"))
        assertFalse(homeRoute.contains("cameraPermissionLauncher.launch"))
    }

    @Test
    fun missingCameraPermissionRoutesLiveRecordingEntryToSettings() {
        val action = homeRoute.substringAfter("onOpenLiveRecording = {")
            .substringBefore("},")

        assertTrue(action.contains("screen = SafeClipScreen.Settings()"))
        assertFalse(action.contains("cameraPermissionLauncher.launch"))
    }

    @Test
    fun mediaLibraryPermissionCanBeRequestedDirectlyFromHome() {
        val action = homeRoute
            .substringAfter("onRequestMediaLibraryPermission = {")
            .substringBefore("},")

        assertTrue(action.contains("MediaLibraryPermissionPlan.requiredPermissions"))
        assertTrue(action.contains("settingsPermissionLauncher.launch"))
    }

    @Test
    fun grantingAnySettingsPermissionImmediatelyReloadsSavedMedia() {
        val launcher = activitySource
            .substringAfter("val settingsPermissionLauncher =")
            .substringBefore("val permissionLifecycleOwner")

        assertTrue(launcher.contains("refreshRuntimePermissions()"))
        assertTrue(launcher.contains("refreshSavedMediaCountAsync()"))
    }
}
