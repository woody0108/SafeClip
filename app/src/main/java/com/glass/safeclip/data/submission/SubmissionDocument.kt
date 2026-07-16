package com.glass.safeclip.data.submission

import com.glass.safeclip.domain.model.VideoCandidate
import com.glass.safeclip.ui.status.LocalSubmissionRecord
import com.glass.safeclip.ui.status.SubmissionStatus
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FieldValue
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object SubmissionDocument {
    fun createFields(input: SubmissionInput): Map<String, Any?> {
        return mapOf(
            "ownerUid" to input.ownerUid,
            "guestId" to input.guestId,
            "ownerDisplayName" to input.ownerDisplayName,
            "ownerEmail" to input.ownerEmail,
            "status" to STATUS_WAITING_REVIEW,
            "sourceUri" to input.video.uriString,
            "originalFileName" to input.video.displayName,
            "fileSizeBytes" to input.video.sizeBytes,
            "originalLastModifiedMillis" to input.video.lastModifiedMillis,
            "originalFolderPath" to input.video.folderPath,
            "incidentDateTime" to input.draft.incidentDateTime,
            "incidentLocationText" to input.draft.locationText,
            "violationTypeCandidate" to input.draft.incidentType,
            "userMemo" to input.draft.memo,
            "reportReviewConsent" to input.draft.reviewConsent,
            "videoStorageConsent" to input.draft.storageConsent,
            "trafficRiskDataConsent" to input.draft.dataUseConsent,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
    }

    fun toLocalRecord(documentId: String, data: Map<String, Any?>): LocalSubmissionRecord {
        val incidentType = data["violationTypeCandidate"] as? String ?: ""
        return LocalSubmissionRecord(
            id = documentId,
            video = VideoCandidate(
                uriString = data["sourceUri"] as? String ?: "",
                displayName = data["originalFileName"] as? String ?: "제출 파일",
                sizeBytes = data["fileSizeBytes"] as? Long,
                lastModifiedMillis = data["originalLastModifiedMillis"] as? Long,
                folderPath = data["originalFolderPath"] as? String ?: ""
            ),
            title = incidentType.ifBlank { "블랙박스 영상 제출" },
            incidentDateTime = data["incidentDateTime"] as? String ?: "",
            locationText = data["incidentLocationText"] as? String ?: "",
            incidentType = incidentType,
            memo = data["userMemo"] as? String ?: "",
            status = statusFromFirestore(data["status"] as? String),
            submittedAtText = formatSubmittedAt(data["createdAt"])
        )
    }

    private fun formatSubmittedAt(value: Any?): String {
        val date = when (value) {
            is Timestamp -> value.toDate()
            is Date -> value
            is Long -> Date(value)
            else -> null
        } ?: return ""

        return SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.KOREA).format(date)
    }

    private fun statusFromFirestore(status: String?): SubmissionStatus {
        return when (status) {
            "uploading" -> SubmissionStatus.Uploading
            "reviewing" -> SubmissionStatus.Reviewing
            "needs_more_info" -> SubmissionStatus.NeedsMoreInfo
            "report_package_ready" -> SubmissionStatus.ReportPackageReady
            "rejected" -> SubmissionStatus.Rejected
            "completed" -> SubmissionStatus.Completed
            else -> SubmissionStatus.WaitingReview
        }
    }

    private const val STATUS_WAITING_REVIEW = "waiting_review"
}
