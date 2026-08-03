package com.glass.safeclip.ui.submission

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SubmissionReportTypeTest {
    @Test
    fun includesSafetyReportVehicleViolationTypes() {
        assertTrue("교통위반(고속도로 포함)" in SubmissionReportType.Options)
        assertTrue("불법등화, 반사판(지) 가림·손상" in SubmissionReportType.Options)
        assertTrue("기타 자동차 안전 기준 위반" in SubmissionReportType.Options)
    }

    @Test
    fun normalizeAllowsOnlyKnownTypes() {
        assertEquals("이륜차 위반", SubmissionReportType.normalize(" 이륜차 위반 "))
        assertEquals("", SubmissionReportType.normalize("직접 입력"))
    }
}
