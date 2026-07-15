package com.glass.safeclip.data.media

import android.content.Context
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import com.glass.safeclip.data.file.LastSelectedEventFolderStore
import java.io.File

class AndroidSafeClipSavedMediaCounter(
    private val context: Context,
    eventFolderStore: LastSelectedEventFolderStore? = null
) {
    private val eventFolder = eventFolderStore?.let { SafeClipEventFolder(context, it) }

    fun countSavedItems(): Int {
        val eventFolderCount = eventFolder?.loadOrCreate()?.listFiles()?.count { it.isFile }
        if (eventFolderCount != null) return eventFolderCount

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            countMediaStoreItems()
        } else {
            countLegacyFiles()
        }
    }

    private fun countMediaStoreItems(): Int {
        return countMediaStoreCollection(
            collectionUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            relativePath = SafeClipMediaSaveLocation.imageCaptureRelativePath
        ) + countMediaStoreCollection(
            collectionUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
            relativePath = SafeClipMediaSaveLocation.videoClipRelativePath
        )
    }

    private fun countMediaStoreCollection(
        collectionUri: android.net.Uri,
        relativePath: String
    ): Int {
        return runCatching {
            val projection = arrayOf(MediaStore.MediaColumns._ID)
            val selection = "${MediaStore.MediaColumns.RELATIVE_PATH}=?"
            val selectionArgs = arrayOf("$relativePath/")
            context.contentResolver.query(
                collectionUri,
                projection,
                selection,
                selectionArgs,
                null
            )?.use { cursor ->
                cursor.count
            } ?: 0
        }.getOrDefault(0)
    }

    private fun countLegacyFiles(): Int {
        val imageDirectory = File(
            context.getExternalFilesDir(Environment.DIRECTORY_PICTURES),
            "captures"
        )
        val clipDirectory = File(
            context.getExternalFilesDir(Environment.DIRECTORY_MOVIES),
            SafeClipMediaSaveLocation.albumName
        )
        return imageDirectory.safeFileCount() + clipDirectory.safeFileCount()
    }

    private fun File.safeFileCount(): Int {
        return listFiles()
            ?.count { it.isFile }
            ?: 0
    }
}
