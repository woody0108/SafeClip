package com.glass.safeclip.data.file

import org.junit.Assert.assertEquals
import org.junit.Test

class ManagedFolderFileSummaryTest {
    @Test
    fun `counts videos and photos separately`() {
        val files = listOf(
            ManagedFolderFile("content://1", "front.mp4", "video/mp4", 10L),
            ManagedFolderFile("content://2", "rear.MOV", "application/octet-stream", 20L),
            ManagedFolderFile("content://3", "capture.jpg", "image/jpeg", 30L),
            ManagedFolderFile("content://4", "memo.txt", "text/plain", 40L)
        )

        val summary = ManagedFolderFileSummary.from(files)

        assertEquals(2, summary.videoCount)
        assertEquals(1, summary.photoCount)
        assertEquals(3, summary.mediaCount)
    }
}
