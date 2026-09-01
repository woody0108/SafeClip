package com.glass.safeclip.data.media

import android.content.Context
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.net.Uri
import com.glass.safeclip.data.file.LastSelectedEventFolderStore
import java.io.File
import java.io.FileOutputStream

class AndroidFrameCaptureStore(
    private val context: Context,
    @Suppress("UNUSED_PARAMETER") eventFolderStore: LastSelectedEventFolderStore? = null,
    private val mediaPublisher: SafeClipMediaPublisher = AndroidSafeClipMediaPublisher(context)
) {
    suspend fun captureFrame(videoUri: Uri, displayName: String, positionMs: Long): File {
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

    private suspend fun saveBitmap(bitmap: Bitmap, displayName: String, positionMs: Long): File {
        val fileName = CaptureFileName.forVideo(displayName, positionMs)
        val tempDirectory = File(context.cacheDir ?: context.filesDir, "safeclip-capture-temp")
            .apply { mkdirs() }
        val tempFile = File(tempDirectory, fileName)
        try {
            FileOutputStream(tempFile).use { stream ->
                if (!bitmap.compress(Bitmap.CompressFormat.JPEG, 95, stream)) {
                    error("캡쳐 이미지를 JPEG로 저장하지 못했습니다.")
                }
            }
            mediaPublisher.publishImage(tempFile, fileName, "image/jpeg")
            return File(SafeClipMediaSaveLocation.displayPath(fileName))
        } finally {
            tempFile.delete()
        }
    }
}
