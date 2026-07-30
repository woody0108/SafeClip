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
                "일시 : 2026.07.15 10:30",
                "위치 : 서울 강남구",
                "신고유형 : 추돌 사고",
                "첨부 : 파일 1개 / 영상 1개 / 사진 0개",
                "메모 : 급정거 후 추돌"
            ),
            SubmissionDetailText.linesFor(record)
        )
    }

    @Test
    fun detailLinesShowCompanyCommentWithStatusSpecificLabel() {
        val record = LocalSubmissionRecord(
            id = "LOCAL-1",
            video = VideoCandidate("uri", "event.mp4", 1000L, null, "EVENT"),
            title = "보완 요청",
            incidentDateTime = "2026.07.15 10:30",
            locationText = "서울 강남구",
            incidentType = "교통법규 위반",
            memo = "메모",
            status = SubmissionStatus.SupplementRequested,
            companyComment = "차량번호가 흐려 추가 사진이 필요합니다."
        )

        assertEquals(
            "보완 요청 내용 : 차량번호가 흐려 추가 사진이 필요합니다.",
            SubmissionDetailText.linesFor(record).last()
        )
    }
}
