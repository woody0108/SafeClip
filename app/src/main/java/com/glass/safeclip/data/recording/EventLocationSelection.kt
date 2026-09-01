package com.glass.safeclip.data.recording

import kotlin.math.abs

object EventLocationSelection {
    private const val MaxDistanceFromTriggerMs = 30_000L

    fun closestToTrigger(
        candidates: List<EventLocation>,
        triggerEpochMs: Long
    ): EventLocation? {
        return candidates
            .asSequence()
            .filter { abs(it.capturedAtEpochMs - triggerEpochMs) <= MaxDistanceFromTriggerMs }
            .minWithOrNull(
                compareBy<EventLocation> { abs(it.capturedAtEpochMs - triggerEpochMs) }
                    .thenBy { it.accuracyMeters ?: Float.MAX_VALUE }
            )
    }
}
