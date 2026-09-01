package com.glass.safeclip.data.recording

import androidx.camera.core.Preview
import androidx.lifecycle.LifecycleOwner
import kotlinx.coroutines.flow.Flow

sealed interface RecordingEngineEvent {
    data class Started(val startElapsedMs: Long) : RecordingEngineEvent
    data class SegmentFinalized(val segment: RollingSegment) : RecordingEngineEvent
    data class Error(val message: String, val cause: Throwable? = null) : RecordingEngineEvent
    data object Stopped : RecordingEngineEvent
}

interface RecordingSessionController {
    val events: Flow<RecordingEngineEvent>

    suspend fun bind(
        owner: LifecycleOwner,
        surfaceProvider: Preview.SurfaceProvider,
        requestedQuality: RecordingQuality
    ): RecordingQuality

    fun start(audioEnabled: Boolean)
    fun stop()
    fun release()
}
