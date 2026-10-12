package com.glass.safeclip.data.media

import org.junit.Assert.assertEquals
import org.junit.Test

class TimestampCopyFileNameTest {
    @Test
    fun keepsEventAndPartNamesDistinctFromTheOriginal() {
        assertEquals("SC_EVENT_20260917_120000_TIMESTAMP.mp4", TimestampCopyFileName.forVideo("SC_EVENT_20260917_120000.mp4"))
        assertEquals("SC_EVENT_20260917_120000_02_TIMESTAMP.mp4", TimestampCopyFileName.forVideo("SC_EVENT_20260917_120000_02.mp4"))
    }

    @Test
    fun acceptsUpperCaseExtension() {
        assertEquals("clip_TIMESTAMP.mp4", TimestampCopyFileName.forVideo("clip.MP4"))
    }
}
