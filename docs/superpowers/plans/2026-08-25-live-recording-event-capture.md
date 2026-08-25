# SafeClip Live Recording Event Capture Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** SafeClip에 포그라운드 실시간 녹화, 1분 순환 조각, 리모컨 이벤트 앞뒤 1분 보존, 음성 제거 사본, `DCIM/SafeClip` 고정 저장을 추가한다.

**Architecture:** CameraX 녹화 엔진과 Media3 편집기를 UI에서 분리하고, 순수 Kotlin 정책 객체가 이벤트 범위·보존·중복 입력을 결정한다. 완성 미디어만 MediaStore의 `DCIM/SafeClip`에 게시하며 녹화 조각과 편집 임시 파일은 앱 전용 저장소에 둔다. 기존 `MainActivity`에는 화면 전환, 권한 런처, 하드웨어 키 전달만 연결한다.

**Tech Stack:** Kotlin, Jetpack Compose, CameraX 1.6.1, Media3 Transformer 1.8.0, Android MediaStore, JUnit 4, Compose UI tests

**Spec:** `docs/superpowers/specs/2026-08-25-live-recording-event-capture-design.md`

## Global Constraints

- Android `minSdk`는 24, `targetSdk`는 36을 유지한다.
- CameraX는 안정 버전 `1.6.1`의 `camera-camera2`, `camera-lifecycle`, `camera-video`, `camera-view`만 추가한다.
- 녹화 화질은 `FHD`, `HD`, `SD`만 제공하고 UHD는 요청하지 않는다.
- 사용자에게 보이는 새 미디어는 모두 `DCIM/SafeClip`에 저장한다.
- 순환 녹화 조각은 60초 단위이며 최근 3시간만 앱 전용 저장소에 보관한다.
- 이벤트 영상 이름은 `SC_EVENT_yyyyMMdd_HHmmss.mp4`, 음성 제거 사본은 `_MUTED.mp4` 접미사를 사용한다.
- 이벤트 입력은 3초 동안 중복 방지한다.
- 녹화는 실시간 녹화 화면이 보이는 동안에만 가능하고 화면 이탈 시 종료한다.
- 사용자가 누르지 않은 녹화·업로드·신고는 수행하지 않는다.
- 기존 블랙박스 SAF 폴더와 기존 저장 파일은 이동하거나 삭제하지 않는다.

---

## File Map

### 새 파일

- `app/src/main/java/com/glass/safeclip/data/recording/RecordingQuality.kt`: FHD/HD/SD 모델과 지원 화질 선택
- `app/src/main/java/com/glass/safeclip/data/recording/RollingSegment.kt`: 조각 시간과 보호 상태 모델
- `app/src/main/java/com/glass/safeclip/data/recording/EventClipPlan.kt`: 이벤트 조립 입력 모델
- `app/src/main/java/com/glass/safeclip/data/recording/EventWindowPlanner.kt`: 이벤트 범위와 조각 자르기 계산
- `app/src/main/java/com/glass/safeclip/data/recording/RollingSegmentRetention.kt`: 3시간 보존 삭제 대상 계산
- `app/src/main/java/com/glass/safeclip/data/recording/RemoteEventDebouncer.kt`: 3초 반복 입력 차단
- `app/src/main/java/com/glass/safeclip/data/recording/RollingSegmentRepository.kt`: 앱 전용 조각 파일과 메타데이터 관리
- `app/src/main/java/com/glass/safeclip/data/recording/RecordingSessionController.kt`: 녹화 엔진 인터페이스
- `app/src/main/java/com/glass/safeclip/data/recording/CameraXRecordingSessionController.kt`: CameraX 미리보기와 1분 녹화 회전
- `app/src/main/java/com/glass/safeclip/data/media/SafeClipMediaPublisher.kt`: 완성 미디어 게시 인터페이스
- `app/src/main/java/com/glass/safeclip/data/media/AndroidSafeClipMediaPublisher.kt`: API 버전별 `DCIM/SafeClip` 게시 구현
- `app/src/main/java/com/glass/safeclip/data/media/EventClipAssembler.kt`: 이벤트 조립 인터페이스
- `app/src/main/java/com/glass/safeclip/data/media/Media3EventClipAssembler.kt`: Media3 자르기·연결 구현
- `app/src/main/java/com/glass/safeclip/data/media/AudioRemovalExporter.kt`: 음성 제거 인터페이스
- `app/src/main/java/com/glass/safeclip/data/media/Media3AudioRemovalExporter.kt`: Media3 무음 사본 구현
- `app/src/main/java/com/glass/safeclip/ui/recording/LiveRecordingUiState.kt`: 화면 상태와 이벤트 작업 상태
- `app/src/main/java/com/glass/safeclip/ui/recording/LiveRecordingViewModel.kt`: 녹화 오케스트레이션
- `app/src/main/java/com/glass/safeclip/ui/recording/LiveRecordingViewModelFactory.kt`: Android 의존성 생성
- `app/src/main/java/com/glass/safeclip/ui/recording/LiveRecordingScreen.kt`: 가로 녹화 화면
- `app/src/main/java/com/glass/safeclip/ui/recording/LiveRecordingWindowEffect.kt`: 방향, 밝기, 화면 켜짐 복원
- 각 정책과 화면에 대응하는 `app/src/test/...` 및 `app/src/androidTest/...` 테스트

### 수정 파일

