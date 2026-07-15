package com.glass.safeclip.ui.status

import com.glass.safeclip.domain.model.VideoCandidate
import org.junit.Assert.assertEquals
import org.junit.Test

class LocalSubmissionRecordTest {
    @Test
    fun detailLinesIncludeSubmittedIncidentFields() {
        val record = LocalSubmissionRecord(
            id = "LOCAL-1",
            video = VideoCandidate("uri", "event.mp4", 1000L, null, "EVENT"),
            title = "추돌 사고",
            incidentDateTime = "2026.07.15 10:30",
            locationText = "서울 강남구",
            incidentType = "추돌 사고",
            memo = "급정거 후 추돌",
            status = SubmissionStatus.WaitingReview
        )

        assertEquals(
            listOf(
                "사고일시 : 2026.07.15 10:30",
                "위치 : 서울 강남구",
                "사고유형 : 추돌 사고",
                "메모 : 급정거 후 추돌"
            ),
            SubmissionDetailText.linesFor(record)
        )
    }
}
