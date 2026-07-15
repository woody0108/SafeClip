package com.glass.safeclip.ui.status

import org.junit.Assert.assertEquals
import org.junit.Test

class SubmissionStatusUiTextTest {
    @Test
    fun statusLabelsUseKoreanMvpLabels() {
        assertEquals("업로드중", SubmissionStatusUiText.labelFor(SubmissionStatus.Uploading))
        assertEquals("검토대기", SubmissionStatusUiText.labelFor(SubmissionStatus.WaitingReview))
        assertEquals("검토중", SubmissionStatusUiText.labelFor(SubmissionStatus.Reviewing))
        assertEquals("자료보완필요", SubmissionStatusUiText.labelFor(SubmissionStatus.NeedsMoreInfo))
        assertEquals("신고자료준비완료", SubmissionStatusUiText.labelFor(SubmissionStatus.ReportPackageReady))
        assertEquals("반려", SubmissionStatusUiText.labelFor(SubmissionStatus.Rejected))
        assertEquals("완료", SubmissionStatusUiText.labelFor(SubmissionStatus.Completed))
    }
}
