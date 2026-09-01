package com.glass.safeclip.ui.home

import org.junit.Assert.assertEquals
import org.junit.Test

class HomeStatusSummaryTest {
    @Test
    fun showsSafeClipSavedMediaCurrentFolderAndSubmissionCount() {
        val summary = HomeStatusSummary.from(
            savedMediaPermissionGranted = true,
            savedEventVideoCount = 3,
            savedEventPhotoCount = 5,
            currentFolderVideoCount = 12,
            currentFolderPhotoCount = 4,
            submissionCount = 2
        )

        assertEquals("SafeClip", summary.savedEventFolderName)
        assertEquals("영상 3개\n사진 5개", summary.savedEventCountText)
        assertEquals("영상 12개\n사진 4개", summary.currentFolderCountText)
        assertEquals("제출 2개", summary.submissionCountText)
    }

    @Test
    fun replacesIncompleteCountsWithPermissionGuidance() {
        val summary = HomeStatusSummary.from(
            savedMediaPermissionGranted = false,
            savedEventVideoCount = 0,
            savedEventPhotoCount = 2,
            currentFolderVideoCount = 12,
            currentFolderPhotoCount = 4
        )

        assertEquals("폴더 권한을\n추가해주세요", summary.savedEventCountText)
    }
}
