package com.glass.safeclip.app

import com.glass.safeclip.data.file.ManagedFolderFile
import com.glass.safeclip.data.file.VideoDocument
import com.glass.safeclip.data.file.VideoScanner
import com.glass.safeclip.data.file.ManagedFolderFileScanner
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class FolderLoadCoordinatorTest {
    private val coordinator = FolderLoadCoordinator(
        videoScanner = VideoScanner(),
        managedFileScanner = ManagedFolderFileScanner()
    )

    @Test
    fun `builds loaded folder state with scanned videos and files`() {
        val root = VideoDocument.folder(
            displayName = "BLACKBOX",
            uriString = "content://tree/root",
            children = listOf(
                VideoDocument.file("event.mp4", "content://tree/event.mp4", 100L, 200L),
                VideoDocument.file("capture.jpg", "content://tree/capture.jpg", 10L, 210L)
            )
        )

        val result = coordinator.fromRoot(
            root = root,
            selectedUriString = "content://tree/root",
            fallbackName = "선택한 폴더",
            savedMediaItemCount = 3
        )

        assertEquals("BLACKBOX", result.state.selectedFolderName)
        assertEquals("content://tree/root", result.state.selectedFolderUriString)
        assertEquals(3, result.state.savedMediaItemCount)
        assertEquals(1, result.state.videos.size)
        assertEquals(2, result.files.size)
        assertNull(result.state.errorMessage)
    }

    @Test
    fun `uses fallback name and video fallback files when root is missing`() {
        val result = coordinator.fromRoot(
            root = null,
            selectedUriString = "content://tree/missing",
            fallbackName = "선택한 폴더",
            savedMediaItemCount = 7
        )

        assertEquals("선택한 폴더", result.state.selectedFolderName)
        assertEquals(7, result.state.savedMediaItemCount)
        assertEquals(emptyList<ManagedFolderFile>(), result.files)
    }
}
