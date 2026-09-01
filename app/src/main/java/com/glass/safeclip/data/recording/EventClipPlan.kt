package com.glass.safeclip.data.recording

data class EventClipPart(
    val segment: RollingSegment,
    val clipStartMs: Long,
    val clipEndMs: Long
)

data class EventClipPlan(
    val windowStartElapsedMs: Long,
    val windowEndElapsedMs: Long,
    val parts: List<EventClipPart>,
    val isShort: Boolean,
    val timeAnchor: RecordingTimeAnchor? = null
) {
    fun epochMillisAt(elapsedRealtimeMs: Long): Long {
        val anchor = requireNotNull(timeAnchor) { "실제 시각 기준이 없습니다." }
        return anchor.epochMillis + (elapsedRealtimeMs - anchor.elapsedRealtimeMs)
    }
}

data class RecordingTimeAnchor(
    val elapsedRealtimeMs: Long,
    val epochMillis: Long
)