- `gradle/libs.versions.toml`: CameraX와 lifecycle ViewModel 의존성
- `app/build.gradle.kts`: CameraX, ViewModel Compose 의존성
- `app/src/main/AndroidManifest.xml`: 마이크와 Android 9 이하 저장 권한
- `app/src/main/java/com/glass/safeclip/data/media/SafeClipMediaSaveLocation.kt`: `DCIM/SafeClip` 단일 경로
- 기존 프레임·클립 저장소와 SafeClip 파일 조회 구현: 새 고정 앨범 사용
- `app/src/main/java/com/glass/safeclip/ui/home/MainHomeScreen.kt`: 실시간 녹화 진입 버튼
- `app/src/main/java/com/glass/safeclip/ui/navigation/SafeClipScreen.kt`: `LiveRecording` 화면
- `app/src/main/java/com/glass/safeclip/ui/navigation/SafeClipBackNavigation.kt`: 녹화 화면의 홈 복귀
- `app/src/main/java/com/glass/safeclip/ui/video/VideoPreviewScreen.kt`: 음성 제거 사본 동작
- `app/src/main/java/com/glass/safeclip/MainActivity.kt`: 권한, 화면, 리모컨 키, 저장 목록 새로고침 연결

---

### Task 1: Dependencies, Permissions, and Recording Policy Models

**Files:**
- Modify: `gradle/libs.versions.toml`
- Modify: `app/build.gradle.kts`
- Modify: `app/src/main/AndroidManifest.xml`
- Create: `app/src/main/java/com/glass/safeclip/data/recording/RecordingQuality.kt`
- Create: `app/src/main/java/com/glass/safeclip/data/recording/RollingSegment.kt`
- Create: `app/src/main/java/com/glass/safeclip/data/recording/EventClipPlan.kt`
- Create: `app/src/main/java/com/glass/safeclip/data/recording/EventWindowPlanner.kt`
- Create: `app/src/main/java/com/glass/safeclip/data/recording/RollingSegmentRetention.kt`
- Create: `app/src/main/java/com/glass/safeclip/data/recording/RemoteEventDebouncer.kt`
- Test: `app/src/test/java/com/glass/safeclip/data/recording/RecordingQualityTest.kt`
- Test: `app/src/test/java/com/glass/safeclip/data/recording/EventWindowPlannerTest.kt`
- Test: `app/src/test/java/com/glass/safeclip/data/recording/RollingSegmentRetentionTest.kt`
- Test: `app/src/test/java/com/glass/safeclip/data/recording/RemoteEventDebouncerTest.kt`

**Interfaces:**
- Produces: `RecordingQuality.select(requested, supported): RecordingQuality`
- Produces: `EventWindowPlanner.plan(triggerElapsedMs, availableEndElapsedMs, segments): EventClipPlan`
- Produces: `RollingSegmentRetention.expired(nowElapsedMs, segments): List<RollingSegment>`
- Produces: `RemoteEventDebouncer.accept(elapsedMs): Boolean`

- [ ] **Step 1: Write failing policy tests**

```kotlin
@Test fun eventWindowUsesSixtySecondsBeforeAndAfterTrigger() {
    val segments = listOf(
        RollingSegment("a", "file:///a.mp4", 0, 60_000, false),
        RollingSegment("b", "file:///b.mp4", 60_000, 120_000, false),
        RollingSegment("c", "file:///c.mp4", 120_000, 180_000, false)
    )
    val plan = EventWindowPlanner.plan(90_000, 180_000, segments)
    assertEquals(30_000, plan.windowStartElapsedMs)
    assertEquals(150_000, plan.windowEndElapsedMs)
    assertEquals(listOf("a", "b", "c"), plan.parts.map { it.segment.id })
    assertEquals(30_000, plan.parts.first().clipStartMs)
    assertEquals(30_000, plan.parts.last().clipEndMs)
}

@Test fun protectedSegmentsAreNotExpired() {
    val old = RollingSegment("old", "file:///old.mp4", 0, 60_000, false)
    val protected = RollingSegment("protected", "file:///p.mp4", 0, 60_000, true)
    assertEquals(listOf(old), RollingSegmentRetention.expired(10_860_001, listOf(old, protected)))
}

@Test fun remoteInputIsDebouncedForThreeSeconds() {
    val debouncer = RemoteEventDebouncer(3_000)
    assertTrue(debouncer.accept(10_000))
    assertFalse(debouncer.accept(12_999))
    assertTrue(debouncer.accept(13_000))
}
```

- [ ] **Step 2: Run tests and verify missing types fail**

Run: `./gradlew.bat testDebugUnitTest --tests "com.glass.safeclip.data.recording.*"`

Expected: compilation fails because the recording policy types do not exist.

- [ ] **Step 3: Add stable dependencies and manifest permissions**

```toml
cameraX = "1.6.1"
androidx-camera-camera2 = { group = "androidx.camera", name = "camera-camera2", version.ref = "cameraX" }
androidx-camera-lifecycle = { group = "androidx.camera", name = "camera-lifecycle", version.ref = "cameraX" }
androidx-camera-video = { group = "androidx.camera", name = "camera-video", version.ref = "cameraX" }
androidx-camera-view = { group = "androidx.camera", name = "camera-view", version.ref = "cameraX" }
androidx-lifecycle-viewmodel-compose = { group = "androidx.lifecycle", name = "lifecycle-viewmodel-compose", version.ref = "lifecycleRuntimeKtx" }
```

```xml
<uses-permission android:name="android.permission.RECORD_AUDIO" />
<uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE" android:maxSdkVersion="28" />
```

- [ ] **Step 4: Implement the policy models**

