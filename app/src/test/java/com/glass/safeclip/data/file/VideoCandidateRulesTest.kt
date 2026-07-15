package com.glass.safeclip.data.file

import com.glass.safeclip.domain.model.VideoCandidate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class VideoCandidateRulesTest {
    @Test
    fun supportsCommonBlackboxVideoExtensionsCaseInsensitive() {
        assertTrue(VideoCandidateRules.isSupportedVideoFile("front_001.MP4"))
        assertTrue(VideoCandidateRules.isSupportedVideoFile("rear_001.mov"))
        assertTrue(VideoCandidateRules.isSupportedVideoFile("event_001.AVI"))
        assertTrue(VideoCandidateRules.isSupportedVideoFile("clip_001.ts"))
    }

    @Test
    fun rejectsNonVideoAndExtensionlessNames() {
        assertFalse(VideoCandidateRules.isSupportedVideoFile("readme.txt"))
        assertFalse(VideoCandidateRules.isSupportedVideoFile("thumbnail.jpg"))
        assertFalse(VideoCandidateRules.isSupportedVideoFile("EVENT_FILE"))
    }

    @Test
    fun sortsByLastModifiedDescendingThenNameDescending() {
        val candidates = listOf(
            VideoCandidate("uri-a", "A.mp4", 100, 2000, "EVENT"),
            VideoCandidate("uri-c", "C.mp4", 100, 3000, "EVENT"),
            VideoCandidate("uri-b", "B.mp4", 100, 3000, "EVENT")
        )

        val sorted = VideoCandidateRules.sortRecentFirst(candidates)

        assertEquals(listOf("C.mp4", "B.mp4", "A.mp4"), sorted.map { it.displayName })
    }
}
