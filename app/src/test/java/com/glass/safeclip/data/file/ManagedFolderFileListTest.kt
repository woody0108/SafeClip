package com.glass.safeclip.data.file

import com.glass.safeclip.domain.model.VideoCandidate
import org.junit.Assert.assertEquals
import org.junit.Test

class ManagedFolderFileListTest {
    @Test
    fun `creates managed folder files from video candidates`() {
        val videos = listOf(
            VideoCandidate(
                uriString = "content://safeclip/video/1",
                displayName = "front.mp4",
                sizeBytes = 100L,
                lastModifiedMillis = null,
                folderPath = "blackbox"
            )
        )

        val files = ManagedFolderFileList.fromVideoCandidates(videos)

        assertEquals(1, files.size)
        assertEquals("content://safeclip/video/1", files.first().uriString)
        assertEquals("front.mp4", files.first().displayName)
        assertEquals("video/mp4", files.first().mimeType)
        assertEquals(100L, files.first().sizeBytes)
    }

    @Test
    fun `uses scanned files before video candidate fallback`() {
        val scanned = listOf(ManagedFolderFile("content://safeclip/photo/1", "capture.jpg", "image/jpeg", 10L))
        val videos = listOf(
            VideoCandidate(
                uriString = "content://safeclip/video/1",
                displayName = "front.mp4",
                sizeBytes = 100L,
                lastModifiedMillis = null,
                folderPath = "blackbox"
            )
        )

        val files = ManagedFolderFileList.preferScannedFiles(scanned, videos)

        assertEquals(scanned, files)
    }
}