```kotlin
enum class RecordingQuality { FHD, HD, SD;
    companion object {
        fun select(requested: RecordingQuality, supported: Set<RecordingQuality>): RecordingQuality =
            entries.drop(entries.indexOf(requested)).firstOrNull(supported::contains)
                ?: entries.lastOrNull(supported::contains)
                ?: error("No supported recording quality")
    }
}

data class RollingSegment(
    val id: String,
    val uriString: String,
    val startElapsedMs: Long,
    val endElapsedMs: Long,
    val isProtected: Boolean
)

data class EventClipPart(val segment: RollingSegment, val clipStartMs: Long, val clipEndMs: Long)
data class EventClipPlan(
    val triggerElapsedMs: Long,
    val windowStartElapsedMs: Long,
    val windowEndElapsedMs: Long,
    val isShort: Boolean,
    val parts: List<EventClipPart>
)
```

Implement `EventWindowPlanner` with `maxOf(segments.first().startElapsedMs, trigger - 60_000)` and `minOf(availableEnd, trigger + 60_000)`. Calculate each part's offsets relative to its segment start. Implement retention with `3 * 60 * 60 * 1_000L` and exclude protected segments.

- [ ] **Step 5: Run policy tests**

Run: `./gradlew.bat testDebugUnitTest --tests "com.glass.safeclip.data.recording.*"`

Expected: all recording policy tests pass.

- [ ] **Step 6: Commit**

```powershell
git add gradle/libs.versions.toml app/build.gradle.kts app/src/main/AndroidManifest.xml app/src/main/java/com/glass/safeclip/data/recording app/src/test/java/com/glass/safeclip/data/recording
git commit -m "feat: add live recording policy foundation"
```

---

### Task 2: Fixed DCIM/SafeClip Media Publishing

**Files:**
- Modify: `app/src/main/java/com/glass/safeclip/data/media/SafeClipMediaSaveLocation.kt`
- Create: `app/src/main/java/com/glass/safeclip/data/media/SafeClipMediaPublisher.kt`
- Create: `app/src/main/java/com/glass/safeclip/data/media/AndroidSafeClipMediaPublisher.kt`
- Modify: `app/src/main/java/com/glass/safeclip/data/media/AndroidFrameCaptureStore.kt`
- Modify: `app/src/main/java/com/glass/safeclip/data/media/AndroidVideoClipExporter.kt`
- Modify: `app/src/main/java/com/glass/safeclip/data/media/AndroidSafeClipSavedMediaRepository.kt`
- Modify: `app/src/main/java/com/glass/safeclip/data/media/AndroidSafeClipSavedMediaCounter.kt`
- Modify: `app/src/main/java/com/glass/safeclip/ui/home/HomeImportActions.kt`
- Modify: `app/src/main/java/com/glass/safeclip/ui/home/HomeStatusSummary.kt`
- Test: `app/src/test/java/com/glass/safeclip/data/media/SafeClipMediaSaveLocationTest.kt`
- Test: `app/src/test/java/com/glass/safeclip/ui/home/HomeImportActionsTest.kt`

**Interfaces:**
- Produces: `SafeClipMediaPublisher.publishVideo(source: File, displayName: String): Uri`
- Produces: `SafeClipMediaPublisher.publishImage(source: File, displayName: String, mimeType: String): Uri`
- Produces: `SafeClipMediaSaveLocation.relativePath == "DCIM/SafeClip"`

- [ ] **Step 1: Replace path expectations with the fixed album**

```kotlin
@Test fun allGeneratedMediaUsesDcimSafeClip() {
    assertEquals("SafeClip", SafeClipMediaSaveLocation.albumName)
    assertEquals("DCIM/SafeClip", SafeClipMediaSaveLocation.relativePath)
    assertEquals("DCIM/SafeClip/a.mp4", SafeClipMediaSaveLocation.displayPath("a.mp4"))
}
```

Update `HomeImportActionsTest` so opening the SafeClip folder does not depend on camera permission or a separately selected event folder.

- [ ] **Step 2: Run focused tests and verify old path assertions fail**

Run: `./gradlew.bat testDebugUnitTest --tests "com.glass.safeclip.data.media.SafeClipMediaSaveLocationTest" --tests "com.glass.safeclip.ui.home.HomeImportActionsTest"`

Expected: tests fail with `SafeClip Captures`, `Pictures`, or `Movies` path mismatches.

- [ ] **Step 3: Implement the publisher contract and API-specific writes**

```kotlin
interface SafeClipMediaPublisher {
    suspend fun publishVideo(source: File, displayName: String): Uri
    suspend fun publishImage(source: File, displayName: String, mimeType: String): Uri
}
```

For API 29+, insert into `MediaStore.Video.Media.EXTERNAL_CONTENT_URI` or `MediaStore.Images.Media.EXTERNAL_CONTENT_URI` using:

```kotlin
ContentValues().apply {
    put(MediaStore.MediaColumns.DISPLAY_NAME, displayName)
    put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
    put(MediaStore.MediaColumns.RELATIVE_PATH, "DCIM/SafeClip")
    put(MediaStore.MediaColumns.IS_PENDING, 1)
}
```

Copy bytes with buffered streams, clear `IS_PENDING` only after success, and delete the inserted row on failure. For API 24–28, create `Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM)/SafeClip`, copy the file, and call `MediaScannerConnection.scanFile`.

- [ ] **Step 4: Route existing generated media through the fixed publisher**

Remove the selected event folder as a write destination from `AndroidFrameCaptureStore` and `AndroidVideoClipExporter`. Query new MediaStore entries under `DCIM/SafeClip`; if a previously persisted legacy event-folder URI is still readable, append those legacy items without prompting the user to select it again. Deduplicate by URI.

