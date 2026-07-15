package com.glass.safeclip.ui.status

import com.glass.safeclip.domain.model.VideoCandidate

enum class SubmissionStatus {
    Uploading,
    WaitingReview,
    Reviewing,
    NeedsMoreInfo,
    ReportPackageReady,
    Rejected,
    Completed
}

object SubmissionStatusUiText {
    fun labelFor(status: SubmissionStatus): String {
        return when (status) {
            SubmissionStatus.Uploading -> "업로드중"
            SubmissionStatus.WaitingReview -> "검토대기"
            SubmissionStatus.Reviewing -> "검토중"
            SubmissionStatus.NeedsMoreInfo -> "자료보완필요"
            SubmissionStatus.ReportPackageReady -> "신고자료준비완료"
            SubmissionStatus.Rejected -> "반려"
            SubmissionStatus.Completed -> "완료"
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
    val status: SubmissionStatus
)

object SubmissionDetailText {
    fun linesFor(record: LocalSubmissionRecord): List<String> {
        return listOf(
            "사고일시 : ${record.incidentDateTime}",
            "위치 : ${record.locationText}",
            "사고유형 : ${record.incidentType}",
            "메모 : ${record.memo}"
        )
    }
}
