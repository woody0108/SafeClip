package com.glass.safeclip.data.media

import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import com.glass.safeclip.data.file.LastSelectedEventFolderStore
import com.glass.safeclip.data.file.ManagedFolderFile
import java.io.File

class AndroidSafeClipSavedMediaRepository(
    private val context: Context,
    eventFolderStore: LastSelectedEventFolderStore? = null
) {
    private val eventFolder = eventFolderStore?.let { SafeClipEventFolder(context, it) }

    fun listSavedItems(): List<ManagedFolderFile> {
        val eventFolderItems = listEventFolderItems()
        if (eventFolderItems != null) return eventFolderItems

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            listMediaStoreItems()
        } else {
            listLegacyFiles()
        }
    }

    private fun listEventFolderItems(): List<ManagedFolderFile>? {
        val folder = eventFolder?.loadOrCreate() ?: return null
        return folder.listFiles()
            .filter { it.isFile }
            .map {
                ManagedFolderFile(
                    uriString = it.uri.toString(),
                    displayName = it.name ?: "SafeClip 파일",
                    mimeType = it.type ?: "application/octet-stream",
                    sizeBytes = it.length().takeIf { size -> size > 0 }
                )
            }
    }

    private fun listMediaStoreItems(): List<ManagedFolderFile> {
        return listMediaStoreCollection(
            collectionUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            relativePath = SafeClipMediaSaveLocation.imageCaptureRelativePath,
            mimeTypeFallback = "image/jpeg"
        ) + listMediaStoreCollection(
            collectionUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
            relativePath = SafeClipMediaSaveLocation.videoClipRelativePath,
            mimeTypeFallback = "video/mp4"
        )
    }

    private fun listMediaStoreCollection(
        collectionUri: Uri,
        relativePath: String,
        mimeTypeFallback: String
    ): List<ManagedFolderFile> {
        val projection = arrayOf(
            MediaStore.MediaColumns._ID,
            MediaStore.MediaColumns.DISPLAY_NAME,
            MediaStore.MediaColumns.MIME_TYPE,
            MediaStore.MediaColumns.SIZE
        )
        val selection = "${MediaStore.MediaColumns.RELATIVE_PATH}=?"
        val selectionArgs = arrayOf("$relativePath/")
        return runCatching {
            context.contentResolver.query(collectionUri, projection, selection, selectionArgs, null)
                ?.use { cursor ->
                    val idIndex = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns._ID)
                    val nameIndex = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DISPLAY_NAME)
                    val mimeIndex = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.MIME_TYPE)
                    val sizeIndex = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.SIZE)
                    buildList {
                        while (cursor.moveToNext()) {
                            val id = cursor.getLong(idIndex)
                            val uri = Uri.withAppendedPath(collectionUri, id.toString())
                            add(
                                ManagedFolderFile(
                                    uriString = uri.toString(),
                                    displayName = cursor.getString(nameIndex) ?: "SafeClip 파일",
                                    mimeType = cursor.getString(mimeIndex) ?: mimeTypeFallback,
                                    sizeBytes = cursor.getLong(sizeIndex).takeIf { it > 0 }
                                )
                            )
                        }
                    }
                }.orEmpty()
        }.getOrDefault(emptyList())
    }

    private fun listLegacyFiles(): List<ManagedFolderFile> {
        val imageDirectory = File(
            context.getExternalFilesDir(Environment.DIRECTORY_PICTURES),
            "captures"
        )
        val clipDirectory = File(
            context.getExternalFilesDir(Environment.DIRECTORY_MOVIES),
            SafeClipMediaSaveLocation.albumName
        )
        return imageDirectory.toManagedFiles("image/jpeg") + clipDirectory.toManagedFiles("video/mp4")
    }

    private fun File.toManagedFiles(mimeType: String): List<ManagedFolderFile> {
        return listFiles()
            ?.filter { it.isFile }
            ?.map {
                ManagedFolderFile(
                    uriString = Uri.fromFile(it).toString(),
                    displayName = it.name,
                    mimeType = mimeType,
                    sizeBytes = it.length().takeIf { size -> size > 0 }
                )
            }
            .orEmpty()
    }
}
