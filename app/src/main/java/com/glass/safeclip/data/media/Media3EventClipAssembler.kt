package com.glass.safeclip.data.media

import android.content.Context
import android.net.Uri
import androidx.annotation.OptIn
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.effect.OverlayEffect
import androidx.media3.transformer.Composition
import androidx.media3.transformer.EditedMediaItem
import androidx.media3.transformer.EditedMediaItemSequence
import androidx.media3.transformer.Effects
import androidx.media3.transformer.ExportException
import androidx.media3.transformer.ExportResult
import androidx.media3.transformer.InAppMp4Muxer
import androidx.media3.transformer.Transformer
import com.glass.safeclip.data.recording.EventClipPlan
import com.glass.safeclip.data.recording.EventLocation
import com.glass.safeclip.data.recording.RollingSegmentRepository
import java.io.File
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext

@OptIn(UnstableApi::class)
class Media3EventClipAssembler(
    private val context: Context,
    private val segmentRepository: RollingSegmentRepository,
    private val mediaPublisher: SafeClipMediaPublisher = AndroidSafeClipMediaPublisher(context)
) : EventClipAssembler {
    override suspend fun assemble(
        plan: EventClipPlan,
        displayName: String,
        location: EventLocation?
    ): Result<Uri> = runCatching {
        require(plan.parts.isNotEmpty()) { "이벤트로 저장할 녹화 조각이 없습니다." }
        val ids = plan.parts.map { it.segment.id }.toSet()
        segmentRepository.protect(ids)
        val output = File(context.cacheDir, "event-${System.nanoTime()}.mp4")
        try {
            val items = plan.parts.map { part ->
                val mediaItem = MediaItem.Builder()
                    .setUri(Uri.parse(part.segment.uriString))
                    .setClippingConfiguration(
                        MediaItem.ClippingConfiguration.Builder()
                            .setStartPositionMs(part.clipStartMs)
                            .setEndPositionMs(part.clipEndMs)
                            .build()
                    )
                    .build()
                val itemBuilder = EditedMediaItem.Builder(mediaItem)
                plan.timeAnchor?.let {
                    val partStartElapsedMs = part.segment.startElapsedMs + part.clipStartMs
                    itemBuilder.setEffects(
                        Effects(
                            emptyList(),
                            listOf(OverlayEffect(listOf(EventTimestampOverlay(plan.epochMillisAt(partStartElapsedMs)))))
                        )
                    )
                }
                itemBuilder.build()
            }
            val sequence = EditedMediaItemSequence.Builder(items).build()
            val composition = Composition.Builder(sequence).build()
            withContext(Dispatchers.Main.immediate) {
                exportComposition(composition, output, location)
            }
            mediaPublisher.publishVideo(output, displayName)
        } finally {
            output.delete()
            segmentRepository.release(ids)
        }
    }

    private suspend fun exportComposition(
        composition: Composition,
        output: File,
        location: EventLocation?
    ) {
        suspendCancellableCoroutine { continuation ->
            val transformer = Transformer.Builder(context)
                .setMuxerFactory(InAppMp4Muxer.Factory(EventLocationMetadataProvider(location)))
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
            transformer.start(composition, output.absolutePath)
        }
    }
}
