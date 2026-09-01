package com.glass.safeclip.data.recording

object EventWindowPlanner {
    private const val EVENT_SIDE_DURATION_MS = 60_000L

    fun plan(
        triggerElapsedMs: Long,
        availableEndElapsedMs: Long,
        segments: List<RollingSegment>,
        timeAnchor: RecordingTimeAnchor? = null
    ): EventClipPlan {
        require(segments.isNotEmpty()) { "At least one segment is required." }

        val sortedSegments = segments.sortedBy(RollingSegment::startElapsedMs)
        val requestedStart = triggerElapsedMs - EVENT_SIDE_DURATION_MS
        val requestedEnd = triggerElapsedMs + EVENT_SIDE_DURATION_MS
        val windowStart = maxOf(requestedStart, sortedSegments.first().startElapsedMs)
        val windowEnd = minOf(
            requestedEnd,
            availableEndElapsedMs,
            sortedSegments.last().endElapsedMs
        )
        require(windowEnd > windowStart) { "The available event window is empty." }

        val parts = sortedSegments.mapNotNull { segment ->
            val partStart = maxOf(windowStart, segment.startElapsedMs)
            val partEnd = minOf(windowEnd, segment.endElapsedMs)
            if (partEnd <= partStart) {
                null
            } else {
                EventClipPart(
                    segment = segment,
                    clipStartMs = partStart - segment.startElapsedMs,
                    clipEndMs = partEnd - segment.startElapsedMs
                )
            }
        }

        return EventClipPlan(
            windowStartElapsedMs = windowStart,
            windowEndElapsedMs = windowEnd,
            parts = parts,
            isShort = windowStart != requestedStart || windowEnd != requestedEnd,
            timeAnchor = timeAnchor
        )
    }
}
