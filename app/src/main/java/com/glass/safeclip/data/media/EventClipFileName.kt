package com.glass.safeclip.data.media

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

object EventClipFileName {
    private val formatter = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")

    fun event(instant: Instant, zoneId: ZoneId = ZoneId.systemDefault()): String {
        return "SC_EVENT_${formatter.withZone(zoneId).format(instant)}.mp4"
    }

    fun muted(originalName: String): String {
        val base = originalName.removeSuffix(".mp4")
        return "${base}_MUTED.mp4"
    }
}