- [ ] **Step 5: Run media and home tests**

Run: `./gradlew.bat testDebugUnitTest --tests "com.glass.safeclip.data.media.*" --tests "com.glass.safeclip.ui.home.*"`

Expected: all focused tests pass.

- [ ] **Step 6: Commit**

```powershell
git add app/src/main/java/com/glass/safeclip/data/media app/src/main/java/com/glass/safeclip/ui/home app/src/test/java/com/glass/safeclip/data/media app/src/test/java/com/glass/safeclip/ui/home
git commit -m "feat: publish generated media to dcim safeclip"
```

---

### Task 3: Rolling Segment Repository

**Files:**
- Create: `app/src/main/java/com/glass/safeclip/data/recording/RollingSegmentRepository.kt`
- Create: `app/src/test/java/com/glass/safeclip/data/recording/RollingSegmentRepositoryTest.kt`

**Interfaces:**
- Consumes: `RollingSegment`, `RollingSegmentRetention`
- Produces: `createPending(startElapsedMs): PendingSegment`
- Produces: `finalize(pending, endElapsedMs): RollingSegment`
- Produces: `protect(ids: Set<String>)`, `release(ids: Set<String>)`, `cleanup(nowElapsedMs)`
- Produces: `segments(): List<RollingSegment>`
- Produces: `availableBytes(): Long`

- [ ] **Step 1: Write repository tests with a temporary directory**

```kotlin
@Test fun finalizePersistsMetadataAndCleanupKeepsProtectedFiles() {
    val repo = RollingSegmentRepository(tempFolder.root)
    val pending = repo.createPending(0)
    pending.file.writeBytes(byteArrayOf(1, 2, 3))
    val segment = repo.finalize(pending, 60_000)
    repo.protect(setOf(segment.id))
    repo.cleanup(10_860_001)
    assertTrue(File(URI(segment.uriString)).exists())
    assertTrue(repo.segments().single().isProtected)
}
```

- [ ] **Step 2: Run the test and verify it fails**

Run: `./gradlew.bat testDebugUnitTest --tests "com.glass.safeclip.data.recording.RollingSegmentRepositoryTest"`

Expected: compilation fails because `RollingSegmentRepository` and `PendingSegment` do not exist.

- [ ] **Step 3: Implement atomic metadata persistence**

Use `<externalFilesDir>/recording/segments` when constructed by Android code and accept a `File` in the repository constructor for unit tests. Persist one `segments.json` file through a temporary file followed by rename. Ignore `.pending.mp4` files on normal listing and delete abandoned pending files during startup cleanup.

```kotlin
data class PendingSegment(val id: String, val file: File, val startElapsedMs: Long)

class RollingSegmentRepository(private val directory: File) {
    fun createPending(startElapsedMs: Long): PendingSegment
    fun finalize(pending: PendingSegment, endElapsedMs: Long): RollingSegment
    fun segments(): List<RollingSegment>
    fun protect(ids: Set<String>)
    fun release(ids: Set<String>)
    fun cleanup(nowElapsedMs: Long)
    fun availableBytes(): Long
}
```

- [ ] **Step 4: Run repository and policy tests**

Run: `./gradlew.bat testDebugUnitTest --tests "com.glass.safeclip.data.recording.*"`

Expected: all tests pass and abandoned pending files are removed by the crash recovery test.

- [ ] **Step 5: Commit**

```powershell
git add app/src/main/java/com/glass/safeclip/data/recording app/src/test/java/com/glass/safeclip/data/recording
git commit -m "feat: manage rolling recording segments"
```

---

### Task 4: CameraX Preview and One-Minute Segment Rotation

**Files:**
- Create: `app/src/main/java/com/glass/safeclip/data/recording/RecordingSessionController.kt`
- Create: `app/src/main/java/com/glass/safeclip/data/recording/CameraXRecordingSessionController.kt`
- Create: `app/src/androidTest/java/com/glass/safeclip/data/recording/CameraXRecordingSessionControllerTest.kt`

**Interfaces:**
- Consumes: `RollingSegmentRepository`, `RecordingQuality`
- Produces: `RecordingSessionController.events: Flow<RecordingEngineEvent>`
- Produces: `bind(owner, surfaceProvider, requestedQuality): RecordingQuality`
- Produces: `start(audioEnabled: Boolean)`, `stop()`, `release()`

- [ ] **Step 1: Define a fake-driven controller contract test**

```kotlin
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
```

The instrumentation test must grant camera permission, bind a `PreviewView`, start a segment, wait for `Started`, stop, and assert `Stopped` without crashing. Mark the test with `@SdkSuppress(minSdkVersion = 29)` and use `GrantPermissionRule`.

- [ ] **Step 2: Run the instrumentation compile and verify missing implementation fails**

Run: `./gradlew.bat compileDebugAndroidTestKotlin`

Expected: compilation fails because `CameraXRecordingSessionController` does not exist.

- [ ] **Step 3: Implement CameraX binding and quality fallback**

Map only these values:

```kotlin
private fun RecordingQuality.toCameraX(): Quality = when (this) {
    RecordingQuality.FHD -> Quality.FHD
    RecordingQuality.HD -> Quality.HD
    RecordingQuality.SD -> Quality.SD
}
```

