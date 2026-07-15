package com.glass.safeclip.data.media

import org.junit.Assert.assertEquals
import org.junit.Test

class ClipTempFileNameTest {
    @Test
    fun forSourceCopyKeepsKnownVideoExtension() {
        assertEquals("front_001_source.mp4", ClipTempFileName.forSourceCopy("front_001.mp4"))
        assertEquals("rear_source.mov", ClipTempFileName.forSourceCopy("rear.MOV"))
    }

    @Test
    fun forSourceCopyFallsBackToMp4ForUnknownExtension() {
        assertEquals("event_source.mp4", ClipTempFileName.forSourceCopy("event"))
        assertEquals("event_source.mp4", ClipTempFileName.forSourceCopy("event.txt"))
    }
}
