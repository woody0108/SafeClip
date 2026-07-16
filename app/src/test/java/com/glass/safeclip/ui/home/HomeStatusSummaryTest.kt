package com.glass.safeclip.ui.home

import org.junit.Assert.assertEquals
import org.junit.Test

class HomeStatusSummaryTest {
    @Test
    fun showsSafeClipSavedMediaCurrentFolderAndSubmissionCount() {
        val summary = HomeStatusSummary.from(
            savedEventVideoCount = 3,
            savedEventPhotoCount = 5,
            currentFolderVideoCount = 12,
            currentFolderPhotoCount = 4,
            submissionCount = 2
        )

        assertEquals("SafeClip Captures", summary.savedEventFolderName)
        assertEquals("영상 3개\n사진 5개", summary.savedEventCountText)
        assertEquals("영상 12개\n사진 4개", summary.currentFolderCountText)
        assertEquals("제출 2개", summary.submissionCountText)
    }
}
