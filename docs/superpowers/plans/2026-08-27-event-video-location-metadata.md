# Event Video Location Metadata Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Capture the device location when the event button is pressed and store it as standard location metadata inside the generated MP4 without drawing it on the video.

**Architecture:** A recording-specific location provider returns an optional event location without making video export depend on location availability. `LiveRecordingViewModel` starts the one-shot lookup at button time and passes the result to `EventClipAssembler`. `Media3EventClipAssembler` writes `Mp4LocationData` through `InAppMp4Muxer`, so the existing submission metadata reader can reuse the value.

**Tech Stack:** Kotlin, Android `LocationManager`, coroutines, Media3 Transformer/Muxer 1.8.0, JUnit 4

**Spec:** `docs/exec-plans/2026-08-25-live-recording-device-validation.md`

## Global Constraints

- Location is not rendered into video frames.
- Missing permission, disabled providers, timeout, or lookup errors must not prevent event video creation.
- Location access remains runtime-permission based and is requested with the existing recording permission flow.
- The output stays in `DCIM/SafeClip` and remains compatible with `AndroidSubmissionMetadataReader`.

---

### Task 1: Event location handoff

**Files:**
- Create: `app/src/main/java/com/glass/safeclip/data/recording/EventLocation.kt`
- Create: `app/src/main/java/com/glass/safeclip/data/recording/EventLocationProvider.kt`
- Modify: `app/src/main/java/com/glass/safeclip/ui/recording/LiveRecordingViewModel.kt`
- Modify: `app/src/main/java/com/glass/safeclip/data/media/EventClipAssembler.kt`
- Test: `app/src/test/java/com/glass/safeclip/ui/recording/LiveRecordingViewModelTest.kt`

**Interfaces:**
- Produces: `EventLocation(latitude: Double, longitude: Double, accuracyMeters: Float?, capturedAtEpochMs: Long)`
- Produces: `EventLocationProvider.captureLocation(triggerEpochMs: Long): EventLocation?`
- Changes: `EventClipAssembler.assemble(plan, displayName, location)`

- [x] **Step 1: Write a failing ViewModel test** proving button-time location is forwarded to the assembler.
- [x] **Step 2: Run the focused test** and confirm it fails because the new location contract is absent.
- [x] **Step 3: Add the optional location contract and asynchronous handoff.** Start lookup at event creation, await it only when export starts, and pass `null` on failure.
- [x] **Step 4: Run the focused tests** and confirm location success and failure both preserve event export.

### Task 2: Android one-shot location capture

**Files:**
- Create: `app/src/main/java/com/glass/safeclip/data/recording/AndroidEventLocationProvider.kt`
- Modify: `app/src/main/java/com/glass/safeclip/ui/recording/LiveRecordingViewModelFactory.kt`
- Modify: `app/src/main/java/com/glass/safeclip/MainActivity.kt`
- Test: `app/src/test/java/com/glass/safeclip/data/recording/EventLocationSelectionTest.kt`

**Interfaces:**
- Consumes: location permission already declared in `AndroidManifest.xml`.
- Produces: a current GPS/network location with a bounded wait and a recent-last-known fallback.

- [x] **Step 1: Write failing pure selection tests** for choosing the closest valid location to the trigger and rejecting invalid coordinates.
- [x] **Step 2: Run the focused test** and confirm the selector is missing.
- [x] **Step 3: Implement the selector and Android provider.** Use enabled providers, a five-second timeout, and never throw to event export.
- [x] **Step 4: Extend the existing recording permission request** with fine/coarse location while keeping camera/storage as the only required permissions.
- [x] **Step 5: Run focused tests** and compile the Android sources.

### Task 3: MP4 location metadata

**Files:**
- Create: `app/src/main/java/com/glass/safeclip/data/media/EventLocationMetadataProvider.kt`
- Modify: `app/src/main/java/com/glass/safeclip/data/media/Media3EventClipAssembler.kt`
- Test: `app/src/test/java/com/glass/safeclip/data/media/EventLocationMetadataProviderTest.kt`

**Interfaces:**
- Consumes: optional `EventLocation` from Task 1.
- Produces: `Mp4LocationData` in the output MP4 `udta` metadata through `InAppMp4Muxer.Factory`.

- [x] **Step 1: Write a failing metadata-provider test** that expects exactly one MP4 location entry when location exists and none when absent.
- [x] **Step 2: Run the focused test** and confirm the provider is missing.
- [x] **Step 3: Configure Transformer with `InAppMp4Muxer.Factory`** and add `Mp4LocationData` without changing timestamp overlays.
- [x] **Step 4: Run media and submission metadata tests.**

### Task 4: Verification and handoff

**Files:**
- Modify: `docs/exec-plans/2026-08-25-live-recording-device-validation.md`

- [x] **Step 1: Run all unit tests.**
- [x] **Step 2: Run debug build, Android test compilation, lint, and release bundle build.**
- [x] **Step 3: Record that physical-device verification must confirm the MP4 metadata through the submission autofill path.**
