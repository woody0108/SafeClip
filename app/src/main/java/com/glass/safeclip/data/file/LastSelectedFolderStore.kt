package com.glass.safeclip.data.file

import android.content.Context
import android.net.Uri

class LastSelectedFolderStore(context: Context) {
    private val preferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    fun save(uri: Uri) {
        preferences.edit()
            .putString(KEY_LAST_FOLDER_URI, uri.toString())
            .apply()
    }

    fun load(): Uri? {
        val uriString = preferences.getString(KEY_LAST_FOLDER_URI, null)
        return uriString?.takeIf { it.isNotBlank() }?.let(Uri::parse)
    }

    fun clear() {
        preferences.edit()
            .remove(KEY_LAST_FOLDER_URI)
            .apply()
    }

    companion object {
        private const val PREFERENCES_NAME = "safeclip_folder_preferences"
        private const val KEY_LAST_FOLDER_URI = "last_folder_uri"
    }
}
