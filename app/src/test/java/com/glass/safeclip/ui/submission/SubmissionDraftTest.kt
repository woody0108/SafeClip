package com.glass.safeclip.ui.submission

import com.glass.safeclip.data.submission.SubmissionAttachment
import com.glass.safeclip.data.submission.SubmissionAttachmentKind
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SubmissionDraftTest {
    @Test
    fun draftIsNotReadyWhenRequiredFieldsAreMissing() {
        assertFalse(SubmissionDraft().isReadyToSubmit)
    }

    @Test
    fun draftIsReadyWhenFieldsAndConsentAreComplete() {
        val draft = SubmissionDraft(
            incidentDateTime = "2026.07.14 10:30",
            locationText = "서울 강남구 테헤란로",
            incidentType = "추돌 사고",
            memo = "급정거 후 추돌",
            reviewConsent = true,
            storageConsent = true,
            dataUseConsent = true
        )

        assertTrue(draft.isReadyToSubmit)
    }

    @Test
    fun submissionRequiresAtLeastOneAttachment() {
        val draft = readyDraft()

        assertFalse(draft.canSubmitWith(emptyList()))
    }

    @Test
    fun submissionCanSendReadyDraftWithAttachment() {
        val draft = readyDraft()

        assertTrue(draft.canSubmitWith(listOf(attachment())))
    }

    @Test
    fun incidentDateTimeInputKeepsOnlyDateTimeCharacters() {
        val sanitized = SubmissionDraft.sanitizeIncidentDateTimeInput("2026년 07월 29일 오후 3시:10분 abc!?")

        assertTrue(sanitized.all { it.isDigit() || it == '.' || it == ':' || it == '-' || it == '/' || it == ' ' })
        assertFalse(sanitized.contains("년"))
        assertFalse(sanitized.contains("abc"))
    }

    private fun readyDraft(): SubmissionDraft {
        return SubmissionDraft(
            incidentDateTime = "2026.07.14 10:30",
            locationText = "서울 강남구 테헤란로",
            incidentType = "추돌 사고",
            memo = "급정거 후 추돌",
            reviewConsent = true,
            storageConsent = true,
            dataUseConsent = true
        )
    }

    private fun attachment(): SubmissionAttachment {
        return SubmissionAttachment(
            uriString = "content://video",
            displayName = "event.mp4",
            mimeType = "video/mp4",
            sizeBytes = 100L,
            folderPath = "현재 폴더",
            kind = SubmissionAttachmentKind.Video
        )
    }
}
