package com.glass.safeclip.data.submission

import com.glass.safeclip.domain.model.VideoCandidate
import com.glass.safeclip.data.profile.UserProfile
import com.glass.safeclip.ui.status.LocalSubmissionRecord
import com.glass.safeclip.ui.status.SubmissionStatus
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FieldValue
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object SubmissionDocument {
    fun createFields(input: SubmissionInput): Map<String, Any?> {
        val incidentDateTime = splitIncidentDateTime(input.draft.incidentDateTime)
        return mapOf(
            "ownerUid" to input.ownerUid,
            "guestId" to input.guestId,
            "ownerDisplayName" to input.ownerDisplayName,
            "status" to STATUS_WAITING_REVIEW,
            "incidentDate" to incidentDateTime.date,
            "incidentTime" to incidentDateTime.time,
            "incidentLocation" to input.draft.locationText,
            "reportType" to input.draft.incidentType,
            "reportMemo" to input.draft.memo,
            "companyComment" to "",
            "reviewConsent" to input.draft.reviewConsent,
            "storageConsent" to input.draft.storageConsent,
            "dataUseConsent" to input.draft.dataUseConsent,
            "attachments" to input.attachments.map { it.toFirestoreFields() },
            "submissionSequence" to input.submissionSequence,
            "submissionSequenceText" to input.submissionSequenceText,
            "nasSubmissionFolder" to input.nasSubmissionFolder,
            "submittedAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
    }

    fun guestOwnerLinkFields(profile: UserProfile): Map<String, Any?> {
        return mapOf(
            "ownerUid" to profile.uid,
            "guestId" to profile.guestId,
            "ownerDisplayName" to (profile.displayName ?: profile.email),
            "updatedAt" to FieldValue.serverTimestamp()
        )
    }

    fun toLocalRecord(documentId: String, data: Map<String, Any?>): LocalSubmissionRecord {
        val incidentType = data["reportType"] as? String ?: ""
        val attachments = attachments(data)
        val attachment = firstAttachment(data)
        val incidentDateTime = listOf(
            data["incidentDate"] as? String ?: "",
            data["incidentTime"] as? String ?: ""
        ).filter { it.isNotBlank() }.joinToString(" ")
        val submittedAtDate = dateFromSubmittedAt(data["submittedAt"])
        return LocalSubmissionRecord(
            id = documentId,
            video = VideoCandidate(
                uriString = attachment?.get("nasRelativePath") as? String ?: "",
                displayName = attachment?.get("displayName") as? String ?: "제출 파일",
                sizeBytes = attachment?.get("sizeBytes") as? Long,
                lastModifiedMillis = null,
                folderPath = data["nasSubmissionFolder"] as? String ?: ""
            ),
            title = incidentType.ifBlank { "블랙박스 영상 제출" },
            incidentDateTime = incidentDateTime,
            locationText = data["incidentLocation"] as? String ?: "",
            incidentType = incidentType,
            memo = data["reportMemo"] as? String ?: "",
            status = statusFromFirestore(data["status"] as? String),
            submittedAtText = formatSubmittedAt(submittedAtDate),
            submittedAtMillis = submittedAtDate?.time ?: 0L,
            companyComment = data["companyComment"] as? String ?: "",
            attachmentCount = if (attachments.isEmpty()) 1 else attachments.size,
            videoCount = if (attachments.isEmpty()) 1 else attachments.count { it["kind"] == "video" },
            photoCount = attachments.count { it["kind"] == "photo" }
        )
    }

    private fun dateFromSubmittedAt(value: Any?): Date? {
        return when (value) {
            is Timestamp -> value.toDate()
            is Date -> value
            is Long -> Date(value)
            else -> null
        }
    }

    private fun formatSubmittedAt(date: Date?): String {
        return date?.let { SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.KOREA).format(it) }.orEmpty()
    }

    private fun statusFromFirestore(status: String?): SubmissionStatus {
        return when (status) {
            "검토 완료" -> SubmissionStatus.ReviewCompleted
            "보완 요청" -> SubmissionStatus.SupplementRequested
            "신고 완료" -> SubmissionStatus.ReportCompleted
            "신고 결과" -> SubmissionStatus.ReportResult
            else -> SubmissionStatus.WaitingReview
        }
    }

    private fun firstAttachment(data: Map<String, Any?>): Map<String, Any?>? {
        return attachments(data).firstOrNull()
    }

    private fun attachments(data: Map<String, Any?>): List<Map<String, Any?>> {
        return (data["attachments"] as? List<*>)
            ?.mapNotNull { it as? Map<String, Any?> }
            .orEmpty()
    }

    private fun splitIncidentDateTime(value: String): IncidentDateTime {
        val parts = value.trim().split(Regex("\\s+"), limit = 2)
        return IncidentDateTime(
            date = parts.getOrNull(0).orEmpty(),
            time = parts.getOrNull(1).orEmpty()
        )
    }

    private data class IncidentDateTime(
        val date: String,
        val time: String
    )

    private const val STATUS_WAITING_REVIEW = "검토 대기 중"
}
