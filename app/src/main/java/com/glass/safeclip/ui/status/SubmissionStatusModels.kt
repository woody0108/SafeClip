package com.glass.safeclip.ui.status

import com.glass.safeclip.domain.model.VideoCandidate

enum class SubmissionStatus {
    WaitingReview,
    ReviewCompleted,
    SupplementRequested,
    ReportCompleted,
    ReportResult
}

object SubmissionStatusUiText {
    fun labelFor(status: SubmissionStatus): String {
        return when (status) {
            SubmissionStatus.WaitingReview -> "검토 대기 중"
            SubmissionStatus.ReviewCompleted -> "검토 완료"
            SubmissionStatus.SupplementRequested -> "보완 요청"
            SubmissionStatus.ReportCompleted -> "신고 완료"
            SubmissionStatus.ReportResult -> "신고 결과"
        }
    }
}

data class LocalSubmissionRecord(
    val id: String,
    val video: VideoCandidate,
    val title: String,
    val incidentDateTime: String,
    val locationText: String,
    val incidentType: String,
    val memo: String,
    val status: SubmissionStatus,
    val submittedAtText: String = "",
    val submittedAtMillis: Long = 0L,
    val companyComment: String = "",
    val attachmentCount: Int = 1,
    val videoCount: Int = 1,
    val photoCount: Int = 0
)

object SubmissionDetailText {
    fun linesFor(record: LocalSubmissionRecord): List<String> {
        val lines = mutableListOf(
            "일시 : ${record.incidentDateTime}",
            "위치 : ${record.locationText}",
            "신고유형 : ${record.incidentType}",
            "첨부 : 파일 ${record.attachmentCount}개 / 영상 ${record.videoCount}개 / 사진 ${record.photoCount}개",
            "메모 : ${record.memo}"
        )
        if (record.companyComment.isNotBlank()) {
            lines += "${companyCommentLabel(record.status)} : ${record.companyComment}"
        }
        return lines
    }

    private fun companyCommentLabel(status: SubmissionStatus): String {
        return when (status) {
            SubmissionStatus.SupplementRequested -> "보완 요청 내용"
            SubmissionStatus.ReportResult -> "신고 결과 내용"
            else -> "처리 의견"
        }
    }
}
