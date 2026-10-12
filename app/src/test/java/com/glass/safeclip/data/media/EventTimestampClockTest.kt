package com.glass.safeclip.data.media

import org.junit.Assert.assertEquals
import org.junit.Test

class EventTimestampClockTest {
    @Test
    fun startsEachSegmentAtItsOwnCaptureTimeDespiteCompositionOffset() {
        val clock = EventTimestampClock(100_000L)
        assertEquals(100_000L, clock.epochMillisAt(60_000_000L))
        assertEquals(101_234L, clock.epochMillisAt(61_234_000L))
    }

    @Test
    fun independentSegmentsKeepGapsInActualCaptureTime() {
        val first = EventTimestampClock(100_000L)
        val second = EventTimestampClock(162_000L)
        assertEquals(100_000L, first.epochMillisAt(0L))
        assertEquals(159_000L, first.epochMillisAt(59_000_000L))
        assertEquals(162_000L, second.epochMillisAt(60_000_000L))
    }
}
