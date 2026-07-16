package com.glass.safeclip.app

import com.glass.safeclip.data.submission.SubmissionLookupKey

object SubmissionLookupSelector {
    fun from(ownerUid: String?, guestId: String): SubmissionLookupKey {
        return ownerUid?.let { SubmissionLookupKey.OwnerUid(it) }
            ?: SubmissionLookupKey.GuestId(guestId)
    }
}
