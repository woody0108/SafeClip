package com.glass.safeclip.data.media

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import com.glass.safeclip.data.file.LastSelectedEventFolderStore
import java.io.File
import java.io.FileOutputStream

class AndroidFrameCaptureStore(
    private val context: Context,
    eventFolderStore: LastSelectedEventFolderStore? = null
) {
    private val eventFolder = eventFolderStore?.let { SafeClipEventFolder(context, it) }

    fun captureFrame(videoUri: Uri, displayName: String, positionMs: Long): File {
        val retriever = MediaMetadataRetriever()
        try {
            context.contentResolver.openAssetFileDescriptor(videoUri, "r")?.use { descriptor ->
                retriever.setDataSource(
                    descriptor.fileDescriptor,
                    descriptor.startOffset,
                    descriptor.length
                )
            } ?: error("영상 파일을 열 수 없습니다.")

            val bitmap = findFrame(retriever, positionMs)
                ?: error("현재 위치 근처에서 캡쳐할 프레임을 찾지 못했습니다. 재생 위치를 조금 앞뒤로 옮겨 다시 시도해주세요.")
            return saveBitmap(bitmap, displayName, positionMs)
        } finally {
            retriever.release()
        }
    }

    private fun findFrame(retriever: MediaMetadataRetriever, positionMs: Long): Bitmap? {
        val options = listOf(
            MediaMetadataRetriever.OPTION_CLOSEST,
            MediaMetadataRetriever.OPTION_CLOSEST_SYNC,
            MediaMetadataRetriever.OPTION_PREVIOUS_SYNC,
            MediaMetadataRetriever.OPTION_NEXT_SYNC
        )
        for (candidateMs in FrameCaptureSearchPlan.candidatePositions(positionMs)) {
            for (option in options) {
                val bitmap = retriever.getFrameAtTime(candidateMs * 1_000L, option)
                if (bitmap != null) return bitmap
            }
        }
        return null
    }

    private fun saveBitmap(bitmap: Bitmap, displayName: String, positionMs: Long): File {
        saveBitmapToEventFolder(bitmap, displayName, positionMs)?.let { return it }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            return saveBitmapToMediaStore(bitmap, displayName, positionMs)
        }

        val directory = File(
            context.getExternalFilesDir(Environment.DIRECTORY_PICTURES),
            "captures"
        )
        directory.mkdirs()

        val outputFile = File(directory, CaptureFileName.forVideo(displayName, positionMs))
        FileOutputStream(outputFile).use { stream ->
            if (!bitmap.compress(Bitmap.CompressFormat.JPEG, 95, stream)) {
                error("캡쳐 이미지를 JPEG로 저장하지 못했습니다.")
            }
        }
        return outputFile
    }

    private fun saveBitmapToEventFolder(bitmap: Bitmap, displayName: String, positionMs: Long): File? {
        val folder = eventFolder?.loadOrCreate() ?: return null
        val fileName = CaptureFileName.forVideo(displayName, positionMs)
        val outputFile = folder.createFile("image/jpeg", fileName) ?: return null
        context.contentResolver.openOutputStream(outputFile.uri)?.use { stream ->
            if (!bitmap.compress(Bitmap.CompressFormat.JPEG, 95, stream)) {
                error("캡쳐 이미지를 JPEG로 저장하지 못했습니다.")
            }
        } ?: return null
        return File(SafeClipMediaSaveLocation.eventFolderDisplayPath(fileName))
    }

    private fun saveBitmapToMediaStore(bitmap: Bitmap, displayName: String, positionMs: Long): File {
        val fileName = CaptureFileName.forVideo(displayName, positionMs)
        val resolver = context.contentResolver
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
            put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
            put(MediaStore.Images.Media.RELATIVE_PATH, SafeClipMediaSaveLocation.imageCaptureRelativePath)
            put(MediaStore.Images.Media.IS_PENDING, 1)
        }
        val imageUri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
            ?: error("캡쳐 이미지를 저장할 위치를 만들지 못했습니다.")

        try {
            resolver.openOutputStream(imageUri)?.use { stream ->
                if (!bitmap.compress(Bitmap.CompressFormat.JPEG, 95, stream)) {
                    error("캡쳐 이미지를 JPEG로 저장하지 못했습니다.")
                }
            } ?: error("캡쳐 이미지를 저장하지 못했습니다.")

            values.clear()
            values.put(MediaStore.Images.Media.IS_PENDING, 0)
            resolver.update(imageUri, values, null, null)

            return File("${SafeClipMediaSaveLocation.imageCaptureRelativePath}/$fileName")
        } catch (error: Throwable) {
            resolver.delete(imageUri, null, null)
            throw error
        }
    }
}
