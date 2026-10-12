package com.glass.safeclip.data.recording

object EventWindowPlanner {
    private const val DEFAULT_EVENT_SIDE_DURATION_MS = 60_000L

    fun plan(
        triggerElapsedMs: Long,
        availableEndElapsedMs: Long,
        segments: List<RollingSegment>,
        timeAnchor: RecordingTimeAnchor? = null,
        sideDurationMs: Long = DEFAULT_EVENT_SIDE_DURATION_MS
    ): EventClipPlan {
        require(segments.isNotEmpty()) { "At least one segment is required." }
        require(sideDurationMs > 0) { "Event side duration must be positive." }

        val sortedSegments = segments.sortedBy(RollingSegment::startElapsedMs)
        val requestedStart = triggerElapsedMs - sideDurationMs
        val requestedEnd = triggerElapsedMs + sideDurationMs
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

    fun split(plan: EventClipPlan, maxDurationMs: Long): List<EventClipPlan> {
        require(maxDurationMs > 0) { "Maximum duration must be positive." }
        if (plan.windowEndElapsedMs <= plan.windowStartElapsedMs) return emptyList()

        return generateSequence(plan.windowStartElapsedMs) { previous ->
            (previous + maxDurationMs).takeIf { it < plan.windowEndElapsedMs }
        }.map { chunkStart ->
            val chunkEnd = minOf(chunkStart + maxDurationMs, plan.windowEndElapsedMs)
            val chunkParts = plan.parts.mapNotNull { part ->
                val partStart = part.segment.startElapsedMs + part.clipStartMs
                val partEnd = part.segment.startElapsedMs + part.clipEndMs
                val overlapStart = maxOf(chunkStart, partStart)
                val overlapEnd = minOf(chunkEnd, partEnd)
                if (overlapEnd <= overlapStart) {
                    null
                } else {
                    EventClipPart(
                        segment = part.segment,
                        clipStartMs = overlapStart - part.segment.startElapsedMs,
                        clipEndMs = overlapEnd - part.segment.startElapsedMs
                    )
                }
            }
            EventClipPlan(
                windowStartElapsedMs = chunkStart,
                windowEndElapsedMs = chunkEnd,
                parts = chunkParts,
                isShort = plan.isShort,
                timeAnchor = plan.timeAnchor
            )
        }.toList()
    }
}
