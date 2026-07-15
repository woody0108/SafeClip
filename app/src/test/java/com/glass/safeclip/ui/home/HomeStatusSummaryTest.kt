package com.glass.safeclip.ui.home

import org.junit.Assert.assertEquals
import org.junit.Test

class HomeStatusSummaryTest {
    @Test
    fun showsSafeClipSavedMediaCurrentFolderAndSubmissionCount() {
        val summary = HomeStatusSummary.from(
            savedMediaItemCount = 3,
            currentFolderVideoCount = 12,
            submissionCount = 2
        )

        assertEquals("SafeClip Captures", summary.savedEventFolderName)
        assertEquals("3개", summary.savedEventCountText)
        assertEquals("12개", summary.currentFolderCountText)
        assertEquals("제출 2개", summary.submissionCountText)
    }
}
