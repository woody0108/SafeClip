package com.glass.safeclip.data.plate

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PlateTextParserTest {
    @Test
    fun extractsModernKoreanPlateAfterRemovingVisualSeparators() {
        assertEquals(
            listOf("123가4567", "12나3456"),
            PlateTextParser.extractCandidates("앞차 123 가 4567 / 옆차 12-나-3456")
        )
    }

    @Test
    fun rejectsPlainPhoneNumbersAndIncompletePlateText() {
        assertTrue(PlateTextParser.extractCandidates("010-1234-5678 12가345").isEmpty())
    }

    @Test
    fun keepsCandidatesInReadingOrderWithoutDuplicates() {
        assertEquals(
            listOf("123가4567", "234나5678"),
            PlateTextParser.extractCandidates("123가4567 123가4567 234나5678")
        )
    }

    @Test
    fun normalizesUserConfirmedPlateAndRejectsInvalidEdit() {
        assertEquals("123가4567", PlateTextParser.normalizeConfirmed("123 가-4567"))
        assertEquals(null, PlateTextParser.normalizeConfirmed("확인불가"))
    }
}
