package com.glass.safeclip.data.media

import org.junit.Assert.assertEquals
import org.junit.Test

class SafeClipMediaSaveLocationTest {
    @Test
    fun captureAndClipUseSameAlbumName() {
        assertEquals("SafeClip Captures", SafeClipMediaSaveLocation.albumName)
    }

    @Test
    fun clipDisplayPathUsesSafeClipAlbum() {
        assertEquals(
            "Movies/SafeClip Captures/front_clip.mp4",
            SafeClipMediaSaveLocation.videoClipDisplayPath("front_clip.mp4")
        )
    }

    @Test
    fun eventFolderDisplayPathUsesSafeClipAlbum() {
        assertEquals(
            "SafeClip Captures/front_capture.jpg",
            SafeClipMediaSaveLocation.eventFolderDisplayPath("front_capture.jpg")
        )
    }
}