Use `QualitySelector.getSupportedQualities(cameraInfo)`, choose through `RecordingQuality.select`, build `Recorder`, bind `Preview` plus `VideoCapture` to the back camera, and set `PreviewView.ImplementationMode.COMPATIBLE`. Build `VideoCapture` with `setTargetFrameRate(Range(30, 30))` when the camera reports that exact range; otherwise choose a supported range that contains 30 and has the lowest upper bound. CameraX treats this as a target rather than a guarantee, so record the actual frame rate during Task 9 validation. Do not include `Quality.UHD` in the ordered list.

- [ ] **Step 4: Implement 60-second rotation**

Create each file through `RollingSegmentRepository.createPending(SystemClock.elapsedRealtime())`, record with `FileOutputOptions`, and enable audio only when requested and permitted. After 60 seconds call `Recording.stop()`. On `VideoRecordEvent.Finalize`, finalize metadata, emit `SegmentFinalized`, immediately begin the next segment if the session is still active, then run retention cleanup. Emit a user-facing error and stop if finalization has a non-recoverable error.

- [ ] **Step 5: Compile and run unit tests**

Run: `./gradlew.bat testDebugUnitTest assembleDebug compileDebugAndroidTestKotlin`

Expected: commands succeed. Run the physical-device controller test from Android Studio because it needs a camera.

- [ ] **Step 6: Commit**

```powershell
git add app/src/main/java/com/glass/safeclip/data/recording app/src/androidTest/java/com/glass/safeclip/data/recording
git commit -m "feat: record rotating camerax segments"
```

---

### Task 5: Event Assembly and Muted Copies

**Files:**
- Create: `app/src/main/java/com/glass/safeclip/data/media/EventClipAssembler.kt`
- Create: `app/src/main/java/com/glass/safeclip/data/media/Media3EventClipAssembler.kt`
- Create: `app/src/main/java/com/glass/safeclip/data/media/AudioRemovalExporter.kt`
- Create: `app/src/main/java/com/glass/safeclip/data/media/Media3AudioRemovalExporter.kt`
- Create: `app/src/main/java/com/glass/safeclip/data/media/EventClipFileName.kt`
- Test: `app/src/test/java/com/glass/safeclip/data/media/EventClipFileNameTest.kt`
- Test: `app/src/androidTest/java/com/glass/safeclip/data/media/Media3EventClipAssemblerTest.kt`

**Interfaces:**
- Consumes: `EventClipPlan`, `SafeClipMediaPublisher`
- Produces: `EventClipAssembler.assemble(plan, displayName): Result<Uri>`
- Produces: `AudioRemovalExporter.export(sourceUri, displayName): Flow<MediaExportProgress>`
- Produces: `EventClipFileName.event(instant)`, `EventClipFileName.muted(originalName)`

- [ ] **Step 1: Write file-name and assembler input tests**

```kotlin
@Test fun eventAndMutedNamesFollowStoreRules() {
    val name = EventClipFileName.event(Instant.parse("2026-08-25T03:04:05Z"), ZoneOffset.UTC)
    assertEquals("SC_EVENT_20260825_030405.mp4", name)
    assertEquals("SC_EVENT_20260825_030405_MUTED.mp4", EventClipFileName.muted(name))
}
```

The instrumentation fixture must use three short MP4 assets under `app/src/androidTest/assets/recording/`, assemble clipped parts, inspect the output duration with `MediaMetadataRetriever`, and allow 500ms tolerance.

- [ ] **Step 2: Run tests and verify missing exporters fail**

Run: `./gradlew.bat testDebugUnitTest --tests "com.glass.safeclip.data.media.EventClipFileNameTest" compileDebugAndroidTestKotlin`

Expected: compilation fails because the new file-name and exporter types do not exist.

- [ ] **Step 3: Implement Media3 event composition**

```kotlin
interface EventClipAssembler {
    suspend fun assemble(plan: EventClipPlan, displayName: String): Result<Uri>
}
```

For each part, create a `MediaItem` with `ClippingConfiguration(startPositionMs, endPositionMs)`, wrap it in `EditedMediaItem`, then call `EditedMediaItemSequence.withAudioAndVideoFrom(items)` and `Composition.Builder(sequence).build()`. Export to an app-private `.mp4`; publish only after Transformer completion; delete the temporary output on success and failure. Protect all input segment IDs before export and release them in `finally`.

- [ ] **Step 4: Implement audio removal without replacing the original**

```kotlin
val edited = EditedMediaItem.Builder(MediaItem.fromUri(sourceUri))
    .setRemoveAudio(true)
    .build()
```

Define the contract and progress states exactly as follows:

```kotlin
sealed interface MediaExportProgress {
    data class Running(val percent: Int) : MediaExportProgress
    data class Completed(val uri: Uri) : MediaExportProgress
    data class Failed(val message: String) : MediaExportProgress
}

interface AudioRemovalExporter {
    fun export(sourceUri: Uri, displayName: String): Flow<MediaExportProgress>
}
```

Export to a private temporary file, publish with the `_MUTED.mp4` name, leave the source URI untouched, and emit Transformer progress through this flow.

- [ ] **Step 5: Run all media tests and assemble**

Run: `./gradlew.bat testDebugUnitTest --tests "com.glass.safeclip.data.media.*" assembleDebug compileDebugAndroidTestKotlin`

Expected: unit tests and compilation pass. Run the asset-based instrumentation test on the emulator.

- [ ] **Step 6: Commit**

```powershell
git add app/src/main/java/com/glass/safeclip/data/media app/src/test/java/com/glass/safeclip/data/media app/src/androidTest
git commit -m "feat: assemble event clips and muted copies"
```

---

### Task 6: Live Recording ViewModel and State Machine

