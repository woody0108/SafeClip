package com.glass.safeclip.data.media

import java.util.Locale

data class VideoClipSelection(
    val startMs: Long? = null,
    val endMs: Long? = null
) {
    val isComplete: Boolean
        get() = startMs != null && endMs != null && endMs > startMs

    val durationMs: Long?
        get() = if (isComplete) endMs!! - startMs!! else null

    fun withStart(positionMs: Long): VideoClipSelection {
        return copy(startMs = positionMs.coerceAtLeast(0))
    }

    fun withEnd(positionMs: Long): VideoClipSelection {
        return copy(endMs = positionMs.coerceAtLeast(0))
    }
}

object VideoClipText {
    fun rangeLabel(selection: VideoClipSelection): String {
        val start = selection.startMs ?: return "구간: 시작/끝을 지정해주세요"
        val end = selection.endMs ?: return "구간: 시작 ${formatTime(start)} / 끝을 지정해주세요"
        val duration = selection.durationMs ?: return "구간: 끝 시간이 시작 시간보다 앞에 있습니다"
        return "구간: ${formatTime(start)} - ${formatTime(end)} (${formatSeconds(duration)}초)"
    }

    private fun formatTime(positionMs: Long): String {
        val minutes = positionMs / 60_000
        val seconds = (positionMs % 60_000) / 1_000
        val millis = positionMs % 1_000
        return String.format(Locale.US, "%02d:%02d.%03d", minutes, seconds, millis)
    }

    private fun formatSeconds(durationMs: Long): String {
        val seconds = durationMs / 1_000.0
        return if (durationMs % 1_000 == 0L) {
            String.format(Locale.US, "%.0f", seconds)
        } else {
            String.format(Locale.US, "%.1f", seconds)
        }
    }
}
