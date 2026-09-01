package com.glass.safeclip.data.media

import org.junit.Assert.assertEquals
import org.junit.Test

class EventVideoTimestampTest {
    @Test
    fun formatsKoreanActualTimeForVideoOverlay() {
        assertEquals(
            "2026-08-26 15:23:41 KST",
            EventVideoTimestamp.format(1_787_725_421_000)
        )
    }
}