**Files:**
- Create: `app/src/main/java/com/glass/safeclip/ui/recording/LiveRecordingUiState.kt`
- Create: `app/src/main/java/com/glass/safeclip/ui/recording/LiveRecordingViewModel.kt`
- Create: `app/src/main/java/com/glass/safeclip/ui/recording/LiveRecordingViewModelFactory.kt`
- Test: `app/src/test/java/com/glass/safeclip/ui/recording/LiveRecordingViewModelTest.kt`

**Interfaces:**
- Consumes: `RecordingSessionController`, `RollingSegmentRepository`, `EventWindowPlanner`, `EventClipAssembler`, `RemoteEventDebouncer`
- Produces: `LiveRecordingViewModel.state: StateFlow<LiveRecordingUiState>`
- Produces: `startRecording()`, `stopRecording()`, `requestEvent(elapsedMs)`, `setRemoteTestEnabled(enabled)`, `onRemoteKey(keyCode, elapsedMs): Boolean`, `leaveScreen()`

- [ ] **Step 1: Write coroutine state tests with fakes**

```kotlin
@Test fun eventWaitsForPostMinuteThenExports() = runTest {
    val controller = FakeRecordingSessionController()
    val assembler = FakeEventClipAssembler()
    val viewModel = createViewModel(controller, assembler)

    viewModel.requestEvent(90_000)
    assertEquals(EventWorkStatus.CapturingAfter, viewModel.state.value.events.single().status)
    controller.emitFinalized(segmentEndingAt = 150_000)
    advanceUntilIdle()
    assertEquals(1, assembler.plans.size)
}

@Test fun leavingScreenStopsRecordingAndRejectsRemoteKeys() = runTest {
    val controller = FakeRecordingSessionController()
    val viewModel = createViewModel(controller = controller)
    viewModel.startRecording()
    viewModel.leaveScreen()
    assertEquals(1, controller.stopCalls)
    assertFalse(viewModel.onRemoteKey(KeyEvent.KEYCODE_VOLUME_UP, 20_000))
}

@Test fun remoteTestReportsSupportedKeyWithoutCreatingEvent() = runTest {
    val viewModel = createViewModel()
    viewModel.setRemoteTestEnabled(true)
    assertTrue(viewModel.onRemoteKey(KeyEvent.KEYCODE_VOLUME_UP, 20_000))
    assertEquals("볼륨 올리기 키 인식", viewModel.state.value.lastRemoteKeyLabel)
    assertTrue(viewModel.state.value.events.isEmpty())
}
```

- [ ] **Step 2: Run tests and verify the state machine is missing**

Run: `./gradlew.bat testDebugUnitTest --tests "com.glass.safeclip.ui.recording.LiveRecordingViewModelTest"`

Expected: compilation fails because recording UI types do not exist.

- [ ] **Step 3: Implement explicit UI and event states**

```kotlin
enum class RecordingPhase { Ready, Preparing, Recording, Stopping, Error }
enum class EventWorkStatus { CapturingAfter, Exporting, Completed, Failed }

data class EventWorkUi(
    val id: String,
    val triggerElapsedMs: Long,
    val status: EventWorkStatus,
    val message: String,
    val outputUri: String? = null
)

data class LiveRecordingUiState(
    val phase: RecordingPhase = RecordingPhase.Ready,
    val supportedQualities: List<RecordingQuality> = emptyList(),
    val selectedQuality: RecordingQuality = RecordingQuality.FHD,
    val audioEnabled: Boolean = true,
    val elapsedMs: Long = 0,
    val freeBytes: Long = 0,
    val events: List<EventWorkUi> = emptyList(),
    val remoteTestEnabled: Boolean = false,
    val lastRemoteKeyLabel: String? = null,
    val message: String? = null
)
```

Queue event jobs by trigger time. Start assembly when a finalized segment reaches `trigger + 60_000`; when stopping early, assemble against the latest finalized end and set the short-clip message. Handle each event independently so one failure does not stop recording. While recording, run a one-second coroutine ticker that updates `elapsedMs` and `freeBytes` from `RollingSegmentRepository.availableBytes()`; cancel it on stop and clear.

- [ ] **Step 4: Implement remote key handling and storage failure stop**

Accept only volume up, volume down, and camera keys. While `phase == Recording`, pass them through the debouncer and request an event. While `phase == Ready && remoteTestEnabled`, update `lastRemoteKeyLabel` without creating an event. Return `true` only in those two cases. On controller storage or camera error, call `stop`, update `phase = Error`, and keep completed event outputs visible.

- [ ] **Step 5: Run state tests**

Run: `./gradlew.bat testDebugUnitTest --tests "com.glass.safeclip.ui.recording.*"`

Expected: all ViewModel tests pass.

- [ ] **Step 6: Commit**

```powershell
git add app/src/main/java/com/glass/safeclip/ui/recording app/src/test/java/com/glass/safeclip/ui/recording
git commit -m "feat: coordinate live recording state"
```

---

### Task 7: Landscape Recording Screen and Window Behavior

**Files:**
- Create: `app/src/main/java/com/glass/safeclip/ui/recording/LiveRecordingScreen.kt`
- Create: `app/src/main/java/com/glass/safeclip/ui/recording/LiveRecordingWindowEffect.kt`
- Create: `app/src/androidTest/java/com/glass/safeclip/ui/recording/LiveRecordingScreenTest.kt`
- Modify: `app/src/main/java/com/glass/safeclip/ui/theme/Color.kt` only if existing semantic colors do not provide a visible recording/error status in both themes

