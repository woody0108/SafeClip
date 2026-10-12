package com.glass.safeclip.data.media

import android.content.Context
import android.net.Uri
import androidx.annotation.OptIn
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.effect.OverlayEffect
import androidx.media3.transformer.Composition
import androidx.media3.transformer.DefaultEncoderFactory
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
import kotlinx.coroutines.CancellationException
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
        val stampedOutput = File(context.cacheDir, "event-timestamp-${System.nanoTime()}.mp4")
        try {
            withContext(Dispatchers.Main.immediate) {
                exportComposition(createComposition(plan, withTimestamp = false), output, location)
            }
            val originalUri = mediaPublisher.publishVideo(output, displayName)
            output.delete()
            if (plan.timeAnchor == null) {
                originalUri
            } else {
                // 원본을 먼저 보존한다. 시간 표시 사본 생성 실패 시 원본은 삭제하지 않는다.
                try {
                    withContext(Dispatchers.Main.immediate) {
                        exportComposition(createComposition(plan, withTimestamp = true), stampedOutput, location)
                    }
                    mediaPublisher.publishVideo(stampedOutput, TimestampCopyFileName.forVideo(displayName))
                } catch (error: CancellationException) {
                    throw error
                } catch (error: Exception) {
                    throw IllegalStateException("원본 영상은 저장했지만 시간 표시 사본을 저장하지 못했습니다.", error)
                }
            }
        } finally {
            output.delete()
            stampedOutput.delete()
            segmentRepository.release(ids)
        }
    }.onFailure { if (it is CancellationException) throw it }

    private fun createComposition(plan: EventClipPlan, withTimestamp: Boolean): Composition {
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
            if (withTimestamp && plan.timeAnchor != null) {
                val partStartElapsedMs = part.segment.startElapsedMs + part.clipStartMs
                val padding = TimestampBandPadding()
                val overlay = EventTimestampOverlay(plan.epochMillisAt(partStartElapsedMs), padding)
                itemBuilder.setEffects(Effects(emptyList(), listOf(padding, OverlayEffect(listOf(overlay)))))
            }
            itemBuilder.build()
        }
        return Composition.Builder(EditedMediaItemSequence.Builder(items).build()).build()
    }

    private suspend fun exportComposition(
        composition: Composition,
        output: File,
        location: EventLocation?
    ) {
        suspendCancellableCoroutine { continuation ->
            val transformer = Transformer.Builder(context)
                // 시간 띠가 추가된 크기를 지원하지 않으면 원본을 몰래 축소하지 않고 실패를 알린다.
                .setEncoderFactory(DefaultEncoderFactory.Builder(context).setEnableFallback(false).build())
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
