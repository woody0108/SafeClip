package com.glass.safeclip.data.recording

import org.junit.Assert.assertEquals
import org.junit.Test

class RecordingQualityTest {
    @Test
    fun unsupportedFhdFallsBackToHighestSupportedLowerQuality() {
        val selected = RecordingQuality.select(
            requested = RecordingQuality.FHD,
            supported = setOf(RecordingQuality.HD, RecordingQuality.SD)
        )

        assertEquals(RecordingQuality.HD, selected)
    }

    @Test(expected = IllegalArgumentException::class)
    fun emptySupportedSetIsRejected() {
        RecordingQuality.select(RecordingQuality.FHD, emptySet())
    }
}
