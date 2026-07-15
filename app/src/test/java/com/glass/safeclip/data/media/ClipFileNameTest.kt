package com.glass.safeclip.data.media

import org.junit.Assert.assertEquals
import org.junit.Test

class ClipFileNameTest {
    @Test
    fun forVideoKeepsSafeNameAndIncludesRange() {
        val fileName = ClipFileName.forVideo("front_001.mp4", startMs = 1000, endMs = 3500)

        assertEquals("front_001_clip_1000ms_3500ms.mp4", fileName)
    }

    @Test
    fun forVideoReplacesUnsafeCharacters() {
        val fileName = ClipFileName.forVideo("event / rear?.avi", startMs = 500, endMs = 1500)

        assertEquals("event_rear_clip_500ms_1500ms.mp4", fileName)
    }
}
