package com.glass.safeclip.data.submission

import com.glass.safeclip.domain.model.VideoCandidate
import com.glass.safeclip.data.profile.UserProfile
import com.glass.safeclip.ui.submission.SubmissionDraft
import com.google.firebase.Timestamp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
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
        locationDetail = "3차로 버스정류장 앞",
        locationLatitude = 37.5012,
        locationLongitude = 127.0396,
        locationSource = "map_selected",
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
        assertEquals("검토 대기 중", fields["status"])
        assertEquals("2026.07.15", fields["incidentDate"])
        assertEquals("14:20", fields["incidentTime"])
        assertEquals("서울 강남구 테헤란로", fields["incidentLocation"])
        assertEquals("3차로 버스정류장 앞", fields["incidentLocationDetail"])
        assertEquals(37.5012, fields["incidentLatitude"])
        assertEquals(127.0396, fields["incidentLongitude"])
        assertEquals("map_selected", fields["incidentLocationSource"])
        assertEquals("추돌 사고", fields["reportType"])
        assertEquals("급정거 후 추돌", fields["reportMemo"])
        assertEquals("", fields["companyComment"])
        assertEquals(true, fields["reviewConsent"])
        assertTrue(fields.containsKey("submittedAt"))
        assertTrue(fields.containsKey("updatedAt"))
        assertFalse(fields.containsKey("createdAt"))
        assertFalse(fields.containsKey("ownerEmail"))
        assertFalse(fields.containsKey("sourceUri"))
        assertFalse(fields.containsKey("originalFileName"))
        assertFalse(fields.containsKey("fileSizeBytes"))
        assertFalse(fields.containsKey("originalFolderPath"))
        assertFalse(fields.containsKey("originalLastModifiedMillis"))
        assertFalse(fields.containsKey("incidentDateTime"))
        assertFalse(fields.containsKey("incidentLocationText"))
        assertFalse(fields.containsKey("violationTypeCandidate"))
        assertFalse(fields.containsKey("userMemo"))
        assertFalse(fields.containsKey("reportReviewConsent"))
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

        assertFalse(fields.containsKey("videoCount"))
        assertFalse(fields.containsKey("photoCount"))
        val attachments = fields["attachments"] as List<Map<String, Any?>>
        assertEquals("front.mp4", attachments[0]["displayName"])
        assertEquals("video", attachments[0]["kind"])
        assertFalse(attachments[0].containsKey("uriString"))
        assertFalse(attachments[0].containsKey("folderPath"))
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
            nasRelativePath = "2026/07/29/베짱이들/01/stored.mp4",
            uploadedSizeBytes = 1234L,
            nasSubmissionFolder = "2026/07/29/베짱이들/01",
            submissionSequence = 1,
            submissionSequenceText = "01"
        )
        val input = SubmissionInput(
            ownerUid = null,
            video = video,
            draft = draft,
            guestId = "guest-1",
            nasSubmissionFolder = uploaded.nasSubmissionFolder,
            submissionSequence = uploaded.submissionSequence,
            submissionSequenceText = uploaded.submissionSequenceText,
            attachments = listOf(uploaded)
        )

        val fields = SubmissionDocument.createFields(input)

        assertEquals("2026/07/29/베짱이들/01", fields["nasSubmissionFolder"])
        assertEquals(1, fields["submissionSequence"])
        assertEquals("01", fields["submissionSequenceText"])
        assertFalse(fields.containsKey("nasRelativePath"))
        assertFalse(fields.containsKey("nasFiles"))
        val attachments = fields["attachments"] as List<Map<String, Any?>>
        assertFalse(attachments[0].containsKey("nasStoredName"))
        assertEquals("2026/07/29/베짱이들/01/stored.mp4", attachments[0]["nasRelativePath"])
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
        assertFalse(fields.containsKey("ownerEmail"))
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
            attachments = listOf(
                attachment("content://front", "front.mp4", "video/mp4", SubmissionAttachmentKind.Video).copy(
                    nasRelativePath = "2026/07/29/front.mp4"
                )
            )
        )
        val fields = SubmissionDocument.createFields(input)

        val record = SubmissionDocument.toLocalRecord(
            documentId = "submission-123",
            data = fields
        )

        assertEquals("submission-123", record.id)
        assertEquals("추돌 사고", record.title)
        assertEquals("front.mp4", record.video.displayName)
        assertEquals("2026/07/29/front.mp4", record.video.uriString)
        assertEquals("서울 강남구 테헤란로", record.locationText)
    }

    @Test
    fun restoresSubmittedAtTextFromSubmittedAtTimestamp() {
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
            fields["submittedAt"] = Timestamp(submittedAt.time)

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
        assertFalse(fields.containsKey("ownerEmail"))
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
