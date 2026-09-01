package com.glass.safeclip.ui.recording

import android.view.KeyEvent
import androidx.camera.core.Preview
import androidx.lifecycle.LifecycleOwner
import com.glass.safeclip.data.media.EventClipAssembler
import com.glass.safeclip.data.recording.EventLocation
import com.glass.safeclip.data.recording.EventLocationProvider
import com.glass.safeclip.data.recording.RecordingEngineEvent
import com.glass.safeclip.data.recording.RecordingQuality
import com.glass.safeclip.data.recording.RecordingSessionController
import com.glass.safeclip.data.recording.RollingSegmentRepository
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

@OptIn(ExperimentalCoroutinesApi::class)
class LiveRecordingViewModelTest {
    @get:Rule
    val temporaryFolder = TemporaryFolder()

    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun remoteKeyUsesRecordingClockInsteadOfKeyEventClock() = runTest(dispatcher) {
        val controller = FakeRecordingSessionController()
        val viewModel = createViewModel(controller, clock = { 1_000_000 })
        runCurrent()
        controller.emit(RecordingEngineEvent.Started(900_000))
        runCurrent()

        assertTrue(viewModel.onRemoteKey(KeyEvent.KEYCODE_VOLUME_UP, elapsedMs = 10_000))

        assertEquals(1_000_000, viewModel.state.value.events.single().triggerElapsedMs)
        viewModel.leaveScreen()
    }

    @Test
    fun stoppedRecordingWaitsForPendingEventExportBeforeBecomingReady() = runTest(dispatcher) {
        val controller = FakeRecordingSessionController()
        val assembler = BlockingEventClipAssembler()
        val repository = createRepository()
        addSegment(repository, start = 100_000, end = 130_000)
        val viewModel = LiveRecordingViewModel(controller, repository, assembler, clock = { 130_000 })
        runCurrent()
        controller.emit(RecordingEngineEvent.Started(100_000))
        runCurrent()
        viewModel.requestEvent(120_000)
        viewModel.stopRecording()

        controller.emit(RecordingEngineEvent.Stopped)
        runCurrent()
        assembler.started.await()

        assertEquals(RecordingPhase.Stopping, viewModel.state.value.phase)
        assertEquals(EventWorkStatus.Exporting, viewModel.state.value.events.single().status)

        assembler.release.complete(Unit)
        runCurrent()
        assertEquals(RecordingPhase.Ready, viewModel.state.value.phase)
    }

    @Test
    fun newRecordingDoesNotIncludeSegmentsFromPreviousRecording() = runTest(dispatcher) {
        val controller = FakeRecordingSessionController()
        val assembler = RecordingEventClipAssembler()
        val repository = createRepository()
        addSegment(repository, start = 0, end = 60_000)
        addSegment(repository, start = 100_000, end = 130_000)
        val viewModel = LiveRecordingViewModel(controller, repository, assembler, clock = { 130_000 })
        runCurrent()
        controller.emit(RecordingEngineEvent.Started(100_000))
        runCurrent()
        viewModel.requestEvent(120_000)
        viewModel.stopRecording()

        controller.emit(RecordingEngineEvent.Stopped)
        runCurrent()

        assertEquals(listOf(100_000L), assembler.segmentStarts.single())
    }

    @Test
    fun overlappingEventsAreExportedOneAtATime() = runTest(dispatcher) {
        val controller = FakeRecordingSessionController()
        val assembler = SerialCheckEventClipAssembler()
        val repository = createRepository()
        addSegment(repository, start = 100_000, end = 140_000)
        val viewModel = LiveRecordingViewModel(controller, repository, assembler, clock = { 140_000 })
        runCurrent()
        controller.emit(RecordingEngineEvent.Started(100_000))
        runCurrent()
        viewModel.requestEvent(120_000)
        viewModel.requestEvent(125_000)
        viewModel.stopRecording()
        controller.emit(RecordingEngineEvent.Stopped)
        runCurrent()

        assembler.allowNext.complete(Unit)
        runCurrent()

        assertEquals(1, assembler.maxConcurrent)
        assertEquals(2, assembler.exportCount)
    }

    @Test
    fun eventFileNameUsesTheActualButtonTime() = runTest(dispatcher) {
        val controller = FakeRecordingSessionController()
        val assembler = RecordingEventClipAssembler()
        val repository = createRepository()
        addSegment(repository, start = 100_000, end = 140_000)
        val viewModel = LiveRecordingViewModel(
            controller = controller,
            segments = repository,
            assembler = assembler,
            clock = { 100_000 },
            wallClock = { 1_787_725_421_000 }
        )
        runCurrent()
        controller.emit(RecordingEngineEvent.Started(100_000))
        runCurrent()
        viewModel.requestEvent(120_000)
        viewModel.stopRecording()
        controller.emit(RecordingEngineEvent.Stopped)
        runCurrent()

        assertEquals("SC_EVENT_20260826_152401.mp4", assembler.displayNames.single())
    }

