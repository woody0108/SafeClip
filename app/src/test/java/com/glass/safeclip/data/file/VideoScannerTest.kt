package com.glass.safeclip.data.file

import org.junit.Assert.assertEquals
import org.junit.Test

class VideoScannerTest {
    @Test
    fun scanReturnsOnlySupportedVideosFromNestedFolders() {
        val root = VideoDocument.folder(
            displayName = "BLACKBOX",
            uriString = "tree-root",
            children = listOf(
                VideoDocument.folder(
                    displayName = "EVENT",
                    uriString = "tree-event",
                    children = listOf(
                        VideoDocument.file("front.mp4", "uri-front", 100, 3000),
                        VideoDocument.file("rear.avi", "uri-rear", 90, 2000),
                        VideoDocument.file("note.txt", "uri-note", 10, 4000)
                    )
                ),
                VideoDocument.folder(
                    displayName = "NORMAL",
                    uriString = "tree-normal",
                    children = listOf(
                        VideoDocument.file("drive.ts", "uri-drive", 80, 1000)
                    )
                )
            )
        )

        val result = VideoScanner().scan(root)

        assertEquals(listOf("front.mp4", "rear.avi", "drive.ts"), result.map { it.displayName })
        assertEquals(listOf("BLACKBOX/EVENT", "BLACKBOX/EVENT", "BLACKBOX/NORMAL"), result.map { it.folderPath })
    }

    @Test
    fun scanSortsRecentVideosFirstAcrossFolders() {
        val root = VideoDocument.folder(
            displayName = "BLACKBOX",
            uriString = "tree-root",
            children = listOf(
                VideoDocument.folder(
                    displayName = "EVENT",
                    uriString = "tree-event",
                    children = listOf(VideoDocument.file("old.mp4", "uri-old", 100, 1000))
                ),
                VideoDocument.folder(
                    displayName = "PARKING",
                    uriString = "tree-parking",
                    children = listOf(VideoDocument.file("new.mp4", "uri-new", 100, 5000))
                )
            )
        )

        val result = VideoScanner().scan(root)

        assertEquals(listOf("new.mp4", "old.mp4"), result.map { it.displayName })
    }
}
