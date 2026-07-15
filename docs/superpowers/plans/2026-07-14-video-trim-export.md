# Video Trim Export Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Let the user mark a start/end point in preview, save a trimmed submission clip, and keep original video metadata for the later submission flow.

**Architecture:** Keep original dashcam files read-only. Add a small pure Kotlin selection model for clip ranges, a Media3 Transformer Android exporter that writes a new app-owned MP4, and compact Compose controls beside the existing capture button.

**Tech Stack:** Kotlin, Jetpack Compose, Android SAF URI input, Media3 ExoPlayer/Transformer, JUnit.

## Global Constraints

- Do not modify or overwrite the original video.
- Preserve original URI, display name, size, modified time, folder path, and selected start/end times in the export result.
- Use Android Storage Access Framework URIs; do not assume direct external-storage paths.
- Keep UI controls compact because preview size matters.
- Add tests before production logic where the logic can run on the JVM.

---

### Task 1: Clip Range Rules

**Files:**
- Create: `app/src/test/java/com/glass/safeclip/data/media/VideoClipSelectionTest.kt`
- Create: `app/src/main/java/com/glass/safeclip/data/media/VideoClipSelection.kt`

**Interfaces:**
- Produces: `data class VideoClipSelection(val startMs: Long? = null, val endMs: Long? = null)`
- Produces: `VideoClipSelection.isComplete: Boolean`
- Produces: `VideoClipSelection.durationMs: Long?`
- Produces: `VideoClipSelection.withStart(positionMs: Long)`
- Produces: `VideoClipSelection.withEnd(positionMs: Long)`
- Produces: `VideoClipText.rangeLabel(selection)`

- [x] Write failing tests for complete/incomplete ranges, clamping negative positions, duration, and labels.
- [x] Run the new test and verify missing symbols fail.
- [x] Implement the model and range text.
- [x] Re-run the new test.

### Task 2: Clip File Name Rules

**Files:**
- Create: `app/src/test/java/com/glass/safeclip/data/media/ClipFileNameTest.kt`
- Create: `app/src/main/java/com/glass/safeclip/data/media/ClipFileName.kt`

**Interfaces:**
- Produces: `ClipFileName.forVideo(displayName: String, startMs: Long, endMs: Long): String`

- [x] Write failing tests for preserving a safe base name and replacing unsafe characters.
- [x] Run the new test and verify missing symbol failure.
- [x] Implement the filename helper.
- [x] Re-run the new test.

### Task 3: Android Clip Exporter

**Files:**
- Modify: `gradle/libs.versions.toml`
- Modify: `app/build.gradle.kts`
- Create: `app/src/main/java/com/glass/safeclip/data/media/VideoClipExportResult.kt`
- Create: `app/src/main/java/com/glass/safeclip/data/media/AndroidVideoClipExporter.kt`

**Interfaces:**
- Consumes: `VideoClipSelection`, `ClipFileName`
- Produces: `data class VideoClipExportResult(...)`
- Produces: `suspend fun AndroidVideoClipExporter.exportClip(video: VideoCandidate, selection: VideoClipSelection): VideoClipExportResult`

- [x] Add Media3 Transformer dependencies using the same Media3 version as the existing player modules.
- [x] Implement app-owned clip output directory under external Movies.
- [x] Build a clipped `MediaItem` with start/end milliseconds.
- [x] Run Transformer and return result metadata after completion.

### Task 4: Preview UI Wiring

**Files:**
- Modify: `app/src/main/java/com/glass/safeclip/ui/video/VideoPreviewScreen.kt`
- Modify: `app/src/main/java/com/glass/safeclip/MainActivity.kt`

**Interfaces:**
- Consumes: `onExportClip: suspend (VideoCandidate, VideoClipSelection) -> Result<VideoClipExportResult>`

- [x] Add compact buttons beside `현재 장면 캡쳐`: `시작 지정`, `끝 지정`, `클립 저장`.
- [x] Show selected range and export progress/success/failure text.
- [x] Wire `MainActivity` to `AndroidVideoClipExporter`.

### Task 5: Verification And Docs

**Files:**
- Modify: `docs/exec-plans/2026-07-13-project-start.md`

- [x] Run unit tests.
- [x] Run debug build.
- [x] Update the exec-plan log with the clipping/export behavior and verification result.
