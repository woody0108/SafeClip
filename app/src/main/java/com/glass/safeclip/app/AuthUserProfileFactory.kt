package com.glass.safeclip.app

import com.glass.safeclip.data.auth.AuthConnectionResult
import com.glass.safeclip.data.profile.UserProfile

object AuthUserProfileFactory {
    fun from(result: AuthConnectionResult, guestId: String): UserProfile? {
        if (result !is AuthConnectionResult.SignedIn) return null
        return UserProfile(
            uid = result.uid,
            guestId = guestId,
            email = result.email,
            displayName = result.displayName,
            provider = result.provider
        )
    }
}
