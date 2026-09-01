package com.glass.safeclip.data.recording

import org.junit.Assert.assertEquals
import org.junit.Test

class RollingSegmentRetentionTest {
    @Test
    fun protectedSegmentsAreNotExpired() {
        val old = RollingSegment("old", "file:///old.mp4", 0, 60_000, false)
        val protected = RollingSegment("protected", "file:///protected.mp4", 0, 60_000, true)

        val expired = RollingSegmentRetention.expired(
            nowElapsedMs = 10_860_001,
            segments = listOf(old, protected)
        )

        assertEquals(listOf(old), expired)
    }

    @Test
    fun segmentsInsideThreeHoursAreKept() {
        val recent = RollingSegment("recent", "file:///recent.mp4", 60_001, 120_001, false)

        assertEquals(
            emptyList<RollingSegment>(),
            RollingSegmentRetention.expired(10_860_001, listOf(recent))
        )
    }
}
