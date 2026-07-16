package com.glass.safeclip.data.file

import org.junit.Assert.assertEquals
import org.junit.Test

class ManagedFolderFileScannerTest {
    @Test
    fun scanReturnsVideoAndJpegFilesOnly() {
        val root = VideoDocument.folder(
            displayName = "BLACKBOX",
            uriString = "content://folder/root",
            children = listOf(
                VideoDocument.file("front.mp4", "content://file/front", 100L, 1L),
                VideoDocument.file("capture.jpg", "content://file/capture", 50L, 2L),
                VideoDocument.file("note.txt", "content://file/note", 10L, 3L),
                VideoDocument.folder(
                    displayName = "EVENT",
                    uriString = "content://folder/event",
                    children = listOf(
                        VideoDocument.file("rear.MOV", "content://file/rear", 200L, 4L),
                        VideoDocument.file("plate.jpeg", "content://file/plate", 60L, 5L)
                    )
                )
            )
        )

        val files = ManagedFolderFileScanner().scan(root)

        assertEquals(
            listOf("front.mp4", "capture.jpg", "rear.MOV", "plate.jpeg"),
            files.map { it.displayName }
        )
        assertEquals(
            listOf("video/mp4", "image/jpeg", "video/mp4", "image/jpeg"),
            files.map { it.mimeType }
        )
    }
}
