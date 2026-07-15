package com.glass.safeclip.data.file

import android.content.Context
import android.net.Uri

class LastSelectedEventFolderStore(context: Context) {
    private val preferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    fun save(uri: Uri) {
        preferences.edit()
            .putString(KEY_EVENT_PARENT_URI, uri.toString())
            .apply()
    }

    fun load(): Uri? {
        val uriString = preferences.getString(KEY_EVENT_PARENT_URI, null)
        return uriString?.takeIf { it.isNotBlank() }?.let(Uri::parse)
    }

    companion object {
        private const val PREFERENCES_NAME = "safeclip_event_folder_preferences"
        private const val KEY_EVENT_PARENT_URI = "event_parent_uri"
    }
}
