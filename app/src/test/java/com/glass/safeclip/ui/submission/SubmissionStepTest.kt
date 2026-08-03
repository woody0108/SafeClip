package com.glass.safeclip.ui.submission

import com.glass.safeclip.data.submission.SubmissionAttachment
import com.glass.safeclip.data.submission.SubmissionAttachmentKind
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SubmissionStepTest {
    @Test
    fun fileStepCanContinueWhenAttachmentsAreValid() {
        assertTrue(SubmissionStep.Files.canContinue(readyDraftWithoutConsent(), listOf(attachment())))
    }

    @Test
    fun fileStepCannotContinueWhenAttachmentsAreInvalid() {
        assertFalse(
            SubmissionStep.Files.canContinue(
                readyDraftWithoutConsent(),
                listOf(attachment(sizeBytes = 501L * 1024L * 1024L))
            )
        )
    }

    @Test
    fun detailsStepCanOpenConsentWhenAttachmentsAreValid() {
        assertTrue(SubmissionStep.Details.canContinue(readyDraftWithoutConsent(), listOf(attachment())))
    }

    @Test
    fun detailsStepCanOpenConsentWhenIncidentInfoIsMissing() {
        assertTrue(SubmissionStep.Details.canContinue(SubmissionDraft(), listOf(attachment())))
    }

    @Test
    fun detailsStepCannotOpenConsentWhenAttachmentsAreInvalid() {
        assertFalse(
            SubmissionStep.Details.canContinue(
                SubmissionDraft(),
                listOf(attachment(sizeBytes = 501L * 1024L * 1024L))
            )
        )
    }

    private fun readyDraftWithoutConsent(): SubmissionDraft {
        return SubmissionDraft(
            incidentDateTime = "2026.07.30 12:30",
            locationText = "서울 강남구",
            incidentType = "추돌",
            memo = "메모"
        )
    }

    private fun attachment(sizeBytes: Long = 100L): SubmissionAttachment {
        return SubmissionAttachment(
            uriString = "content://video",
            displayName = "event.mp4",
            mimeType = "video/mp4",
            sizeBytes = sizeBytes,
            folderPath = "블랙박스 폴더",
            kind = SubmissionAttachmentKind.Video
        )
    }
}
