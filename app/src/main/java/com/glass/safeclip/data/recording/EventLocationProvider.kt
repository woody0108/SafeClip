package com.glass.safeclip.data.recording

fun interface EventLocationProvider {
    suspend fun captureLocation(triggerEpochMs: Long): EventLocation?

    companion object {
        val None = EventLocationProvider { null }
    }
}
