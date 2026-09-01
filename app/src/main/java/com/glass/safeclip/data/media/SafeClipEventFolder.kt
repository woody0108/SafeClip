package com.glass.safeclip.data.media

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import com.glass.safeclip.data.file.LastSelectedEventFolderStore

class SafeClipEventFolder(
    private val context: Context,
    private val eventFolderStore: LastSelectedEventFolderStore
) {
    fun loadExisting(): DocumentFile? {
        val parentUri = eventFolderStore.load() ?: return null
        val parent = DocumentFile.fromTreeUri(context, parentUri) ?: return null
        if (parent.name == SafeClipMediaSaveLocation.albumName || parent.name == LEGACY_ALBUM_NAME) {
            return parent
        }
        return parent.findFile(SafeClipMediaSaveLocation.albumName)
            ?: parent.findFile(LEGACY_ALBUM_NAME)
    }

    fun ensureIn(parentUri: Uri): DocumentFile? {
        val parent = DocumentFile.fromTreeUri(context, parentUri) ?: return null
        if (parent.name == SafeClipMediaSaveLocation.albumName) return parent

        val existing = parent.findFile(SafeClipMediaSaveLocation.albumName)
        if (existing != null && existing.isDirectory) return existing
        return parent.createDirectory(SafeClipMediaSaveLocation.albumName)
    }

    fun loadOrCreate(): DocumentFile? {
        val parentUri = eventFolderStore.load() ?: return null
        return ensureIn(parentUri)
    }

    private companion object {
        const val LEGACY_ALBUM_NAME = "SafeClip Captures"
    }
}
