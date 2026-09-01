package com.glass.safeclip.data.recording

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.util.Range
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.FileOutputOptions
import androidx.camera.video.Quality
import androidx.camera.video.QualitySelector
import androidx.camera.video.Recorder
import androidx.camera.video.Recording
import androidx.camera.video.VideoCapture
import androidx.camera.video.VideoRecordEvent
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.google.common.util.concurrent.ListenableFuture
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.suspendCancellableCoroutine

class CameraXRecordingSessionController(
    private val context: Context,
    private val segmentRepository: RollingSegmentRepository,
    private val clock: () -> Long = SystemClock::elapsedRealtime
) : RecordingSessionController {
    private val mutableEvents = MutableSharedFlow<RecordingEngineEvent>(
        extraBufferCapacity = 16,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    override val events: Flow<RecordingEngineEvent> = mutableEvents

    private val mainExecutor = ContextCompat.getMainExecutor(context)
    private val handler = Handler(Looper.getMainLooper())
    private var cameraProvider: ProcessCameraProvider? = null
    private var videoCapture: VideoCapture<Recorder>? = null
    private var recording: Recording? = null
    private var pendingSegment: PendingSegment? = null
    private var active = false
    private var audioEnabled = false
    private var releaseRequested = false

    private val rotateSegment = Runnable {
        if (active) recording?.stop()
    }

    override suspend fun bind(
        owner: LifecycleOwner,
        surfaceProvider: Preview.SurfaceProvider,
        requestedQuality: RecordingQuality
    ): RecordingQuality {
        releaseRequested = false
        val provider = ProcessCameraProvider.getInstance(context).await()
        val cameraInfo = provider.getCameraInfo(CameraSelector.DEFAULT_BACK_CAMERA)
        val supported = QualitySelector.getSupportedQualities(cameraInfo)
            .mapNotNull { quality -> quality.toSafeClip() }
            .toSet()
        val selected = RecordingQuality.select(requestedQuality, supported)
        val recorder = Recorder.Builder()
            .setQualitySelector(QualitySelector.from(selected.toCameraX()))
            .build()
        val capture = VideoCapture.Builder(recorder)
            .setTargetFrameRate(Range(30, 30))
            .build()
        val preview = Preview.Builder().build().also { it.setSurfaceProvider(surfaceProvider) }

        provider.unbindAll()
        provider.bindToLifecycle(owner, CameraSelector.DEFAULT_BACK_CAMERA, preview, capture)
        cameraProvider = provider
        videoCapture = capture
        return selected
    }

    override fun start(audioEnabled: Boolean) {
        check(ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            "카메라 권한이 필요합니다."
        }
        check(videoCapture != null) { "카메라 미리보기를 먼저 준비해주세요." }
        if (active) return
        this.audioEnabled = audioEnabled &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
        // 순환 조각은 현재 녹화 세션에서만 사용합니다. 이전 실행의 monotonic 시각과 섞지 않습니다.
        segmentRepository.beginSession()
        active = true
        startNextSegment()
    }

    override fun stop() {
        if (!active) return
        active = false
        handler.removeCallbacks(rotateSegment)
        recording?.stop() ?: mutableEvents.tryEmit(RecordingEngineEvent.Stopped)
    }

    override fun release() {
        releaseRequested = true
        stop()
        if (recording == null) finishRelease()
    }

    private fun finishRelease() {
        cameraProvider?.unbindAll()
        cameraProvider = null
        videoCapture = null
    }

    private fun startNextSegment() {
        val capture = videoCapture ?: return fail("카메라 녹화기가 준비되지 않았습니다.")
        val pending = runCatching { segmentRepository.createPending(clock()) }
            .getOrElse { return fail("녹화 저장 공간을 준비하지 못했습니다.", it) }
        pendingSegment = pending

        var pendingRecording = capture.output
            .prepareRecording(context, FileOutputOptions.Builder(pending.file).build())
        if (audioEnabled &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
        ) {
            pendingRecording = pendingRecording.withAudioEnabled()
        }
        recording = pendingRecording.start(mainExecutor, ::onVideoEvent)
    }

    private fun onVideoEvent(event: VideoRecordEvent) {
        when (event) {
            is VideoRecordEvent.Start -> {
                mutableEvents.tryEmit(RecordingEngineEvent.Started(pendingSegment?.startElapsedMs ?: clock()))
                handler.removeCallbacks(rotateSegment)
                handler.postDelayed(rotateSegment, SEGMENT_DURATION_MS)
            }
            is VideoRecordEvent.Finalize -> finalizeSegment(event)
        }
    }

    private fun finalizeSegment(event: VideoRecordEvent.Finalize) {
        handler.removeCallbacks(rotateSegment)
        recording = null
        val pending = pendingSegment.also { pendingSegment = null }
        if (event.hasError() || pending == null) {
            pending?.file?.delete()
            fail("녹화 조각을 완성하지 못했습니다. (${event.error})", event.cause)
            return
        }

        runCatching { segmentRepository.finalize(pending, clock()) }
            .onSuccess { segment ->
                mutableEvents.tryEmit(RecordingEngineEvent.SegmentFinalized(segment))
                segmentRepository.cleanup(clock())
                if (active) {
                    startNextSegment()
                } else {
                    mutableEvents.tryEmit(RecordingEngineEvent.Stopped)
                    if (releaseRequested) finishRelease()
                }
            }
            .onFailure { fail("녹화 조각을 저장하지 못했습니다.", it) }
    }

    private fun fail(message: String, cause: Throwable? = null) {
        active = false
        handler.removeCallbacks(rotateSegment)
        mutableEvents.tryEmit(RecordingEngineEvent.Error(message, cause))
        if (releaseRequested) finishRelease()
    }

    private fun RecordingQuality.toCameraX(): Quality = when (this) {
        RecordingQuality.FHD -> Quality.FHD
        RecordingQuality.HD -> Quality.HD
        RecordingQuality.SD -> Quality.SD
    }

    private fun Quality.toSafeClip(): RecordingQuality? = when (this) {
        Quality.FHD -> RecordingQuality.FHD
        Quality.HD -> RecordingQuality.HD
        Quality.SD -> RecordingQuality.SD
        else -> null
    }

    private suspend fun <T> ListenableFuture<T>.await(): T = suspendCancellableCoroutine { continuation ->
        addListener(
            {
                runCatching { get() }
                    .onSuccess(continuation::resume)
                    .onFailure(continuation::resumeWithException)
            },
            mainExecutor
        )
        continuation.invokeOnCancellation { cancel(true) }
    }

    private companion object {
        const val SEGMENT_DURATION_MS = 60_000L
    }
}
