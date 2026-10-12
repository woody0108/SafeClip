package com.glass.safeclip.data.media

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TimestampBandLayoutTest {
    @Test
    fun addsSpaceAboveFhdWithoutShrinkingContent() {
        val layout = TimestampBandLayout.forFrame(1920, 1080)
        assertEquals(1920, layout.width)
        assertEquals(80, layout.bandHeight)
        assertEquals(1160, layout.outputHeight)
        assertEquals(1080f, layout.scaleY * layout.outputHeight, 0.001f)
        assertEquals(-1f, -layout.scaleY + layout.translateY, 0.0001f)
        assertEquals(1f - 2f * 80f / 1160f, layout.scaleY + layout.translateY, 0.0001f)
    }

    @Test
    fun supportsPortraitSdAndSmallFramesWithEvenOutputHeight() {
        for ((width, height) in listOf(1080 to 1920, 720 to 480, 320 to 241)) {
            val layout = TimestampBandLayout.forFrame(width, height)
            assertEquals(width, layout.width)
            assertEquals(height, layout.outputHeight - layout.bandHeight)
            assertEquals(0, layout.outputHeight % 2)
            assertTrue(layout.bandHeight >= 24)
        }
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsInvalidDimensions() {
        TimestampBandLayout.forFrame(0, 1080)
    }
}
