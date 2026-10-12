package com.glass.safeclip.data.plate

import org.junit.Assert.assertEquals
import org.junit.Test

class PlateCandidateRankerTest {
    @Test
    fun repeatedReadingWinsOverSingleLargeReading() {
        val ranked = PlateCandidateRanker.rank(
            listOf(
                PlateObservation("123가4567", frameOffsetMs = -1_000, pixelArea = 4_000, sharpness = 8.0),
                PlateObservation("123가4567", frameOffsetMs = 900, pixelArea = 4_500, sharpness = 9.0),
                PlateObservation("999나9999", frameOffsetMs = 0, pixelArea = 20_000, sharpness = 30.0)
            )
        )

        assertEquals("123가4567", ranked.first().text)
        assertEquals(2, ranked.first().frameCount)
    }

    @Test
    fun closestSharperFrameIsSelectedAsPreview() {
        val ranked = PlateCandidateRanker.rank(
            listOf(
                PlateObservation("123가4567", frameOffsetMs = -2_000, pixelArea = 8_000, sharpness = 10.0),
                PlateObservation("123가4567", frameOffsetMs = 300, pixelArea = 7_000, sharpness = 14.0)
            )
        )

        assertEquals(300, ranked.first().bestObservation.frameOffsetMs)
    }
}
