package com.glass.safeclip.data.media

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class VideoClipSelectionTest {
    @Test
    fun selectionIsCompleteOnlyWhenEndIsAfterStart() {
        assertFalse(VideoClipSelection().isComplete)
        assertFalse(VideoClipSelection(startMs = 3000, endMs = 3000).isComplete)
        assertFalse(VideoClipSelection(startMs = 4000, endMs = 3000).isComplete)
        assertTrue(VideoClipSelection(startMs = 1000, endMs = 3000).isComplete)
    }

    @Test
    fun durationUsesCompleteRange() {
        assertEquals(2500L, VideoClipSelection(startMs = 1000, endMs = 3500).durationMs)
        assertNull(VideoClipSelection(startMs = 3500, endMs = 1000).durationMs)
    }

    @Test
    fun markerPositionsAreNeverNegative() {
        val selection = VideoClipSelection()
            .withStart(-10)
            .withEnd(-20)

        assertEquals(0L, selection.startMs)
        assertEquals(0L, selection.endMs)
    }

    @Test
    fun rangeLabelExplainsEmptyAndSelectedRange() {
        assertEquals("구간: 시작/끝을 지정해주세요", VideoClipText.rangeLabel(VideoClipSelection()))
        assertEquals(
            "구간: 00:01.000 - 00:03.500 (2.5초)",
            VideoClipText.rangeLabel(VideoClipSelection(startMs = 1000, endMs = 3500))
        )
    }
}
