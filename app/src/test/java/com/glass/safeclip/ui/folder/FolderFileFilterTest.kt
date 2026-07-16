package com.glass.safeclip.ui.folder

import com.glass.safeclip.data.file.ManagedFolderFile
import org.junit.Assert.assertEquals
import org.junit.Test

class FolderFileFilterTest {
    private val video = ManagedFolderFile("content://video", "front.mp4", "video/mp4", 100L)
    private val photo = ManagedFolderFile("content://photo", "capture.jpg", "image/jpeg", 50L)

    @Test
    fun filtersFilesByAllVideoAndPhoto() {
        val files = listOf(video, photo)

        assertEquals(listOf("front.mp4", "capture.jpg"), FolderFileFilter.All.apply(files).map { it.displayName })
        assertEquals(listOf("front.mp4"), FolderFileFilter.Video.apply(files).map { it.displayName })
        assertEquals(listOf("capture.jpg"), FolderFileFilter.Photo.apply(files).map { it.displayName })
    }
}
