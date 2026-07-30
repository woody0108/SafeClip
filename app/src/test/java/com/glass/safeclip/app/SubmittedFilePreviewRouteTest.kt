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

    @Test
    fun `opens nas submission video through review video api`() {
        val screen = SubmittedFilePreviewRoute.from(
            record = recordWithFileName(
                fileName = "event.mp4",
                uriString = "2026/07/30/submitter/01/event.mp4"
            ),
            nasUploadUrl = "http://192.168.0.3:8080/api/nas-upload-api/public/upload.php"
        )

        assertTrue(screen is SafeClipScreen.VideoPreview)
        assertEquals(
            "http://192.168.0.3:8080/api/video.php?id=submission-1&file=0",
            (screen as SafeClipScreen.VideoPreview).video.uriString
        )
    }

    private fun recordWithFileName(
        fileName: String,
        uriString: String = "content://safeclip/$fileName"
    ): LocalSubmissionRecord {
        return LocalSubmissionRecord(
            id = "submission-1",
            video = VideoCandidate(
                uriString = uriString,
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