    @Test
    fun eventLocationCapturedAtButtonTimeIsForwardedToTheVideoAssembler() = runTest(dispatcher) {
        val controller = FakeRecordingSessionController()
        val assembler = RecordingEventClipAssembler()
        val repository = createRepository()
        addSegment(repository, start = 100_000, end = 140_000)
        val expectedLocation = EventLocation(
            latitude = 35.5396,
            longitude = 129.3114,
            accuracyMeters = 8.5f,
            capturedAtEpochMs = 1_787_725_441_000
        )
        val viewModel = LiveRecordingViewModel(
            controller = controller,
            segments = repository,
            assembler = assembler,
            locationProvider = EventLocationProvider { _ -> expectedLocation },
            clock = { 100_000 },
            wallClock = { 1_787_725_421_000 }
        )
        runCurrent()
        controller.emit(RecordingEngineEvent.Started(100_000))
        runCurrent()

        viewModel.requestEvent(120_000)
        viewModel.stopRecording()
        controller.emit(RecordingEngineEvent.Stopped)
        runCurrent()

        assertEquals(listOf(expectedLocation), assembler.locations)
    }

    @Test
    fun locationFailureDoesNotPreventEventVideoAssembly() = runTest(dispatcher) {
        val controller = FakeRecordingSessionController()
        val assembler = RecordingEventClipAssembler()
        val repository = createRepository()
        addSegment(repository, start = 100_000, end = 140_000)
        val viewModel = LiveRecordingViewModel(
            controller = controller,
            segments = repository,
            assembler = assembler,
            locationProvider = EventLocationProvider { error("location unavailable") },
            clock = { 130_000 }
        )
        runCurrent()
        controller.emit(RecordingEngineEvent.Started(100_000))
        runCurrent()

        viewModel.requestEvent(120_000)
        viewModel.stopRecording()
        controller.emit(RecordingEngineEvent.Stopped)
        runCurrent()

        assertEquals(listOf<EventLocation?>(null), assembler.locations)
    }

    @Test
    fun emptyEventWindowFinishesStoppingWithKoreanFailureMessage() = runTest(dispatcher) {
        val controller = FakeRecordingSessionController()
        val viewModel = createViewModel(controller, clock = { 130_000 })
        runCurrent()
        controller.emit(RecordingEngineEvent.Started(100_000))
        runCurrent()
        viewModel.requestEvent(120_000)
        viewModel.stopRecording()
        controller.emit(RecordingEngineEvent.Stopped)
        runCurrent()

        assertEquals(RecordingPhase.Ready, viewModel.state.value.phase)
        assertEquals(EventWorkStatus.Failed, viewModel.state.value.events.single().status)
        assertEquals(
            "버튼 시점과 겹치는 녹화 구간이 없어 저장하지 못했습니다.",
            viewModel.state.value.events.single().message
        )
    }

    private fun createViewModel(
        controller: FakeRecordingSessionController,
        clock: () -> Long
    ): LiveRecordingViewModel {
        return LiveRecordingViewModel(
            controller = controller,
            segments = createRepository(),
            assembler = RecordingEventClipAssembler(),
            clock = clock
        )
    }

    private fun createRepository(): RollingSegmentRepository {
        return RollingSegmentRepository(temporaryFolder.newFolder())
    }

    private fun addSegment(repository: RollingSegmentRepository, start: Long, end: Long) {
        val pending = repository.createPending(start)
        pending.file.writeText("video")
        repository.finalize(pending, end)
    }
}

private class FakeRecordingSessionController : RecordingSessionController {
    private val mutableEvents = MutableSharedFlow<RecordingEngineEvent>(extraBufferCapacity = 16)
    override val events: Flow<RecordingEngineEvent> = mutableEvents

    fun emit(event: RecordingEngineEvent) {
        mutableEvents.tryEmit(event)
    }

    override suspend fun bind(
        owner: LifecycleOwner,
        surfaceProvider: Preview.SurfaceProvider,
        requestedQuality: RecordingQuality
    ): RecordingQuality = requestedQuality

    override fun start(audioEnabled: Boolean) = Unit
    override fun stop() = Unit
    override fun release() = Unit
}

private open class RecordingEventClipAssembler : EventClipAssembler {
    val segmentStarts = mutableListOf<List<Long>>()
    val displayNames = mutableListOf<String>()
    val locations = mutableListOf<EventLocation?>()

    override suspend fun assemble(
        plan: com.glass.safeclip.data.recording.EventClipPlan,
        displayName: String,
        location: EventLocation?
    ): Result<android.net.Uri> {
        segmentStarts += plan.parts.map { it.segment.startElapsedMs }
        displayNames += displayName
        locations += location
        return Result.failure(IllegalStateException("test complete"))
    }
}

private class BlockingEventClipAssembler : RecordingEventClipAssembler() {
    val started = CompletableDeferred<Unit>()
    val release = CompletableDeferred<Unit>()

    override suspend fun assemble(
        plan: com.glass.safeclip.data.recording.EventClipPlan,
        displayName: String,
        location: EventLocation?
    ): Result<android.net.Uri> {
        started.complete(Unit)
        release.await()
        return super.assemble(plan, displayName, location)
    }
}

private class SerialCheckEventClipAssembler : EventClipAssembler {
    val allowNext = CompletableDeferred<Unit>()
    var active = 0
    var maxConcurrent = 0
    var exportCount = 0

    override suspend fun assemble(
        plan: com.glass.safeclip.data.recording.EventClipPlan,
        displayName: String,
        location: EventLocation?
    ): Result<android.net.Uri> {
        active += 1
        maxConcurrent = maxOf(maxConcurrent, active)
        allowNext.await()
        exportCount += 1
        active -= 1
        return Result.failure(IllegalStateException("test complete"))
    }
}
