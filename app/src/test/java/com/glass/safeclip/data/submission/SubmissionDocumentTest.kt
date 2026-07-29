package com.glass.safeclip.data.submission

import com.glass.safeclip.domain.model.VideoCandidate
import com.glass.safeclip.data.profile.UserProfile
import com.glass.safeclip.ui.submission.SubmissionDraft
import com.google.firebase.Timestamp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar
import java.util.TimeZone

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
    fun createFieldsStoresAttachmentListAndCounts() {
        val input = SubmissionInput(
            ownerUid = null,
            video = video,
            draft = draft,
            guestId = "guest-1",
            attachments = listOf(
                attachment("content://front", "front.mp4", "video/mp4", SubmissionAttachmentKind.Video),
                attachment("content://plate", "plate.jpg", "image/jpeg", SubmissionAttachmentKind.Photo)
            )
        )

        val fields = SubmissionDocument.createFields(input)

        assertEquals(1, fields["videoCount"])
        assertEquals(1, fields["photoCount"])
        val attachments = fields["attachments"] as List<Map<String, Any?>>
        assertEquals("front.mp4", attachments[0]["displayName"])
        assertEquals("video", attachments[0]["kind"])
        assertEquals("plate.jpg", attachments[1]["displayName"])
        assertEquals("photo", attachments[1]["kind"])
    }

    @Test
    fun createFieldsStoresNasUploadPaths() {
        val uploaded = attachment(
            uriString = "content://front",
            displayName = "front.mp4",
            mimeType = "video/mp4",
            kind = SubmissionAttachmentKind.Video
        ).copy(
            nasStoredName = "stored.mp4",
            nasRelativePath = "2026/07/29/stored.mp4",
            uploadedSizeBytes = 1234L
        )
        val input = SubmissionInput(
            ownerUid = null,
            video = video,
            draft = draft,
            guestId = "guest-1",
            attachments = listOf(uploaded)
        )

        val fields = SubmissionDocument.createFields(input)

        assertEquals("2026/07/29/stored.mp4", fields["nasRelativePath"])
        val attachments = fields["attachments"] as List<Map<String, Any?>>
        assertEquals("stored.mp4", attachments[0]["nasStoredName"])
        assertEquals("2026/07/29/stored.mp4", attachments[0]["nasRelativePath"])
        assertEquals(1234L, attachments[0]["uploadedSizeBytes"])
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

    @Test
    fun restoresSubmittedAtTextFromCreatedAtTimestamp() {
        val previousTimeZone = TimeZone.getDefault()
        TimeZone.setDefault(TimeZone.getTimeZone("Asia/Seoul"))
        try {
            val input = SubmissionInput(
                ownerUid = "uid-123",
                video = video,
                draft = draft
            )
            val submittedAt = Calendar.getInstance(TimeZone.getTimeZone("Asia/Seoul")).apply {
                set(2026, Calendar.JULY, 15, 13, 5, 0)
                set(Calendar.MILLISECOND, 0)
            }
            val fields = SubmissionDocument.createFields(input).toMutableMap()
            fields["createdAt"] = Timestamp(submittedAt.time)

            val record = SubmissionDocument.toLocalRecord(
                documentId = "submission-123",
                data = fields
            )

            assertEquals("2026-07-15 13:05", record.submittedAtText)
        } finally {
            TimeZone.setDefault(previousTimeZone)
        }
    }

    @Test
    fun createsGuestSubmissionOwnerLinkFieldsFromUserProfile() {
        val profile = UserProfile(
            uid = "uid-123",
            guestId = "Guest-ABCD-1234",
            email = "safeclip@example.com",
            displayName = "SafeClip User",
            provider = "google"
        )

        val fields = SubmissionDocument.guestOwnerLinkFields(profile)

        assertEquals("uid-123", fields["ownerUid"])
        assertEquals("Guest-ABCD-1234", fields["guestId"])
        assertEquals("SafeClip User", fields["ownerDisplayName"])
        assertEquals("safeclip@example.com", fields["ownerEmail"])
        assertTrue(fields.containsKey("updatedAt"))
    }

    @Test
    fun usesEmailAsOwnerDisplayNameWhenProfileHasNoDisplayName() {
        val profile = UserProfile(
            uid = "uid-123",
            guestId = "Guest-ABCD-1234",
            email = "safeclip@example.com",
            displayName = null,
            provider = "email"
        )

        val fields = SubmissionDocument.guestOwnerLinkFields(profile)

        assertEquals("safeclip@example.com", fields["ownerDisplayName"])
    }

    private fun attachment(
        uriString: String,
        displayName: String,
        mimeType: String,
        kind: SubmissionAttachmentKind
    ): SubmissionAttachment {
        return SubmissionAttachment(
            uriString = uriString,
            displayName = displayName,
            mimeType = mimeType,
            sizeBytes = 100L,
            folderPath = "현재 폴더",
            kind = kind
        )
    }
}
