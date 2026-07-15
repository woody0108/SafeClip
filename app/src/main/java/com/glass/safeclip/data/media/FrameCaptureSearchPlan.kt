package com.glass.safeclip.data.media

object FrameCaptureSearchPlan {
    private val offsetsMs = listOf(0L, -100L, 100L, -500L, 500L, -1_000L, 1_000L)

    fun candidatePositions(positionMs: Long): List<Long> {
        return offsetsMs
            .map { (positionMs + it).coerceAtLeast(0) }
            .distinct()
    }
}
