package com.glass.safeclip.data.identity

import java.util.UUID

object GuestIdGenerator {
    fun create(): String = fromSeed(UUID.randomUUID().toString())

    fun fromSeed(seed: String): String {
        val normalized = seed
            .filter { it.isLetterOrDigit() }
            .uppercase()
            .padEnd(8, '0')
            .take(8)

        return "Guest-${normalized.take(4)}-${normalized.drop(4)}"
    }
}

object GuestIdentity {
    fun create(savedGuestId: String?, newGuestId: () -> String): String {
        return savedGuestId?.takeIf { it.isNotBlank() } ?: newGuestId()
    }
}
