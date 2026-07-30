package com.glass.safeclip.ui.status

import org.junit.Assert.assertEquals
import org.junit.Test

class SubmissionStatusUiTextTest {
    @Test
    fun statusLabelsUseKoreanMvpLabels() {
        assertEquals("검토 대기 중", SubmissionStatusUiText.labelFor(SubmissionStatus.WaitingReview))
        assertEquals("검토 완료", SubmissionStatusUiText.labelFor(SubmissionStatus.ReviewCompleted))
        assertEquals("보완 요청", SubmissionStatusUiText.labelFor(SubmissionStatus.SupplementRequested))
        assertEquals("신고 완료", SubmissionStatusUiText.labelFor(SubmissionStatus.ReportCompleted))
        assertEquals("신고 결과", SubmissionStatusUiText.labelFor(SubmissionStatus.ReportResult))
    }
}
