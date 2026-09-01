package com.glass.safeclip.ui.home

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class HomeImportActionsTest {
    @Test
    fun folderSelectionIsPrimaryWhenFolderPermissionIsOff() {
        val actions = HomeImportActions.from(
            selectedFolderName = null,
            folderPermissionGranted = false,
            cameraPermissionGranted = true
        )

        assertEquals("폴더 설정 열기", actions.folderButtonText)
        assertTrue(actions.folderButtonIsPrimary)
        assertFalse(actions.showRecentButton)
        assertFalse(actions.recentButtonIsPrimary)
        assertFalse(actions.recentButtonEnabled)
        assertTrue(actions.eventFolderEnabled)
        assertNull(actions.selectedFolderText)
    }

    @Test
    fun recentEventsAndEventFolderAreEnabledOnlyWhenBothPermissionsAreOn() {
        val actions = HomeImportActions.from(
            selectedFolderName = "BLACKBOX/EVENT",
            folderPermissionGranted = true,
            cameraPermissionGranted = true
        )

        assertFalse(actions.folderButtonIsPrimary)
        assertFalse(actions.showRecentButton)
        assertTrue(actions.recentButtonIsPrimary)
        assertTrue(actions.recentButtonEnabled)
        assertTrue(actions.eventFolderEnabled)
        assertEquals("선택된 폴더: BLACKBOX/EVENT", actions.selectedFolderText)
    }

    @Test
    fun generatedMediaFolderStaysEnabledWithoutCameraPermission() {
        val actions = HomeImportActions.from(
            selectedFolderName = "BLACKBOX/EVENT",
            folderPermissionGranted = true,
            cameraPermissionGranted = false
        )

        assertFalse(actions.folderButtonIsPrimary)
        assertFalse(actions.showRecentButton)
        assertFalse(actions.recentButtonIsPrimary)
        assertFalse(actions.recentButtonEnabled)
        assertTrue(actions.eventFolderEnabled)
    }
}
