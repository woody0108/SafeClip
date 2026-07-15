package com.glass.safeclip.data.submission

sealed interface SubmissionLookupKey {
    val fieldName: String
    val value: String

    data class OwnerUid(override val value: String) : SubmissionLookupKey {
        override val fieldName: String = "ownerUid"
    }

    data class GuestId(override val value: String) : SubmissionLookupKey {
        override val fieldName: String = "guestId"
    }
}
