package com.glass.safeclip.ui.submission

import com.glass.safeclip.data.submission.SubmissionAttachment
import com.glass.safeclip.data.submission.SubmissionAttachmentKind
import org.junit.Assert.assertEquals
import org.junit.Test

class SubmissionAttachmentPreviewSelectionTest {
    @Test
    fun initialPreviewUsesRepresentativeAttachment() {
        val representative = attachment("content://rep", "front.mp4")

        val selection = SubmissionAttachmentPreviewSelection.initial(representative)

        assertEquals(representative, selection.selected)
        assertEquals("미리보기", selection.title)
    }

    @Test
    fun clickingAttachedFileChangesPreviewOnly() {
        val representative = attachment("content://rep", "front.mp4")
        val clicked = attachment("content://photo", "capture.jpg", SubmissionAttachmentKind.Photo)

        val selection = SubmissionAttachmentPreviewSelection.initial(representative)
            .select(clicked)

        assertEquals(clicked, selection.selected)
        assertEquals("미리보기", selection.title)
    }

    @Test
    fun clickingCandidateFileUsesCandidatePreviewTitle() {
        val representative = attachment("content://rep", "front.mp4")
        val candidate = attachment("content://candidate", "rear.mp4")

        val selection = SubmissionAttachmentPreviewSelection.initial(representative)
            .selectCandidate(candidate)

        assertEquals(candidate, selection.selected)
        assertEquals("추가 전 미리보기", selection.title)
    }

    private fun attachment(
        uri: String,
        name: String,
        kind: SubmissionAttachmentKind = SubmissionAttachmentKind.Video
    ): SubmissionAttachment {
        return SubmissionAttachment(
            uriString = uri,
            displayName = name,
            mimeType = if (kind == SubmissionAttachmentKind.Photo) "image/jpeg" else "video/mp4",
            sizeBytes = 100L,
            folderPath = "블랙박스 폴더",
            kind = kind
        )
    }
}
