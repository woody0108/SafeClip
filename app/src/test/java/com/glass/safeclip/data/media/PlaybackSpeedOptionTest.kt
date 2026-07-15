package com.glass.safeclip.data.media

import org.junit.Assert.assertEquals
import org.junit.Test

class PlaybackSpeedOptionTest {
    @Test
    fun supportedSpeedsIncludeSlowNormalAndFastOptions() {
        val options = PlaybackSpeedOptions.supported

        assertEquals(listOf("0.25x", "0.5x", "1.0x", "1.5x", "2.0x"), options.map { it.label })
        assertEquals(listOf(0.25f, 0.5f, 1.0f, 1.5f, 2.0f), options.map { it.speed })
    }
}
