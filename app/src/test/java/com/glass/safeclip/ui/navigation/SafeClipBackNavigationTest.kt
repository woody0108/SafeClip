package com.glass.safeclip.ui.navigation

import com.glass.safeclip.domain.model.VideoCandidate
import com.glass.safeclip.data.file.ManagedFolderFile
import com.glass.safeclip.ui.folder.FolderViewKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SafeClipBackNavigationTest {
    private val video = VideoCandidate("uri-1", "front.mp4", 1000, 2000, "EVENT")
    private val image = ManagedFolderFile("uri-2", "capture.jpg", "image/jpeg", 512)

    @Test
    fun bootScreenHasNoBackDestination() {
        assertNull(SafeClipBackNavigation.previousScreen(SafeClipScreen.Boot))
    }

    @Test
    fun startScreenHasNoBackDestination() {
        assertNull(SafeClipBackNavigation.previousScreen(SafeClipScreen.Start))
    }

    @Test
    fun homeBackGoesToStart() {
        assertEquals(SafeClipScreen.Start, SafeClipBackNavigation.previousScreen(SafeClipScreen.Home))
    }

    @Test
    fun connectingBackGoesToStart() {
        assertEquals(SafeClipScreen.Start, SafeClipBackNavigation.previousScreen(SafeClipScreen.Connecting))
    }

    @Test
    fun browserBackGoesToHome() {
        assertEquals(SafeClipScreen.Home, SafeClipBackNavigation.previousScreen(SafeClipScreen.VideoBrowser))
    }

    @Test
    fun previewBackGoesToBrowser() {
        assertEquals(SafeClipScreen.VideoBrowser, SafeClipBackNavigation.previousScreen(SafeClipScreen.VideoPreview(video)))
    }

    @Test
    fun imagePreviewBackGoesToSourceFolderManager() {
        assertEquals(
            SafeClipScreen.FolderManager(FolderViewKind.SafeClipSaved),
            SafeClipBackNavigation.previousScreen(
                SafeClipScreen.ImagePreview(image, FolderViewKind.SafeClipSaved)
            )
        )
    }

    @Test
    fun submissionFormBackGoesToPreviewForSameVideo() {
        val previous = SafeClipBackNavigation.previousScreen(SafeClipScreen.SubmissionForm(video, null))

        assertEquals(SafeClipScreen.VideoPreview(video), previous)
    }

    @Test
    fun submissionStatusBackGoesToHome() {
        assertEquals(SafeClipScreen.Home, SafeClipBackNavigation.previousScreen(SafeClipScreen.SubmissionStatus))
    }

    @Test
    fun settingsBackGoesToHome() {
        assertEquals(SafeClipScreen.Home, SafeClipBackNavigation.previousScreen(SafeClipScreen.Settings))
    }
}
