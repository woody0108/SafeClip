package com.glass.safeclip.data.media

import java.time.Instant
import java.time.ZoneOffset
import org.junit.Assert.assertEquals
import org.junit.Test

class EventClipFileNameTest {
    @Test
    fun eventAndMutedNamesFollowStoreRules() {
        val name = EventClipFileName.event(
            Instant.parse("2026-08-25T03:04:05Z"),
            ZoneOffset.UTC
        )

        assertEquals("SC_EVENT_20260825_030405.mp4", name)
        assertEquals(
            "SC_EVENT_20260825_030405_MUTED.mp4",
            EventClipFileName.muted(name)
        )
    }
}
