# Video Preview Slow Capture Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Let a user tap a scanned blackbox video, watch it in-app, change playback speed, and save a frame capture from the current playback position.

**Architecture:** Keep preview UI in `ui/video`, playback-specific pure rules in `data/media`, and Android frame extraction in a small `AndroidFrameCaptureStore`. `MainActivity` keeps simple in-memory navigation between list and preview until a real navigation stack is needed.

**Tech Stack:** Kotlin, Jetpack Compose, AndroidX Media3 ExoPlayer + UI, Android `MediaMetadataRetriever`, JUnit 4.

## Global Constraints

- Use Android Storage Access Framework content URIs; do not assume direct filesystem paths.
- Do not upload or store videos without explicit user action and consent.
- MVP preview should support common MP4 playback first; unsupported codecs may show an error.
- Playback speeds for this slice: `0.25x`, `0.5x`, `1.0x`.
- Capture output goes to app-specific Pictures storage so no broad media permission is required.
- This folder is not currently a Git repository, so commit steps are replaced by file review steps.

---

## Task 1: Preview Rules

**Files:**
- Create: `app/src/main/java/com/glass/safeclip/data/media/PlaybackSpeedOption.kt`
- Create: `app/src/main/java/com/glass/safeclip/data/media/CaptureFileName.kt`
- Test: `app/src/test/java/com/glass/safeclip/data/media/PlaybackSpeedOptionTest.kt`
- Test: `app/src/test/java/com/glass/safeclip/data/media/CaptureFileNameTest.kt`

**Interfaces:**
- Produces: `data class PlaybackSpeedOption(val label: String, val speed: Float)`
- Produces: `object PlaybackSpeedOptions { val supported: List<PlaybackSpeedOption> }`
- Produces: `object CaptureFileName { fun forVideo(displayName: String, positionMs: Long): String }`

- [ ] Add failing tests for supported speed labels and capture file name sanitizing.
- [ ] Run `.\gradlew.bat :app:testDebugUnitTest`; expect unresolved references.
- [ ] Add `PlaybackSpeedOption.kt` and `CaptureFileName.kt`.
- [ ] Run `.\gradlew.bat :app:testDebugUnitTest`; expect pass.

## Task 2: Media3 And Frame Capture

**Files:**
- Modify: `gradle/libs.versions.toml`
- Modify: `app/build.gradle.kts`
- Create: `app/src/main/java/com/glass/safeclip/data/media/AndroidFrameCaptureStore.kt`

**Interfaces:**
- Produces: `class AndroidFrameCaptureStore(private val context: Context)`
- Produces: `fun AndroidFrameCaptureStore.captureFrame(videoUri: Uri, displayName: String, positionMs: Long): File`

- [ ] Add Media3 ExoPlayer and UI dependencies.
- [ ] Add `AndroidFrameCaptureStore` using `MediaMetadataRetriever` and app-specific Pictures storage.
- [ ] Run `.\gradlew.bat :app:assembleDebug`; expect pass.

## Task 3: Preview Screen And List Click

**Files:**
- Modify: `app/src/main/java/com/glass/safeclip/ui/video/VideoListScreen.kt`
- Create: `app/src/main/java/com/glass/safeclip/ui/video/VideoPreviewScreen.kt`
- Modify: `app/src/main/java/com/glass/safeclip/MainActivity.kt`
- Test: `app/src/test/java/com/glass/safeclip/ui/video/VideoPreviewTextTest.kt`

**Interfaces:**
- Changes: `VideoListScreen(state: VideoListState, onSelectFolder: () -> Unit, onVideoSelected: (VideoCandidate) -> Unit)`
- Produces: `object VideoPreviewText { fun captureSuccessMessage(path: String): String; fun captureFailureMessage(): String }`
- Produces: `@Composable fun VideoPreviewScreen(...)`

- [ ] Add failing tests for preview capture messages.
- [ ] Run `.\gradlew.bat :app:testDebugUnitTest`; expect unresolved references.
- [ ] Make video rows clickable and add preview screen using `PlayerView`.
- [ ] Wire `MainActivity` selected-video state and capture callback.
- [ ] Run `.\gradlew.bat :app:testDebugUnitTest` and `.\gradlew.bat :app:assembleDebug`; expect pass.

## Task 4: Documentation

**Files:**
- Modify: `docs/exec-plans/2026-07-13-project-start.md`

**Steps:**
- [ ] Record video preview, slow playback, and capture implementation.
- [ ] Note manual test steps: tap video, play, change speed, capture, check saved image path.

## Self-Review

**Spec coverage:** tap-to-preview is covered by Task 3, slow playback by Task 1 and Task 3, capture by Task 2 and Task 3.

**Known intentional gaps:** no trim/export, no upload submission integration, no gallery-wide image publishing, no front/rear pairing.

**Placeholder scan:** no unfinished marker remains.

**Type consistency:** `PlaybackSpeedOption`, `CaptureFileName`, `AndroidFrameCaptureStore`, and `VideoPreviewText` are consistently named.
