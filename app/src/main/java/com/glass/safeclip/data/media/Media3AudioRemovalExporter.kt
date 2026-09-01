package com.glass.safeclip.data.media

import android.content.Context
import android.net.Uri
import androidx.annotation.OptIn
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.transformer.Composition
import androidx.media3.transformer.EditedMediaItem
import androidx.media3.transformer.ExportException
import androidx.media3.transformer.ExportResult
import androidx.media3.transformer.Transformer
import java.io.File
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext

@OptIn(UnstableApi::class)
class Media3AudioRemovalExporter(
    private val context: Context,
    private val mediaPublisher: SafeClipMediaPublisher = AndroidSafeClipMediaPublisher(context)
) : AudioRemovalExporter {
    override fun export(sourceUri: Uri, displayName: String): Flow<MediaExportProgress> = flow {
        emit(MediaExportProgress.Running(0))
        val output = File(context.cacheDir, "muted-${System.nanoTime()}.mp4")
        try {
            val edited = EditedMediaItem.Builder(MediaItem.fromUri(sourceUri))
                .setRemoveAudio(true)
                .build()
            withContext(Dispatchers.Main.immediate) { exportItem(edited, output) }
            val published = mediaPublisher.publishVideo(output, EventClipFileName.muted(displayName))
            emit(MediaExportProgress.Completed(published))
        } catch (error: Throwable) {
            emit(MediaExportProgress.Failed(error.message ?: "음성 제거 사본을 만들지 못했습니다."))
        } finally {
            output.delete()
        }
    }

    private suspend fun exportItem(item: EditedMediaItem, output: File) {
        suspendCancellableCoroutine { continuation ->
            val transformer = Transformer.Builder(context)
                .addListener(object : Transformer.Listener {
                    override fun onCompleted(composition: Composition, exportResult: ExportResult) {
                        if (continuation.isActive) continuation.resume(Unit)
                    }

                    override fun onError(
                        composition: Composition,
                        exportResult: ExportResult,
                        exportException: ExportException
                    ) {
                        if (continuation.isActive) continuation.resumeWithException(exportException)
                    }
                })
                .build()
            continuation.invokeOnCancellation { transformer.cancel() }
            transformer.start(item, output.absolutePath)
        }
    }
}
