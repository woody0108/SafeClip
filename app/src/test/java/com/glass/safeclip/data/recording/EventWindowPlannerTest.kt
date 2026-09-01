package com.glass.safeclip.data.recording

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class EventWindowPlannerTest {
    private val segments = listOf(
        RollingSegment("a", "file:///a.mp4", 0, 60_000, false),
        RollingSegment("b", "file:///b.mp4", 60_000, 120_000, false),
        RollingSegment("c", "file:///c.mp4", 120_000, 180_000, false)
    )

    @Test
    fun eventWindowUsesSixtySecondsBeforeAndAfterTrigger() {
        val plan = EventWindowPlanner.plan(
            triggerElapsedMs = 90_000,
            availableEndElapsedMs = 180_000,
            segments = segments
        )

        assertEquals(30_000, plan.windowStartElapsedMs)
        assertEquals(150_000, plan.windowEndElapsedMs)
        assertFalse(plan.isShort)
        assertEquals(listOf("a", "b", "c"), plan.parts.map { it.segment.id })
        assertEquals(EventClipPart(segments[0], 30_000, 60_000), plan.parts[0])
        assertEquals(EventClipPart(segments[1], 0, 60_000), plan.parts[1])
        assertEquals(EventClipPart(segments[2], 0, 30_000), plan.parts[2])
    }

    @Test
    fun stoppingBeforePostMinuteProducesShortPlan() {
        val plan = EventWindowPlanner.plan(
            triggerElapsedMs = 90_000,
            availableEndElapsedMs = 120_000,
            segments = segments.take(2)
        )

        assertEquals(30_000, plan.windowStartElapsedMs)
        assertEquals(120_000, plan.windowEndElapsedMs)
        assertTrue(plan.isShort)
    }

    @Test
    fun eventAtSegmentBoundariesAlwaysContainsTheTrigger() {
        listOf(59_000L, 60_000L, 61_000L).forEach { trigger ->
            val plan = EventWindowPlanner.plan(
                triggerElapsedMs = trigger,
                availableEndElapsedMs = 180_000,
                segments = segments
            )

            assertTrue(plan.windowStartElapsedMs <= trigger)
            assertTrue(plan.windowEndElapsedMs > trigger)
            assertTrue(plan.parts.any { part ->
                val absoluteStart = part.segment.startElapsedMs + part.clipStartMs
                val absoluteEnd = part.segment.startElapsedMs + part.clipEndMs
                trigger in absoluteStart until absoluteEnd
            })
        }
    }

    @Test
    fun timeAnchorConvertsElapsedRecordingTimeToActualTime() {
        val plan = EventWindowPlanner.plan(
            triggerElapsedMs = 120_000,
            availableEndElapsedMs = 180_000,
            segments = segments,
            timeAnchor = RecordingTimeAnchor(
                elapsedRealtimeMs = 100_000,
                epochMillis = 1_788_000_000_000
            )
        )

        assertEquals(1_788_000_030_000, plan.epochMillisAt(130_000))
    }
}
