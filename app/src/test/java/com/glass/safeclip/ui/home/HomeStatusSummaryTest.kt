package com.glass.safeclip.ui.home

import org.junit.Assert.assertEquals
import org.junit.Test

class HomeStatusSummaryTest {
    @Test
    fun showsSafeClipSavedMediaCountAndCurrentFolderVideoCount() {
        val summary = HomeStatusSummary.from(
            savedMediaItemCount = 3,
            currentFolderVideoCount = 12
        )

        assertEquals("SafeClip Captures", summary.savedEventFolderName)
        assertEquals("3개", summary.savedEventCountText)
        assertEquals("12개", summary.currentFolderCountText)
    }
}
