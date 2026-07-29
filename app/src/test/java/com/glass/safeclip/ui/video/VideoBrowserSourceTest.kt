package com.glass.safeclip.ui.video

import com.glass.safeclip.data.file.ManagedFolderFile
import org.junit.Assert.assertEquals
import org.junit.Test

class VideoBrowserSourceTest {
    @Test
    fun allSourceCombinesBlackboxAndSafeClipFiles() {
        val blackbox = listOf(file("content://blackbox", "blackbox.mp4"))
        val safeClip = listOf(file("content://safeclip", "capture.jpg"))

        val files = VideoBrowserSource.All.select(blackbox, safeClip)

        assertEquals(listOf("blackbox.mp4", "capture.jpg"), files.map { it.displayName })
    }

    @Test
    fun blackboxSourceShowsOnlyBlackboxFolderFiles() {
        val blackbox = listOf(file("content://blackbox", "blackbox.mp4"))
        val safeClip = listOf(file("content://safeclip", "capture.jpg"))

        val files = VideoBrowserSource.Blackbox.select(blackbox, safeClip)

        assertEquals(listOf("blackbox.mp4"), files.map { it.displayName })
    }

    @Test
    fun safeClipSourceShowsOnlySafeClipFolderFiles() {
        val blackbox = listOf(file("content://blackbox", "blackbox.mp4"))
        val safeClip = listOf(file("content://safeclip", "capture.jpg"))

        val files = VideoBrowserSource.SafeClip.select(blackbox, safeClip)

        assertEquals(listOf("capture.jpg"), files.map { it.displayName })
    }

    private fun file(uri: String, name: String): ManagedFolderFile {
        return ManagedFolderFile(
            uriString = uri,
            displayName = name,
            mimeType = if (name.endsWith(".jpg")) "image/jpeg" else "video/mp4",
            sizeBytes = 100L
        )
    }
}
