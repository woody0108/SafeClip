package com.glass.safeclip.data.recording

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class EventLocationSelectionTest {
    @Test
    fun closestMeasurementToTheButtonTimeWins() {
        val older = EventLocation(35.5, 129.3, 4f, 980_000)
        val closest = EventLocation(35.6, 129.4, 12f, 1_002_000)

        assertEquals(
            closest,
            EventLocationSelection.closestToTrigger(
                candidates = listOf(older, closest),
                triggerEpochMs = 1_000_000
            )
        )
    }

    @Test
    fun measurementsMoreThanThirtySecondsFromTheButtonAreRejected() {
        val stale = EventLocation(35.5, 129.3, 4f, 969_999)

        assertNull(
            EventLocationSelection.closestToTrigger(
                candidates = listOf(stale),
                triggerEpochMs = 1_000_000
            )
        )
    }

    @Test
    fun betterAccuracyBreaksAnEqualTimeTie() {
        val lessAccurate = EventLocation(35.5, 129.3, 20f, 999_000)
        val moreAccurate = EventLocation(35.6, 129.4, 5f, 1_001_000)

        assertEquals(
            moreAccurate,
            EventLocationSelection.closestToTrigger(
                candidates = listOf(lessAccurate, moreAccurate),
                triggerEpochMs = 1_000_000
            )
        )
    }
}
