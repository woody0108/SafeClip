package com.glass.safeclip.data.file

import org.junit.Assert.assertEquals
import org.junit.Test

class ManagedFolderVideoCandidateTest {
    @Test
    fun videoFileCanBecomeVideoCandidate() {
        val file = ManagedFolderFile(
            uriString = "content://safeclip/video/1",
            displayName = "front_clip.mp4",
            mimeType = "video/mp4",
            sizeBytes = 1024L
        )

        val candidate = ManagedFolderVideoCandidate.from(file, folderPath = "SafeClip 저장함")

        requireNotNull(candidate)
        assertEquals("content://safeclip/video/1", candidate.uriString)
        assertEquals("front_clip.mp4", candidate.displayName)
        assertEquals(1024L, candidate.sizeBytes)
        assertEquals("SafeClip 저장함", candidate.folderPath)
    }

    @Test
    fun captureImageCanBecomeSubmissionCandidateAndPreviewImageButCannotPlayVideo() {
        val file = ManagedFolderFile(
            uriString = "content://safeclip/image/1",
            displayName = "front_capture.jpg",
            mimeType = "image/jpeg",
            sizeBytes = 512L
        )

        val candidate = ManagedFolderVideoCandidate.fromSubmittableFile(file, folderPath = "SafeClip 저장함")

        requireNotNull(candidate)
        assertEquals("content://safeclip/image/1", candidate.uriString)
        assertEquals("front_capture.jpg", candidate.displayName)
        assertEquals("SafeClip 저장함", candidate.folderPath)
        assertEquals(false, ManagedFolderVideoCandidate.canUseVideoActions(file))
        assertEquals(true, ManagedFolderVideoCandidate.canPreviewImage(file))
        assertEquals(true, ManagedFolderVideoCandidate.canSubmitFile(file))
        assertEquals("사진 미리보기", ManagedFolderVideoCandidate.primaryPreviewActionText(file))
    }

    @Test
    fun videoFileUsesVideoPreviewActionText() {
        val file = ManagedFolderFile(
            uriString = "content://safeclip/video/1",
            displayName = "front_clip.mp4",
            mimeType = "video/mp4",
            sizeBytes = 1024L
        )

        assertEquals(true, ManagedFolderVideoCandidate.canUseVideoActions(file))
        assertEquals(false, ManagedFolderVideoCandidate.canPreviewImage(file))
        assertEquals("영상재생하기", ManagedFolderVideoCandidate.primaryPreviewActionText(file))
    }
}
