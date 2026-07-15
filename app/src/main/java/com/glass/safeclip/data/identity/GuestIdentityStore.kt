package com.glass.safeclip.data.identity

import android.content.Context

class GuestIdentityStore(context: Context) {
    private val preferences = context.getSharedPreferences("safeclip_guest_identity", Context.MODE_PRIVATE)

    fun loadOrCreate(): String {
        val guestId = GuestIdentity.create(
            savedGuestId = preferences.getString(KEY_GUEST_ID, null),
            newGuestId = GuestIdGenerator::create
        )
        preferences.edit().putString(KEY_GUEST_ID, guestId).apply()
        return guestId
    }

    private companion object {
        const val KEY_GUEST_ID = "guest_id"
    }
}
