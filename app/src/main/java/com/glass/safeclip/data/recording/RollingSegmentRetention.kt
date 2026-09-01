package com.glass.safeclip.data.recording

object RollingSegmentRetention {
    const val RETENTION_DURATION_MS = 3L * 60L * 60L * 1_000L

    fun expired(
        nowElapsedMs: Long,
        segments: List<RollingSegment>
    ): List<RollingSegment> {
        val cutoffElapsedMs = nowElapsedMs - RETENTION_DURATION_MS
        return segments.filter { segment ->
            !segment.isProtected && segment.endElapsedMs < cutoffElapsedMs
        }
    }
}
