package com.glass.safeclip.data.submission

import com.glass.safeclip.domain.model.VideoCandidate
import com.glass.safeclip.ui.submission.SubmissionDraft
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SubmissionDocumentTest {
    private val video = VideoCandidate(
        uriString = "content://safeclip/video/1",
        displayName = "event.mp4",
        sizeBytes = 123_456L,
        lastModifiedMillis = 1_720_000_000_000L,
        folderPath = "현재 폴더"
    )

    private val draft = SubmissionDraft(
        incidentDateTime = "2026.07.15 14:20",
        locationText = "서울 강남구 테헤란로",
        incidentType = "추돌 사고",
        memo = "급정거 후 추돌",
        reviewConsent = true,
        storageConsent = true,
        dataUseConsent = true
    )

    @Test
    fun createsSubmissionFieldsForSignedInUser() {
        val input = SubmissionInput(
            ownerUid = "uid-123",
            video = video,
            draft = draft
        )

        val fields = SubmissionDocument.createFields(input)

        assertEquals("uid-123", fields["ownerUid"])
        assertEquals("waiting_review", fields["status"])
        assertEquals("event.mp4", fields["originalFileName"])
        assertEquals("content://safeclip/video/1", fields["sourceUri"])
        assertEquals("서울 강남구 테헤란로", fields["incidentLocationText"])
        assertEquals("추돌 사고", fields["violationTypeCandidate"])
        assertEquals(true, fields["reportReviewConsent"])
        assertTrue(fields.containsKey("createdAt"))
        assertTrue(fields.containsKey("updatedAt"))
    }

    @Test
    fun keepsOwnerUidEmptyForGuestSubmission() {
        val input = SubmissionInput(
            ownerUid = null,
            video = video,
            draft = draft,
            guestId = "Guest-ABCD-1234"
        )

        val fields = SubmissionDocument.createFields(input)

        assertNull(fields["ownerUid"])
        assertNull(fields["ownerDisplayName"])
        assertNull(fields["ownerEmail"])
        assertEquals("Guest-ABCD-1234", fields["guestId"])
    }

    @Test
    fun restoresLocalRecordFromFirestoreFields() {
        val input = SubmissionInput(
            ownerUid = "uid-123",
            video = video,
            draft = draft,
            guestId = "Guest-ABCD-1234",
            ownerDisplayName = "SafeClip User",
            ownerEmail = "safeclip@example.com"
        )
        val fields = SubmissionDocument.createFields(input)

        val record = SubmissionDocument.toLocalRecord(
            documentId = "submission-123",
            data = fields
        )

        assertEquals("submission-123", record.id)
        assertEquals("추돌 사고", record.title)
        assertEquals("event.mp4", record.video.displayName)
        assertEquals("content://safeclip/video/1", record.video.uriString)
        assertEquals("서울 강남구 테헤란로", record.locationText)
    }
}
