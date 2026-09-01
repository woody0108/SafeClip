package com.glass.safeclip.ui.settings

import org.junit.Assert.assertEquals
import org.junit.Test

class SettingsPermissionItemsTest {
    @Test
    fun createsPermissionItemsInUserFacingOrder() {
        val items = SettingsPermissionItems.from(
            folderGranted = true,
            mediaLibraryGranted = false,
            cameraGranted = false,
            microphoneGranted = true,
            locationGranted = false
        )

        assertEquals(
            listOf("저장 폴더", "저장된 사진·영상", "카메라", "마이크", "위치"),
            items.map { it.label }
        )
        assertEquals(listOf(true, false, false, true, false), items.map { it.granted })
    }

    @Test
    fun permissionSummaryCountsGrantedItems() {
        val items = SettingsPermissionItems.from(
            folderGranted = true,
            mediaLibraryGranted = true,
            cameraGranted = true,
            microphoneGranted = false,
            locationGranted = false
        )

        assertEquals("권한 5개 중 3개 사용 가능", SettingsPermissionItems.summary(items))
    }
}
