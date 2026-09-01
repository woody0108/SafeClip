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
    eventFolderStore: LastSelectedEventFolderStore? = null,
    private val useAccessibleSelectionFallback: () -> Boolean = { false }
) {
    private val eventFolder = eventFolderStore?.let { SafeClipEventFolder(context, it) }

    fun listSavedItems(): List<ManagedFolderFile> {
        val currentItems = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            listMediaStoreItems()
        } else {
            listLegacyFiles()
        }
        return (currentItems + listEventFolderItems().orEmpty())
            .distinctBy(ManagedFolderFile::uriString)
    }

    private fun listEventFolderItems(): List<ManagedFolderFile>? {
        val folder = eventFolder?.loadExisting() ?: return null
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
            MediaStore.MediaColumns.SIZE,
            MediaStore.MediaColumns.RELATIVE_PATH
        )
        val directItems = queryMediaStoreCollection(
            collectionUri = collectionUri,
            projection = projection,
            selection = "${MediaStore.MediaColumns.RELATIVE_PATH}=?",
            selectionArgs = arrayOf("$relativePath/"),
            relativePath = relativePath,
            mimeTypeFallback = mimeTypeFallback
        )
        if (directItems.isNotEmpty() || !useAccessibleSelectionFallback()) {
            return directItems
        }

        // 제한 접근에서는 제조사 MediaStore가 경로 selection을 먼저 적용해 빈 결과를 줄 수 있다.
        return queryMediaStoreCollection(
            collectionUri = collectionUri,
            projection = projection,
            selection = null,
            selectionArgs = null,
            relativePath = relativePath,
            mimeTypeFallback = mimeTypeFallback
        )
    }

    private fun queryMediaStoreCollection(
        collectionUri: Uri,
        projection: Array<String>,
        selection: String?,
        selectionArgs: Array<String>?,
        relativePath: String,
        mimeTypeFallback: String
    ): List<ManagedFolderFile> {
        return runCatching {
            context.contentResolver.query(collectionUri, projection, selection, selectionArgs, null)
                ?.use { cursor ->
                    val idIndex = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns._ID)
                    val nameIndex = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DISPLAY_NAME)
                    val mimeIndex = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.MIME_TYPE)
                    val sizeIndex = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.SIZE)
                    val pathIndex = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.RELATIVE_PATH)
                    buildList {
                        while (cursor.moveToNext()) {
                            val itemPath = cursor.getString(pathIndex)?.trimEnd('/')
                            if (itemPath != relativePath.trimEnd('/')) continue

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
        @Suppress("DEPRECATION")
        val safeClipDirectory = File(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM),
            SafeClipMediaSaveLocation.albumName
        )
        return safeClipDirectory.listFiles()
            ?.filter(File::isFile)
            ?.map { file ->
                val mimeType = if (file.extension.equals("mp4", ignoreCase = true)) {
                    "video/mp4"
                } else {
                    "image/jpeg"
                }
                ManagedFolderFile(
                    uriString = Uri.fromFile(file).toString(),
                    displayName = file.name,
                    mimeType = mimeType,
                    sizeBytes = file.length().takeIf { size -> size > 0 }
                )
            }
            .orEmpty()
    }
}
