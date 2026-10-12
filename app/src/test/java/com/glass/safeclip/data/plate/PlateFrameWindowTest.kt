package com.glass.safeclip.data.plate

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PlateFrameWindowTest {
    @Test
    fun keepsOnlyRecentBoundedFrames() {
        val window = PlateFrameWindow(maxFrames = 3, retentionMs = 3_000)
        (0L..4L).forEach { second ->
            window.add(PlateFrame(second * 1_000, byteArrayOf(second.toByte())))
        }

        assertEquals(listOf(2_000L, 3_000L, 4_000L), window.snapshotAround(3_000, 5_000).map { it.elapsedMs })
    }

    @Test
    fun snapshotDoesNotReturnFramesOutsideEventRange() {
        val window = PlateFrameWindow(maxFrames = 8, retentionMs = 10_000)
        window.add(PlateFrame(1_000, byteArrayOf(1)))
        window.add(PlateFrame(5_000, byteArrayOf(2)))
        window.add(PlateFrame(9_000, byteArrayOf(3)))

        assertEquals(listOf(5_000L), window.snapshotAround(5_000, 1_500).map { it.elapsedMs })
        assertTrue(window.snapshotAround(20_000, 1_000).isEmpty())
    }
}
