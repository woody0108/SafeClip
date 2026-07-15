package com.glass.safeclip.data.media

import org.junit.Assert.assertEquals
import org.junit.Test

class CaptureFileNameTest {
    @Test
    fun forVideoKeepsSafeNameAndIncludesPosition() {
        val fileName = CaptureFileName.forVideo("front_001.mp4", 12345)

        assertEquals("front_001_12345ms.jpg", fileName)
    }

    @Test
    fun forVideoReplacesUnsafeCharacters() {
        val fileName = CaptureFileName.forVideo("event / rear?.avi", 500)

        assertEquals("event_rear_500ms.jpg", fileName)
    }
}
