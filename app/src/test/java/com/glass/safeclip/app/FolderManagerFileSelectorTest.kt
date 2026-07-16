package com.glass.safeclip.app

import com.glass.safeclip.data.file.ManagedFolderFile
import com.glass.safeclip.domain.model.VideoCandidate
import com.glass.safeclip.ui.folder.FolderViewKind
import org.junit.Assert.assertEquals
import org.junit.Test

class FolderManagerFileSelectorTest {
    @Test
    fun `selects saved event files for SafeClip saved folder`() {
        val eventFiles = listOf(ManagedFolderFile("content://event/1", "clip.mp4", "video/mp4", 100L))
        val currentFiles = listOf(ManagedFolderFile("content://current/1", "front.mp4", "video/mp4", 200L))

        val selected = FolderManagerFileSelector.select(
            kind = FolderViewKind.SafeClipSaved,
            eventFolderFiles = eventFiles,
            currentFolderFiles = currentFiles,
            fallbackVideos = emptyList()
        )

        assertEquals(eventFiles, selected)
    }

    @Test
    fun `uses current files before fallback videos for current folder`() {
        val currentFiles = listOf(ManagedFolderFile("content://current/1", "front.mp4", "video/mp4", 200L))

        val selected = FolderManagerFileSelector.select(
            kind = FolderViewKind.CurrentFolder,
            eventFolderFiles = emptyList(),
            currentFolderFiles = currentFiles,
            fallbackVideos = listOf(video("fallback.mp4"))
        )

        assertEquals(currentFiles, selected)
    }

    @Test
    fun `uses fallback videos when current folder files are empty`() {
        val selected = FolderManagerFileSelector.select(
            kind = FolderViewKind.CurrentFolder,
            eventFolderFiles = emptyList(),
            currentFolderFiles = emptyList(),
            fallbackVideos = listOf(video("fallback.mp4"))
        )

        assertEquals("fallback.mp4", selected.single().displayName)
    }

    private fun video(fileName: String): VideoCandidate {
        return VideoCandidate(
            uriString = "content://video/$fileName",
            displayName = fileName,
            sizeBytes = 100L,
            lastModifiedMillis = null,
            folderPath = "blackbox"
        )
    }
}
