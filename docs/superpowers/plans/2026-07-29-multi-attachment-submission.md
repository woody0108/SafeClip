# Multi-Attachment Submission Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Let Android users include up to 2 videos and 5 JPEG photos in one SafeClip submission.

**Architecture:** Add a small attachment domain model that owns file type, limits, duplicate handling, and Firestore field conversion. Pass the current folder file list into the submission form so the UI can add or remove attachments while preserving the existing representative-file Firestore fields.

**Tech Stack:** Kotlin, Jetpack Compose, JUnit 4, Firebase Firestore field maps.

## Global Constraints

- Modify Android app files under `app/` only.
- Keep company web and NAS upload API unchanged.
- Use Android Storage Access Framework URI strings; do not assume filesystem paths.
- Video limit is exactly 2 per submission.
- JPEG photo limit is exactly 5 per submission.
- Keep existing Firestore representative fields for compatibility.
- Add short Korean comments only where behavior would be hard for a junior developer.

---

### Task 1: Attachment Model And Limits

**Files:**
- Create: `app/src/main/java/com/glass/safeclip/data/submission/SubmissionAttachment.kt`
- Test: `app/src/test/java/com/glass/safeclip/data/submission/SubmissionAttachmentTest.kt`

**Interfaces:**
- Produces: `data class SubmissionAttachment(...)`
- Produces: `object SubmissionAttachmentRules`
- Produces: `fun SubmissionAttachmentRules.add(current: List<SubmissionAttachment>, next: SubmissionAttachment): AttachmentSelectionResult`

- [ ] **Step 1: Write the failing test**

```kotlin
@Test
fun addBlocksMoreThanTwoVideos() {
    val first = attachment("content://video-1", "front.mp4", "video/mp4")
    val second = attachment("content://video-2", "rear.mp4", "video/mp4")
    val third = attachment("content://video-3", "side.mp4", "video/mp4")

    val result = SubmissionAttachmentRules.add(listOf(first, second), third)

    assertEquals(AttachmentSelectionResult.Rejected("영상은 최대 2개까지 첨부할 수 있습니다."), result)
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `.\gradlew.bat test --tests com.glass.safeclip.data.submission.SubmissionAttachmentTest`

Expected: FAIL because `SubmissionAttachment` and `SubmissionAttachmentRules` do not exist.

- [ ] **Step 3: Write minimal implementation**

Create `SubmissionAttachment.kt` with:

```kotlin
package com.glass.safeclip.data.submission

data class SubmissionAttachment(
    val uriString: String,
    val displayName: String,
    val mimeType: String,
    val sizeBytes: Long?,
    val folderPath: String,
    val kind: SubmissionAttachmentKind
)

enum class SubmissionAttachmentKind(val firestoreValue: String) {
    Video("video"),
    Photo("photo")
}

sealed interface AttachmentSelectionResult {
    data class Accepted(val attachments: List<SubmissionAttachment>) : AttachmentSelectionResult
    data class Rejected(val message: String) : AttachmentSelectionResult
}

object SubmissionAttachmentRules {
    const val MaxVideos = 2
    const val MaxPhotos = 5

