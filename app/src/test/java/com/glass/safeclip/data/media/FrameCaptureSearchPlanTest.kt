package com.glass.safeclip.data.media

import org.junit.Assert.assertEquals
import org.junit.Test

class FrameCaptureSearchPlanTest {
    @Test
    fun candidatePositionsTryExactPositionFirstThenNearbyPositions() {
        val positions = FrameCaptureSearchPlan.candidatePositions(10_000)

        assertEquals(listOf(10_000L, 9_900L, 10_100L, 9_500L, 10_500L, 9_000L, 11_000L), positions)
    }

    @Test
    fun candidatePositionsNeverGoBelowZeroAndRemoveDuplicates() {
        val positions = FrameCaptureSearchPlan.candidatePositions(50)

        assertEquals(listOf(50L, 0L, 150L, 550L, 1050L), positions)
    }
}
