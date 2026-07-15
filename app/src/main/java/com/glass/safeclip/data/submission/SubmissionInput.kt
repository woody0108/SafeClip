package com.glass.safeclip.data.submission

import com.glass.safeclip.domain.model.VideoCandidate
import com.glass.safeclip.ui.submission.SubmissionDraft

data class SubmissionInput(
    val ownerUid: String?,
    val video: VideoCandidate,
    val draft: SubmissionDraft,
    val guestId: String? = null,
    val ownerDisplayName: String? = null,
    val ownerEmail: String? = null
)
