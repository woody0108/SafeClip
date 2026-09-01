package com.glass.safeclip.data.media

import android.content.ContentValues
import android.content.Context
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AndroidSafeClipMediaPublisher(
    private val context: Context
) : SafeClipMediaPublisher {
    override suspend fun publishVideo(source: File, displayName: String): Uri {
        return publish(
            source = source,
            displayName = displayName,
            mimeType = "video/mp4",
            collection = MediaStore.Video.Media.EXTERNAL_CONTENT_URI
        )
    }

    override suspend fun publishImage(
        source: File,
        displayName: String,
        mimeType: String
    ): Uri {
        return publish(
            source = source,
            displayName = displayName,
            mimeType = mimeType,
            collection = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        )
    }

    private suspend fun publish(
        source: File,
        displayName: String,
        mimeType: String,
        collection: Uri
    ): Uri = withContext(Dispatchers.IO) {
        require(source.isFile) { "게시할 미디어 파일을 찾을 수 없습니다." }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            publishToMediaStore(source, displayName, mimeType, collection)
        } else {
            publishToLegacyDcim(source, displayName, mimeType)
        }
    }

    private fun publishToMediaStore(
        source: File,
        displayName: String,
        mimeType: String,
        collection: Uri
    ): Uri {
        val resolver = context.contentResolver
        val values = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, displayName)
            put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
            put(MediaStore.MediaColumns.RELATIVE_PATH, SafeClipMediaSaveLocation.relativePath)
            put(MediaStore.MediaColumns.IS_PENDING, 1)
        }
        val outputUri = resolver.insert(collection, values)
            ?: error("SafeClip 저장 위치를 만들지 못했습니다.")

        try {
            resolver.openOutputStream(outputUri, "w")?.use { output ->
                FileInputStream(source).use { input -> input.copyTo(output) }
            } ?: error("SafeClip 파일을 저장하지 못했습니다.")

            values.clear()
            values.put(MediaStore.MediaColumns.IS_PENDING, 0)
            resolver.update(outputUri, values, null, null)
            return outputUri
        } catch (error: Throwable) {
            resolver.delete(outputUri, null, null)
            throw error
        }
    }

    @Suppress("DEPRECATION")
    private fun publishToLegacyDcim(
        source: File,
        displayName: String,
        mimeType: String
    ): Uri {
        val directory = File(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM),
            SafeClipMediaSaveLocation.albumName
        ).apply { mkdirs() }
        require(directory.isDirectory) { "DCIM/SafeClip 폴더를 만들지 못했습니다." }

        val outputFile = uniqueFile(directory, displayName)
        FileInputStream(source).use { input ->
            FileOutputStream(outputFile).use { output -> input.copyTo(output) }
        }
        MediaScannerConnection.scanFile(
            context,
            arrayOf(outputFile.absolutePath),
            arrayOf(mimeType),
            null
        )
        return Uri.fromFile(outputFile)
    }

    private fun uniqueFile(directory: File, displayName: String): File {
        val initial = File(directory, displayName)
        if (!initial.exists()) return initial

        val baseName = displayName.substringBeforeLast('.', displayName)
        val extension = displayName.substringAfterLast('.', "")
        var index = 2
        while (true) {
            val suffix = if (extension.isEmpty()) "_$index" else "_$index.$extension"
            val candidate = File(directory, "$baseName$suffix")
            if (!candidate.exists()) return candidate
            index += 1
        }
    }
}
