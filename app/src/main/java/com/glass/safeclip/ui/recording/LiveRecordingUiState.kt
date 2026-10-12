package com.glass.safeclip.ui.recording

import com.glass.safeclip.data.recording.RecordingQuality
import com.glass.safeclip.data.recording.RecordingTimeAnchor

enum class RecordingPhase { Ready, Preparing, Recording, Stopping, Error }
enum class EventWorkStatus { CapturingAfter, Exporting, Completed, Failed }
enum class PlateAnalysisStatus { Idle, Analyzing, CandidateFound, Confirmed, NoCandidate, Failed }

data class EventWorkUi(
    val id: String,
    val triggerElapsedMs: Long,
    val triggerEpochMs: Long,
    val sessionStartElapsedMs: Long,
    val timeAnchor: RecordingTimeAnchor,
    val sideDurationMs: Long,
    val status: EventWorkStatus,
    val message: String,
    val outputUri: String? = null,
    val plateStatus: PlateAnalysisStatus = PlateAnalysisStatus.Idle,
    val plateCandidates: List<String> = emptyList(),
    val confirmedPlate: String? = null,
    val plateContextImageUri: String? = null,
    val plateCropImageUri: String? = null
)

data class LiveRecordingUiState(
    val phase: RecordingPhase = RecordingPhase.Ready,
    val supportedQualities: List<RecordingQuality> = RecordingQuality.entries,
    val selectedQuality: RecordingQuality = RecordingQuality.FHD,
    val audioEnabled: Boolean = true,
    val elapsedMs: Long = 0,
    val freeBytes: Long = 0,
    val events: List<EventWorkUi> = emptyList(),
    val remoteTestEnabled: Boolean = false,
    val lastRemoteKeyLabel: String? = null,
    val message: String? = null
)
