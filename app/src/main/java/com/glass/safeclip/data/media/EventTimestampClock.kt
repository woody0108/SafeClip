package com.glass.safeclip.data.media

class EventTimestampClock(private val startEpochMillis: Long) {
    private var firstPresentationTimeUs: Long? = null

    fun epochMillisAt(presentationTimeUs: Long): Long {
        // Media3는 연결된 조각에 누적 PTS를 전달하므로 각 조각의 첫 프레임을 기준으로 삼는다.
        val first = firstPresentationTimeUs ?: presentationTimeUs.also { firstPresentationTimeUs = it }
        return startEpochMillis + (presentationTimeUs - first) / 1_000L
    }
}
