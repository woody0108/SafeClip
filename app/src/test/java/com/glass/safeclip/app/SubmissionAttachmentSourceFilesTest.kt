package com.glass.safeclip.app

import com.glass.safeclip.data.file.ManagedFolderFile
import com.glass.safeclip.domain.model.VideoCandidate
import org.junit.Assert.assertEquals
import org.junit.Test

class SubmissionAttachmentSourceFilesTest {
    @Test
    fun selectedFolderUsesCurrentFolderFilesEvenWhenSubmittingFromEventFolder() {
        val current = listOf(file("content://current", "blackbox.mp4"))
        val event = listOf(file("content://event", "saved.jpg"))

        val sourceFiles = SubmissionAttachmentSourceFiles.from(
            currentFolderFiles = current,
            eventFolderFiles = event,
            fallbackVideos = emptyList()
        )

        assertEquals(current, sourceFiles.selectedFolderFiles)
        assertEquals(event, sourceFiles.eventFolderFiles)
    }

    @Test
    fun selectedFolderFallsBackToVideoCandidatesWhenCurrentFolderFilesAreEmpty() {
        val video = VideoCandidate("content://video", "event.mp4", 100L, null, "현재 폴더")

        val sourceFiles = SubmissionAttachmentSourceFiles.from(
            currentFolderFiles = emptyList(),
            eventFolderFiles = emptyList(),
            fallbackVideos = listOf(video)
        )

        assertEquals("event.mp4", sourceFiles.selectedFolderFiles.single().displayName)
    }

    private fun file(uriString: String, displayName: String): ManagedFolderFile {
        return ManagedFolderFile(
            uriString = uriString,
            displayName = displayName,
            mimeType = if (displayName.endsWith(".jpg")) "image/jpeg" else "video/mp4",
            sizeBytes = 100L
        )
    }
}