    fun add(
        current: List<SubmissionAttachment>,
        next: SubmissionAttachment
    ): AttachmentSelectionResult {
        if (current.any { it.uriString == next.uriString }) {
            return AttachmentSelectionResult.Rejected("이미 첨부한 파일입니다.")
        }
        val videos = current.count { it.kind == SubmissionAttachmentKind.Video }
        val photos = current.count { it.kind == SubmissionAttachmentKind.Photo }
        if (next.kind == SubmissionAttachmentKind.Video && videos >= MaxVideos) {
            return AttachmentSelectionResult.Rejected("영상은 최대 2개까지 첨부할 수 있습니다.")
        }
        if (next.kind == SubmissionAttachmentKind.Photo && photos >= MaxPhotos) {
            return AttachmentSelectionResult.Rejected("사진은 최대 5개까지 첨부할 수 있습니다.")
        }
        return AttachmentSelectionResult.Accepted(current + next)
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `.\gradlew.bat test --tests com.glass.safeclip.data.submission.SubmissionAttachmentTest`

Expected: PASS.

### Task 2: Convert Managed Files To Attachments

**Files:**
- Modify: `app/src/main/java/com/glass/safeclip/data/submission/SubmissionAttachment.kt`
- Test: `app/src/test/java/com/glass/safeclip/data/submission/SubmissionAttachmentTest.kt`

**Interfaces:**
- Consumes: `ManagedFolderFile`
- Produces: `fun SubmissionAttachment.Companion.fromManagedFile(file: ManagedFolderFile, folderPath: String): SubmissionAttachment?`
- Produces: `fun SubmissionAttachment.Companion.fromVideoCandidate(video: VideoCandidate): SubmissionAttachment`

- [ ] **Step 1: Write failing tests**

```kotlin
@Test
fun fromManagedFileCreatesPhotoAttachmentForJpeg() {
    val file = ManagedFolderFile("content://photo-1", "plate.jpg", "image/jpeg", 12L)

    val attachment = SubmissionAttachment.fromManagedFile(file, "SafeClip 보관함")

    assertEquals(SubmissionAttachmentKind.Photo, attachment?.kind)
    assertEquals("plate.jpg", attachment?.displayName)
}

@Test
fun fromManagedFileRejectsUnsupportedFiles() {
    val file = ManagedFolderFile("content://note", "note.txt", "text/plain", 12L)

    val attachment = SubmissionAttachment.fromManagedFile(file, "SafeClip 보관함")

    assertNull(attachment)
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `.\gradlew.bat test --tests com.glass.safeclip.data.submission.SubmissionAttachmentTest`

Expected: FAIL because conversion functions do not exist.

- [ ] **Step 3: Write minimal implementation**

Add companion conversion functions that classify `video/*` and supported video names as `Video`, and `image/jpeg`, `.jpg`, `.jpeg` as `Photo`.

- [ ] **Step 4: Run test to verify it passes**

Run: `.\gradlew.bat test --tests com.glass.safeclip.data.submission.SubmissionAttachmentTest`

Expected: PASS.

### Task 3: Firestore Attachment Fields

**Files:**
- Modify: `app/src/main/java/com/glass/safeclip/data/submission/SubmissionInput.kt`
- Modify: `app/src/main/java/com/glass/safeclip/data/submission/SubmissionDocument.kt`
- Test: `app/src/test/java/com/glass/safeclip/data/submission/SubmissionDocumentTest.kt`

**Interfaces:**
- Consumes: `SubmissionInput.attachments: List<SubmissionAttachment>`
- Produces Firestore map fields: `attachments`, `videoCount`, `photoCount`

- [ ] **Step 1: Write failing test**

```kotlin
@Test
fun createFieldsStoresAttachmentListAndCounts() {
    val input = SubmissionInput(
        ownerUid = null,
        video = video("content://front", "front.mp4"),
        draft = readyDraft(),
        guestId = "guest-1",
        ownerDisplayName = null,
        ownerEmail = null,
        attachments = listOf(
            attachment("content://front", "front.mp4", "video/mp4", SubmissionAttachmentKind.Video),
            attachment("content://plate", "plate.jpg", "image/jpeg", SubmissionAttachmentKind.Photo)
        )
    )

    val fields = SubmissionDocument.createFields(input)

    assertEquals(1, fields["videoCount"])
    assertEquals(1, fields["photoCount"])
    val attachments = fields["attachments"] as List<Map<String, Any?>>
    assertEquals("front.mp4", attachments[0]["displayName"])
    assertEquals("video", attachments[0]["kind"])
    assertEquals("plate.jpg", attachments[1]["displayName"])
    assertEquals("photo", attachments[1]["kind"])
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `.\gradlew.bat test --tests com.glass.safeclip.data.submission.SubmissionDocumentTest`

Expected: FAIL because `SubmissionInput.attachments` and Firestore fields do not exist.

- [ ] **Step 3: Write minimal implementation**

Add `attachments: List<SubmissionAttachment>` to `SubmissionInput` with a default generated from `video` if needed by existing call sites. Add field conversion in `SubmissionDocument.createFields`.

- [ ] **Step 4: Run test to verify it passes**

Run: `.\gradlew.bat test --tests com.glass.safeclip.data.submission.SubmissionDocumentTest`

Expected: PASS.

### Task 4: Submission Form Multi-Attachment UI

**Files:**
- Modify: `app/src/main/java/com/glass/safeclip/ui/navigation/SafeClipScreen.kt`
- Modify: `app/src/main/java/com/glass/safeclip/ui/submission/SubmissionFormScreen.kt`
- Modify: `app/src/main/java/com/glass/safeclip/MainActivity.kt`
- Test: `app/src/test/java/com/glass/safeclip/ui/submission/SubmissionDraftTest.kt`

**Interfaces:**
- Consumes: `availableFiles: List<ManagedFolderFile>`
- Consumes: `initialAttachments: List<SubmissionAttachment>`
- Produces: `onSubmit(draft: SubmissionDraft, attachments: List<SubmissionAttachment>)`

- [ ] **Step 1: Write failing test**

```kotlin
@Test
fun submissionRequiresAtLeastOneAttachment() {
    val readyDraft = SubmissionDraft(
        incidentDateTime = "2026-07-29 10:00",
        locationText = "서울",
        incidentType = "신호위반",
        memo = "메모",
        reviewConsent = true,
        storageConsent = true,
        dataUseConsent = true
    )

    assertFalse(readyDraft.canSubmitWith(emptyList()))
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `.\gradlew.bat test --tests com.glass.safeclip.ui.submission.SubmissionDraftTest`

Expected: FAIL because `canSubmitWith` does not exist.

- [ ] **Step 3: Write minimal implementation**

Add `fun canSubmitWith(attachments: List<SubmissionAttachment>): Boolean = isReadyToSubmit && attachments.isNotEmpty()` to `SubmissionDraft`.

- [ ] **Step 4: Wire UI**

Update `SubmissionFormScreen` to show selected attachments, available eligible attachments, add/remove buttons, and limit messages using `SubmissionAttachmentRules.add`.

- [ ] **Step 5: Wire Activity**

Pass `currentFolderFilesOrFallback()` or `eventFolderFiles` into `SafeClipScreen.SubmissionForm` based on where the user started. Build `SubmissionInput.attachments` from selected UI attachments.

- [ ] **Step 6: Run related tests**

Run: `.\gradlew.bat test --tests com.glass.safeclip.ui.submission.SubmissionDraftTest --tests com.glass.safeclip.data.submission.SubmissionAttachmentTest --tests com.glass.safeclip.data.submission.SubmissionDocumentTest`

Expected: PASS.

### Task 5: Full Verification

**Files:**
- No production files.

**Interfaces:**
- Consumes completed tasks.
- Produces verification result.

- [ ] **Step 1: Run Android unit tests**

Run: `.\gradlew.bat test`

Expected: PASS if `JAVA_HOME` is configured. If `JAVA_HOME` is missing, report that exact blocker.

- [ ] **Step 2: Run server static checks**

Run: `powershell -ExecutionPolicy Bypass -File company-web-server/tests/Check-CompanyWebServer.ps1`

Expected: PASS.

Run: `powershell -ExecutionPolicy Bypass -File nas-upload-api/tests/UploadApiStaticTests.ps1`

Expected: PASS.
