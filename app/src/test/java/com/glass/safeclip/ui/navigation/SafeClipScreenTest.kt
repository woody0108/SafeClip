package com.glass.safeclip.ui.navigation

import com.glass.safeclip.domain.model.VideoCandidate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SafeClipScreenTest {
    @Test
    fun bootScreenIsFirstScreen() {
        assertEquals(SafeClipScreen.Boot, SafeClipScreen.first())
    }

    @Test
    fun previewScreenCarriesSelectedVideo() {
        val video = VideoCandidate("uri-1", "front.mp4", 1000, 2000, "EVENT")
        val screen = SafeClipScreen.VideoPreview(video)

        assertTrue(screen is SafeClipScreen.VideoPreview)
        assertEquals("front.mp4", screen.video.displayName)
    }
}