**Interfaces:**
- Consumes: `LiveRecordingUiState`
- Produces: `LiveRecordingScreen(state, onStart, onStop, onEvent, onQualityChange, onAudioChange, onBack, onPreviewReady)`
- Produces: `LiveRecordingWindowEffect(isRecording: Boolean)`

- [ ] **Step 1: Write Compose UI tests for ready and recording states**

```kotlin
@Test fun recordingStateShowsStatusAndEventButton() {
    composeRule.setContent {
        SafeClipTheme {
            LiveRecordingScreen(
                state = LiveRecordingUiState(phase = RecordingPhase.Recording, elapsedMs = 65_000),
                onStart = {}, onStop = {}, onEvent = {}, onQualityChange = {},
                onAudioChange = {}, onBack = {}, onPreviewReady = {}
            )
        }
    }
    composeRule.onNodeWithText("녹화 중").assertIsDisplayed()
    composeRule.onNodeWithText("이벤트 저장").assertIsEnabled()
    composeRule.onNodeWithText("00:01:05").assertIsDisplayed()
}
```

- [ ] **Step 2: Compile tests and verify the screen is missing**

Run: `./gradlew.bat compileDebugAndroidTestKotlin`

Expected: compilation fails because `LiveRecordingScreen` does not exist.

- [ ] **Step 3: Build the landscape recording UI**

Use a full-width `PreviewView` as the main scene, a compact top status row, and one bottom control band. Use icon buttons for back and settings-like controls with content descriptions. Use a red circular record/stop control, a clearly labeled `이벤트 저장` command, quality segmented control, audio toggle, event progress list, and storage/error text. Before recording, provide a `리모컨 테스트` toggle and show `lastRemoteKeyLabel` when a supported key arrives. Do not nest cards or put instructional text over the preview.

- [ ] **Step 4: Implement reversible window effects**

```kotlin
@Composable
fun LiveRecordingWindowEffect(isRecording: Boolean) {
    val activity = LocalContext.current.findActivity()
    DisposableEffect(activity, isRecording) {
        val oldOrientation = activity.requestedOrientation
        val oldBrightness = activity.window.attributes.screenBrightness
        activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        if (isRecording) {
            activity.window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            activity.window.attributes = activity.window.attributes.apply { screenBrightness = 0.08f }
        }
        onDispose {
            activity.requestedOrientation = oldOrientation
            activity.window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            activity.window.attributes = activity.window.attributes.apply { screenBrightness = oldBrightness }
        }
    }
}

private tailrec fun Context.findActivity(): Activity = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> error("LiveRecordingScreen requires an Activity context")
}
```

When `isRecording` changes from true to false without leaving the screen, restore brightness and clear the keep-screen-on flag inside the effect restart.

- [ ] **Step 5: Run UI tests in light and dark themes**

Run: `./gradlew.bat connectedDebugAndroidTest`

Expected: status text, controls, and event progress are visible in both theme tests. If no emulator is attached, run `compileDebugAndroidTestKotlin` and record the device test as pending in the handoff checklist without claiming it passed.

- [ ] **Step 6: Commit**

```powershell
git add app/src/main/java/com/glass/safeclip/ui/recording app/src/androidTest/java/com/glass/safeclip/ui/recording app/src/main/java/com/glass/safeclip/ui/theme
git commit -m "feat: add landscape live recording screen"
```

---

### Task 8: Navigation, Permissions, Remote Keys, and Existing Flow Integration

**Files:**
- Modify: `app/src/main/java/com/glass/safeclip/ui/navigation/SafeClipScreen.kt`
- Modify: `app/src/main/java/com/glass/safeclip/ui/navigation/SafeClipBackNavigation.kt`
- Modify: `app/src/main/java/com/glass/safeclip/ui/home/MainHomeScreen.kt`
- Modify: `app/src/main/java/com/glass/safeclip/ui/video/VideoPreviewScreen.kt`
- Modify: `app/src/main/java/com/glass/safeclip/MainActivity.kt`
- Modify: `app/src/test/java/com/glass/safeclip/ui/navigation/SafeClipScreenTest.kt`
- Modify: `app/src/test/java/com/glass/safeclip/ui/navigation/SafeClipBackNavigationTest.kt`
- Create: `app/src/test/java/com/glass/safeclip/ui/recording/RemoteKeyRoutingTest.kt`

**Interfaces:**
- Consumes: all recording and media interfaces from Tasks 1–7
- Produces: Home → LiveRecording → Home navigation
- Produces: hardware key → active `LiveRecordingViewModel.onRemoteKey`
- Produces: completed event URI → existing preview and submission flow

- [ ] **Step 1: Write navigation and remote routing tests**

```kotlin
@Test fun liveRecordingBackTargetIsHome() {
    assertEquals(SafeClipScreen.Home, SafeClipBackNavigation.previousScreen(SafeClipScreen.LiveRecording))
}

@Test fun volumeKeyIsConsumedOnlyWhileRecordingScreenIsActive() {
    assertTrue(RemoteKeyRouting.shouldOffer(SafeClipScreen.LiveRecording, KeyEvent.KEYCODE_VOLUME_UP))
    assertFalse(RemoteKeyRouting.shouldOffer(SafeClipScreen.Home, KeyEvent.KEYCODE_VOLUME_UP))
}
```

- [ ] **Step 2: Run focused tests and verify new route is missing**

Run: `./gradlew.bat testDebugUnitTest --tests "com.glass.safeclip.ui.navigation.*" --tests "com.glass.safeclip.ui.recording.RemoteKeyRoutingTest"`

