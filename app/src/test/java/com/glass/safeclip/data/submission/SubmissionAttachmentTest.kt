package com.glass.safeclip.data.submission

import com.glass.safeclip.data.file.ManagedFolderFile
import com.glass.safeclip.domain.model.VideoCandidate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SubmissionAttachmentTest {
    @Test
    fun addBlocksMoreThanTwoVideos() {
        val first = attachment("content://video-1", "front.mp4", "video/mp4", SubmissionAttachmentKind.Video)
        val second = attachment("content://video-2", "rear.mp4", "video/mp4", SubmissionAttachmentKind.Video)
        val third = attachment("content://video-3", "side.mp4", "video/mp4", SubmissionAttachmentKind.Video)

        val result = SubmissionAttachmentRules.add(listOf(first, second), third)

        assertEquals(AttachmentSelectionResult.Rejected("영상은 최대 2개까지 첨부할 수 있습니다."), result)
    }

    @Test
    fun addBlocksMoreThanFivePhotos() {
        val photos = (1..5).map { index ->
            attachment("content://photo-$index", "plate-$index.jpg", "image/jpeg", SubmissionAttachmentKind.Photo)
        }
        val sixth = attachment("content://photo-6", "plate-6.jpg", "image/jpeg", SubmissionAttachmentKind.Photo)

        val result = SubmissionAttachmentRules.add(photos, sixth)

        assertEquals(AttachmentSelectionResult.Rejected("사진은 최대 5개까지 첨부할 수 있습니다."), result)
    }

    @Test
    fun addBlocksDuplicateUri() {
        val first = attachment("content://same", "front.mp4", "video/mp4", SubmissionAttachmentKind.Video)
        val duplicate = attachment("content://same", "front-copy.mp4", "video/mp4", SubmissionAttachmentKind.Video)

        val result = SubmissionAttachmentRules.add(listOf(first), duplicate)

        assertEquals(AttachmentSelectionResult.Rejected("이미 첨부한 파일입니다."), result)
    }

    @Test
    fun addBlocksVideoAtFiveHundredMbOrMore() {
        val video = attachment(
            uriString = "content://large-video",
            displayName = "large.mp4",
            mimeType = "video/mp4",
            kind = SubmissionAttachmentKind.Video,
            sizeBytes = 500L * 1024L * 1024L
        )

        val result = SubmissionAttachmentRules.add(emptyList(), video)

        assertEquals(AttachmentSelectionResult.Rejected("영상은 500MB 미만 파일만 첨부할 수 있습니다."), result)
    }

    @Test
    fun addBlocksPhotoAtFiftyMbOrMore() {
        val photo = attachment(
            uriString = "content://large-photo",
            displayName = "large.jpg",
            mimeType = "image/jpeg",
            kind = SubmissionAttachmentKind.Photo,
            sizeBytes = 50L * 1024L * 1024L
        )

        val result = SubmissionAttachmentRules.add(emptyList(), photo)

        assertEquals(AttachmentSelectionResult.Rejected("사진은 50MB 미만 파일만 첨부할 수 있습니다."), result)
    }

    @Test
    fun filterShowsOnlyRequestedAttachmentKind() {
        val video = attachment("content://video", "front.mp4", "video/mp4", SubmissionAttachmentKind.Video)
        val photo = attachment("content://photo", "plate.jpg", "image/jpeg", SubmissionAttachmentKind.Photo)

        assertEquals(listOf(video, photo), SubmissionAttachmentFilter.All.apply(listOf(video, photo)))
        assertEquals(listOf(photo), SubmissionAttachmentFilter.Photos.apply(listOf(video, photo)))
        assertEquals(listOf(video), SubmissionAttachmentFilter.Videos.apply(listOf(video, photo)))
    }

    @Test
    fun candidatePreviewShowsOnlyInitialLimitUntilShowAll() {
        val candidates = (1..8).map { index ->
            attachment("content://video-$index", "event-$index.mp4", "video/mp4", SubmissionAttachmentKind.Video)
        }

        assertEquals(6, SubmissionAttachmentCandidateDisplay.visible(candidates, showAll = false).size)
        assertEquals(8, SubmissionAttachmentCandidateDisplay.visible(candidates, showAll = true).size)
    }

    @Test
    fun fromManagedFileCreatesPhotoAttachmentForJpeg() {
        val file = ManagedFolderFile("content://photo-1", "plate.jpg", "image/jpeg", 12L)

        val attachment = SubmissionAttachment.fromManagedFile(file, "SafeClip 보관함")

        assertEquals(SubmissionAttachmentKind.Photo, attachment?.kind)
        assertEquals("plate.jpg", attachment?.displayName)
    }

    @Test
    fun fromManagedFileRejectsUnsupportedFiles() {
        val file = ManagedFolderFile("content://note", "note.txt", "text/plain", 12L)

        val attachment = SubmissionAttachment.fromManagedFile(file, "SafeClip 보관함")

        assertNull(attachment)
    }

    @Test
    fun fromVideoCandidateCreatesVideoAttachment() {
        val video = VideoCandidate(
            uriString = "content://video",
            displayName = "event.mp4",
            sizeBytes = 123L,
            lastModifiedMillis = 456L,
            folderPath = "현재 폴더"
        )

        val attachment = SubmissionAttachment.fromVideoCandidate(video)

        assertEquals(SubmissionAttachmentKind.Video, attachment.kind)
        assertEquals("event.mp4", attachment.displayName)
    }

    @Test
    fun toRepresentativeVideoCandidateUsesAttachmentFields() {
        val attachment = attachment(
            uriString = "content://photo",
            displayName = "plate.jpg",
            mimeType = "image/jpeg",
            kind = SubmissionAttachmentKind.Photo
        )

        val candidate = attachment.toRepresentativeVideoCandidate()

        assertEquals("content://photo", candidate.uriString)
        assertEquals("plate.jpg", candidate.displayName)
        assertEquals(100L, candidate.sizeBytes)
        assertEquals("현재 폴더", candidate.folderPath)
    }

    private fun attachment(
        uriString: String,
        displayName: String,
        mimeType: String,
        kind: SubmissionAttachmentKind,
        sizeBytes: Long? = 100L
    ): SubmissionAttachment {
        return SubmissionAttachment(
            uriString = uriString,
            displayName = displayName,
            mimeType = mimeType,
            sizeBytes = sizeBytes,
            folderPath = "현재 폴더",
            kind = kind
        )
    }
}
