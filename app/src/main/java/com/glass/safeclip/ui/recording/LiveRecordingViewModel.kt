package com.glass.safeclip.ui.recording

import android.os.SystemClock
import android.view.KeyEvent
import androidx.camera.core.Preview
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.glass.safeclip.data.media.EventClipAssembler
import com.glass.safeclip.data.media.EventClipFileName
import com.glass.safeclip.data.recording.EventLocation
import com.glass.safeclip.data.recording.EventLocationProvider
import com.glass.safeclip.data.recording.EventWindowPlanner
import com.glass.safeclip.data.recording.RecordingEngineEvent
import com.glass.safeclip.data.recording.RecordingQuality
import com.glass.safeclip.data.recording.RecordingSessionController
import com.glass.safeclip.data.recording.RecordingTimeAnchor
import com.glass.safeclip.data.recording.RemoteEventDebouncer
import com.glass.safeclip.data.recording.RollingSegmentRepository
import java.time.Instant
import java.time.ZoneId
import java.util.UUID
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class LiveRecordingViewModel(
    private val controller: RecordingSessionController,
    private val segments: RollingSegmentRepository,
    private val assembler: EventClipAssembler,
    private val locationProvider: EventLocationProvider = EventLocationProvider.None,
    private val clock: () -> Long = SystemClock::elapsedRealtime,
    private val wallClock: () -> Long = System::currentTimeMillis
) : ViewModel() {
    private val mutableState = MutableStateFlow(LiveRecordingUiState(freeBytes = segments.availableBytes()))
    val state: StateFlow<LiveRecordingUiState> = mutableState.asStateFlow()
    private val debouncer = RemoteEventDebouncer(3_000)
    private val exportMutex = Mutex()
    private val eventLocations = mutableMapOf<String, Deferred<EventLocation?>>()
    private var ticker: Job? = null
    private var recordingStartedAt: Long? = null
    private var recordingTimeAnchor: RecordingTimeAnchor? = null
    private var stoppingSessionStart: Long? = null
    private var boundOwner: LifecycleOwner? = null
    private var boundSurfaceProvider: Preview.SurfaceProvider? = null
    private var screenActive = true
    private var deferredBind = false

    init {
        viewModelScope.launch { controller.events.collect(::handleEngineEvent) }
    }

    fun bind(owner: LifecycleOwner, surfaceProvider: Preview.SurfaceProvider) {
        screenActive = true
        boundOwner = owner
        boundSurfaceProvider = surfaceProvider
        if (state.value.phase == RecordingPhase.Stopping) {
            deferredBind = true
            return
        }
        prepareCamera(owner, surfaceProvider)
    }

    private fun prepareCamera(owner: LifecycleOwner, surfaceProvider: Preview.SurfaceProvider) {
        mutableState.update { it.copy(phase = RecordingPhase.Preparing, message = null) }
        viewModelScope.launch {
            runCatching {
                controller.bind(owner, surfaceProvider, state.value.selectedQuality)
            }.onSuccess { selected ->
                if (screenActive) {
                    mutableState.update { it.copy(phase = RecordingPhase.Ready, selectedQuality = selected) }
                } else {
                    controller.release()
                }
            }.onFailure { error ->
                if (screenActive) {
                    mutableState.update {
                        it.copy(phase = RecordingPhase.Error, message = error.message ?: "카메라를 준비하지 못했습니다.")
                    }
                }
            }
        }
    }

    fun startRecording() {
        if (state.value.phase != RecordingPhase.Ready) return
        mutableState.update { it.copy(phase = RecordingPhase.Preparing, message = null) }
        runCatching { controller.start(state.value.audioEnabled) }
            .onFailure { error ->
                mutableState.update { it.copy(phase = RecordingPhase.Error, message = error.message) }
            }
    }

    fun stopRecording() {
        if (state.value.phase != RecordingPhase.Recording) return
        mutableState.update { it.copy(phase = RecordingPhase.Stopping) }
        controller.stop()
    }

    fun requestEvent(elapsedMs: Long = clock()) {
        if (state.value.phase != RecordingPhase.Recording) return
        val sessionStart = recordingStartedAt ?: return
        val timeAnchor = recordingTimeAnchor ?: return
        val id = UUID.randomUUID().toString()
        val triggerEpochMs = timeAnchor.epochMillis + (elapsedMs - timeAnchor.elapsedRealtimeMs)
        eventLocations[id] = viewModelScope.async {
            runCatching { locationProvider.captureLocation(triggerEpochMs) }.getOrNull()
        }
        val event = EventWorkUi(
            id = id,
            triggerElapsedMs = elapsedMs,
            triggerEpochMs = triggerEpochMs,
            sessionStartElapsedMs = sessionStart,
            timeAnchor = timeAnchor,
            status = EventWorkStatus.CapturingAfter,
            message = "이후 1분을 녹화하고 있습니다."
        )
        mutableState.update { it.copy(events = it.events + event) }
    }

    fun setAudioEnabled(enabled: Boolean) {
        if (state.value.phase == RecordingPhase.Ready) mutableState.update { it.copy(audioEnabled = enabled) }
    }

    fun setQuality(quality: RecordingQuality) {
        if (state.value.phase != RecordingPhase.Ready || quality == state.value.selectedQuality) return
        mutableState.update { it.copy(selectedQuality = quality) }
        val owner = boundOwner
        val provider = boundSurfaceProvider
        if (owner != null && provider != null) bind(owner, provider)
    }

    fun setRemoteTestEnabled(enabled: Boolean) {
        mutableState.update { it.copy(remoteTestEnabled = enabled, lastRemoteKeyLabel = null) }
    }

    fun onRemoteKey(keyCode: Int, @Suppress("UNUSED_PARAMETER") elapsedMs: Long): Boolean {
        val label = remoteKeyLabel(keyCode) ?: return false
        val current = state.value
        if (current.phase == RecordingPhase.Ready && current.remoteTestEnabled) {
            mutableState.update { it.copy(lastRemoteKeyLabel = "$label 인식") }
            return true
        }
        val recordingElapsedMs = clock()
        if (current.phase != RecordingPhase.Recording || !debouncer.accept(recordingElapsedMs)) return false
        // KeyEvent.eventTime은 절전 시간을 제외하므로 녹화 기준 시계와 섞지 않습니다.
        requestEvent(recordingElapsedMs)
        return true
    }

    fun leaveScreen() {
        screenActive = false
        deferredBind = false
        ticker?.cancel()
        if (state.value.phase == RecordingPhase.Recording) {
            mutableState.update { it.copy(phase = RecordingPhase.Stopping) }
        }
        controller.release()
    }

    private fun handleEngineEvent(event: RecordingEngineEvent) {
        when (event) {
            is RecordingEngineEvent.Started -> {
                if (recordingStartedAt == null) {
                    recordingStartedAt = event.startElapsedMs
                    recordingTimeAnchor = RecordingTimeAnchor(clock(), wallClock())
                    stoppingSessionStart = null
                    debouncer.reset()
                }
                mutableState.update {
                    it.copy(phase = if (screenActive) RecordingPhase.Recording else RecordingPhase.Stopping)
                }
                if (screenActive) startTicker()
            }
            is RecordingEngineEvent.SegmentFinalized -> completeReadyEvents(event.segment.endElapsedMs)
            is RecordingEngineEvent.Error -> {
                ticker?.cancel()
                mutableState.update { it.copy(phase = RecordingPhase.Error, message = event.message) }
            }
            RecordingEngineEvent.Stopped -> {
                ticker?.cancel()
                val sessionStart = recordingStartedAt
                stoppingSessionStart = sessionStart
                val availableEnd = sessionStart?.let { start ->
                    segments.segments()
                        .asSequence()
                        .filter { it.endElapsedMs > start }
                        .maxOfOrNull { it.endElapsedMs }
                } ?: clock()
                completeReadyEvents(availableEnd, force = true)
                finishStopIfReady()
            }
        }
    }

    private fun completeReadyEvents(availableEnd: Long, force: Boolean = false) {
        state.value.events
            .filter { it.status == EventWorkStatus.CapturingAfter }
            .filter { force || availableEnd >= it.triggerElapsedMs + 60_000L }
            .forEach { work -> exportEvent(work, availableEnd) }
    }

    private fun exportEvent(work: EventWorkUi, availableEnd: Long) {
        updateEvent(work.id, EventWorkStatus.Exporting, "이벤트 영상을 만드는 중입니다.")
        viewModelScope.launch {
            runCatching {
                exportMutex.withLock {
                    val sessionSegments = segments.segments().filter { segment ->
                        segment.endElapsedMs > work.sessionStartElapsedMs &&
                            segment.startElapsedMs < availableEnd
                    }
                    val plan = EventWindowPlanner.plan(
                        triggerElapsedMs = work.triggerElapsedMs,
                        availableEndElapsedMs = availableEnd,
                        segments = sessionSegments,
                        timeAnchor = work.timeAnchor
                    )
                    assembler.assemble(
                        plan,
                        EventClipFileName.event(
                            Instant.ofEpochMilli(work.triggerEpochMs),
                            ZoneId.of("Asia/Seoul")
                        ),
                        eventLocations.remove(work.id)?.await()
                    ).getOrThrow() to plan.isShort
                }
            }.onSuccess { (uri, short) ->
                updateEvent(
                    work.id,
                    EventWorkStatus.Completed,
                    if (short) "짧은 이벤트 영상이 저장되었습니다." else "이벤트 영상이 저장되었습니다.",
                    uri.toString()
                )
            }.onFailure { error ->
                updateEvent(work.id, EventWorkStatus.Failed, eventErrorMessage(error))
            }
            finishStopIfReady()
        }
    }

    private fun finishStopIfReady() {
        val sessionStart = stoppingSessionStart ?: return
        val hasPendingEvent = state.value.events.any { work ->
            work.sessionStartElapsedMs == sessionStart &&
                work.status in setOf(EventWorkStatus.CapturingAfter, EventWorkStatus.Exporting)
        }
        if (hasPendingEvent) return

        recordingStartedAt = null
        recordingTimeAnchor = null
        stoppingSessionStart = null
        mutableState.update { it.copy(phase = RecordingPhase.Ready, elapsedMs = 0) }
        if (screenActive && deferredBind) {
            deferredBind = false
            val owner = boundOwner
            val provider = boundSurfaceProvider
            if (owner != null && provider != null) prepareCamera(owner, provider)
        }
    }

    private fun eventErrorMessage(error: Throwable): String {
        return when (error.message) {
            "The available event window is empty.",
            "At least one segment is required." -> "버튼 시점과 겹치는 녹화 구간이 없어 저장하지 못했습니다."
            else -> error.message ?: "이벤트 저장에 실패했습니다."
        }
    }

    private fun updateEvent(id: String, status: EventWorkStatus, message: String, uri: String? = null) {
        mutableState.update { current ->
            current.copy(events = current.events.map { if (it.id == id) it.copy(status = status, message = message, outputUri = uri) else it })
        }
    }

    private fun startTicker() {
        ticker?.cancel()
        ticker = viewModelScope.launch {
            while (isActive) {
                mutableState.update {
                    val startedAt = recordingStartedAt ?: clock()
                    it.copy(elapsedMs = (clock() - startedAt).coerceAtLeast(0), freeBytes = segments.availableBytes())
                }
                delay(1_000)
            }
        }
    }

    private fun remoteKeyLabel(keyCode: Int): String? = when (keyCode) {
        KeyEvent.KEYCODE_VOLUME_UP -> "볼륨 올리기 키"
        KeyEvent.KEYCODE_VOLUME_DOWN -> "볼륨 내리기 키"
        KeyEvent.KEYCODE_CAMERA -> "카메라 키"
        else -> null
    }

    override fun onCleared() {
        leaveScreen()
    }
}
