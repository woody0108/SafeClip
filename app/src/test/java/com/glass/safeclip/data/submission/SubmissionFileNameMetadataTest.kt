package com.glass.safeclip.data.submission

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SubmissionFileNameMetadataTest {
    @Test
    fun readsEventDateTimeFromBlackboxFileName() {
        assertEquals(
            "2026.06.30 12:36",
            SubmissionFileNameMetadata.dateTimeFromFileName("EVT_2026_06_30_12_36_45_F.mp4")
        )
    }

    @Test
    fun readsRecordDateTimeFromBlackboxFileName() {
        assertEquals(
            "2026.07.19 12:42",
            SubmissionFileNameMetadata.dateTimeFromFileName("REC_2026_07_19_12_42_56_R.mp4")
        )
    }

    @Test
    fun ignoresImpossibleDateTimeInFileName() {
        assertNull(
            SubmissionFileNameMetadata.dateTimeFromFileName("EVT_2026_08_35_25_00_00_F.mp4")
        )
    }
}
