package com.glass.safeclip.app

import com.glass.safeclip.domain.model.VideoCandidate
import com.glass.safeclip.ui.navigation.SafeClipScreen
import com.glass.safeclip.ui.status.LocalSubmissionRecord
import com.glass.safeclip.ui.status.SubmissionStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SubmittedFilePreviewRouteTest {
    @Test
    fun `opens jpeg submission as image preview and returns to status`() {
        val screen = SubmittedFilePreviewRoute.from(recordWithFileName("capture.jpg"))

        assertTrue(screen is SafeClipScreen.ImagePreview)
        assertEquals(SafeClipScreen.SubmissionStatus, (screen as SafeClipScreen.ImagePreview).returnScreen)
        assertEquals("image/jpeg", screen.file.mimeType)
    }

    @Test
    fun `opens video submission as video preview and returns to status`() {
        val screen = SubmittedFilePreviewRoute.from(recordWithFileName("event.mp4"))

        assertTrue(screen is SafeClipScreen.VideoPreview)
        assertEquals(SafeClipScreen.SubmissionStatus, (screen as SafeClipScreen.VideoPreview).returnScreen)
    }

    private fun recordWithFileName(fileName: String): LocalSubmissionRecord {
        return LocalSubmissionRecord(
            id = "submission-1",
            video = VideoCandidate(
                uriString = "content://safeclip/$fileName",
                displayName = fileName,
                sizeBytes = 100L,
                lastModifiedMillis = null,
                folderPath = "SafeClip"
            ),
            title = "test",
            incidentDateTime = "2026-07-16 10:00",
            locationText = "Seoul",
            incidentType = "accident",
            memo = "",
            status = SubmissionStatus.WaitingReview
        )
    }
}