Expected: compilation fails because `SafeClipScreen.LiveRecording` and `RemoteKeyRouting` do not exist.

- [ ] **Step 3: Add the home entry and navigation branch**

Add `data object LiveRecording : SafeClipScreen`, return Home from back navigation, add `onOpenLiveRecording` to `MainHomeScreen`, and render the recording screen in `MainActivity`. Keep the existing import, folder, submission, status, and settings routes unchanged.

- [ ] **Step 4: Add permission launchers with video-only fallback**

Request camera before `startRecording`. If audio is enabled and `RECORD_AUDIO` is absent, request it; on denial set `audioEnabled = false`, show `마이크 권한이 없어 영상만 녹화합니다`, and continue. On API 28 or lower request `WRITE_EXTERNAL_STORAGE` before starting or publishing.

- [ ] **Step 5: Route supported hardware keys**

Add an Activity-level callback and override:

```kotlin
override fun dispatchKeyEvent(event: KeyEvent): Boolean {
    if (event.action == KeyEvent.ACTION_DOWN &&
        liveRecordingKeyHandler?.invoke(event.keyCode, event.eventTime) == true
    ) return true
    return super.dispatchKeyEvent(event)
}
```

Register the callback only while `SafeClipScreen.LiveRecording` is composed and clear it in `DisposableEffect.onDispose`. This lets ready-state remote testing and active recording share the same callback. Ignore repeated down events through `RemoteEventDebouncer` during recording.

- [ ] **Step 6: Connect event outputs and muted copies to existing media UI**

After publishing an event or muted copy, refresh `AndroidSafeClipSavedMediaRepository`; let the SafeClip folder open the output, preview it with the existing `VideoPreviewScreen`, and submit it through the existing `SubmissionForm`. Add `음성 제거 사본 만들기` to preview only for files under the SafeClip album and show persistent processing/completion/error state.

- [ ] **Step 7: Handle back and lifecycle stops**

When back is pressed during recording, show a confirmation dialog. Confirming calls `viewModel.leaveScreen()` and navigates only after the controller emits `Stopped`. In `DisposableEffect.onDispose`, call `leaveScreen()` as a final guard. On `Lifecycle.Event.ON_STOP`, stop the recording because this version does not record in the background.

- [ ] **Step 8: Run navigation, UI, and build tests**

Run: `./gradlew.bat testDebugUnitTest assembleDebug compileDebugAndroidTestKotlin`

Expected: all unit tests pass and debug APK plus instrumentation sources compile.

- [ ] **Step 9: Commit**

```powershell
git add app/src/main/java/com/glass/safeclip/MainActivity.kt app/src/main/java/com/glass/safeclip/ui app/src/test/java/com/glass/safeclip/ui
git commit -m "feat: integrate live recording into safeclip"
```

---

### Task 9: Device Validation, Release Notes, and Play Disclosure Checklist

**Files:**
- Create: `docs/exec-plans/2026-08-25-live-recording-device-validation.md`

**Interfaces:**
- Consumes: complete live-recording feature
- Produces: measured release-readiness record with device, Android version, quality, duration, segment gap, audio, remote, storage, and thermal result

- [ ] **Step 1: Run the complete automated suite**

Run: `./gradlew.bat clean testDebugUnitTest assembleDebug lintDebug`

Expected: all commands exit successfully. Record exact failing task and stack trace in the validation document if any command fails; do not mark the feature complete until fixed.

- [ ] **Step 2: Run a 30-minute FHD physical-device recording**

On the Samsung development phone, record for at least 30 minutes with audio. Record device model, Android version, selected and actual resolution, start/end time, final app state, temperature warning presence, and whether every one-minute segment is playable.

- [ ] **Step 3: Measure segment-boundary gaps**

Trigger events at 59 seconds, 60 seconds, and 61 seconds in three runs. Inspect frame timestamps around each join with Media3 or `ffprobe`. The acceptance threshold is no more than 1.0 second of missing content at any join. If exceeded, stop release preparation and record that CameraX restart rotation must be replaced before publication.

- [ ] **Step 4: Test remote, permissions, storage, and lifecycle**

Verify all of these and record pass/fail separately:

```text
camera allowed / denied
microphone allowed / denied / audio toggle off
volume-up remote / volume-down remote / long press debounce
screen back confirmation / Home key / app switch
low storage stop message
event clip duration and playback
muted copy has video and no audio track
DCIM/SafeClip visibility in Files, Gallery, SafeClip preview, and submission
light theme and dark theme on a second phone
```

- [ ] **Step 5: Update Play Console disclosure notes**

Document that the app uses camera for user-started recording, microphone only when the audio toggle is on, stores generated media on device, does not record in background, and uploads only after explicit submission. No Bluetooth permission is declared because pairing is handled by Android and the app receives ordinary hardware key events.

- [ ] **Step 6: Commit validation documentation**

```powershell
git add docs/exec-plans/2026-08-25-live-recording-device-validation.md
git commit -m "docs: record live recording device validation"
```

---

## Completion Gate

The feature is complete only when:

1. `testDebugUnitTest`, `assembleDebug`, and `lintDebug` pass.
2. The Samsung phone completes a 30-minute FHD recording without corrupt segments.
3. Segment-boundary loss is at most 1.0 second in all three boundary tests.
4. A second phone shows readable text in both light and dark themes.
5. A supported Bluetooth remote creates exactly one event per short press.
6. Event and muted files appear in `DCIM/SafeClip`, preview correctly, and enter the existing submission flow.
7. Leaving the recording screen stops capture and restores orientation, brightness, and screen timeout behavior.
