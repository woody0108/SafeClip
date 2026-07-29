package com.glass.safeclip.ui.home

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HomeFolderTileActionsTest {
    @Test
    fun blackboxFolderViewIsPrimaryAfterFolderIsSelected() {
        val actions = HomeFolderTileActions.from(
            folderPermissionGranted = true,
            safeClipVideoCount = 0,
            safeClipPhotoCount = 0
        )

        assertTrue(actions.blackboxFolderViewIsPrimary)
    }

    @Test
    fun blackboxFolderViewIsNotPrimaryBeforeFolderIsSelected() {
        val actions = HomeFolderTileActions.from(
            folderPermissionGranted = false,
            safeClipVideoCount = 0,
            safeClipPhotoCount = 0
        )

        assertFalse(actions.blackboxFolderViewIsPrimary)
    }

    @Test
    fun safeClipFolderViewIsPrimaryWhenItHasAnyMedia() {
        val withVideo = HomeFolderTileActions.from(
            folderPermissionGranted = true,
            safeClipVideoCount = 1,
            safeClipPhotoCount = 0
        )
        val withPhoto = HomeFolderTileActions.from(
            folderPermissionGranted = true,
            safeClipVideoCount = 0,
            safeClipPhotoCount = 1
        )

        assertTrue(withVideo.safeClipFolderViewIsPrimary)
        assertTrue(withPhoto.safeClipFolderViewIsPrimary)
    }

    @Test
    fun safeClipFolderViewIsNotPrimaryWhenItIsEmpty() {
        val actions = HomeFolderTileActions.from(
            folderPermissionGranted = true,
            safeClipVideoCount = 0,
            safeClipPhotoCount = 0
        )

        assertFalse(actions.safeClipFolderViewIsPrimary)
    }
}
