package com.glass.safeclip.data.media

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

object EventVideoTimestamp {
    private val zoneId = ZoneId.of("Asia/Seoul")
    private val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss 'KST'")
        .withZone(zoneId)

    fun format(epochMillis: Long): String {
        return formatter.format(Instant.ofEpochMilli(epochMillis))
    }
}
