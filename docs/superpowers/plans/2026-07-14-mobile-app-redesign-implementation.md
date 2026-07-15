# SafeClip Mobile App Redesign Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Rebuild the Android MVP UI into a coherent SafeClip mobile flow: start, home, video browser, preview/edit, submission form, and submission status.

**Architecture:** Keep the existing single-activity Compose app and SAF/media logic. Add a small screen-state model in `MainActivity`, shared visual components under `ui/components`, new screen packages for onboarding, home, submission, and status, and restyle the existing video screens without changing the working capture/clip export logic.

**Tech Stack:** Kotlin, Jetpack Compose, Material3, Media3 ExoPlayer/Transformer, Android Storage Access Framework, JUnit 4.

## Global Constraints

- Android mobile app redesign only; company server/admin dashboard is future web/admin scope.
- No real Firebase upload in this slice; submission creates local in-memory sample records.
- No real AI number-plate recognition, legal judgment, or automatic reporting.
- Keep current file access through Android Storage Access Framework.
- Keep cards at 8dp radius unless a circular icon is required.
- Avoid nested cards inside cards.
- Do not use large decorative blobs/orbs.
- The video player remains larger than surrounding controls.
- Text does not scale with viewport width.
- Controls must not overlap Android status/navigation bars.
- Preserve working playback, capture, trim, and clip save logic.

---

## File Structure

Create:

- `app/src/main/java/com/glass/safeclip/ui/navigation/SafeClipScreen.kt`  
  Screen state sealed interface for direct Compose navigation.
- `app/src/main/java/com/glass/safeclip/ui/components/SafeClipScaffold.kt`  
  Shared dark background, safe drawing padding, and vertical scrolling helper.
- `app/src/main/java/com/glass/safeclip/ui/components/SafeClipTopBar.kt`  
  Header/brand row used across screens.
- `app/src/main/java/com/glass/safeclip/ui/components/SafeClipButtons.kt`  
  Orange primary and blue secondary buttons.
- `app/src/main/java/com/glass/safeclip/ui/components/SafeClipPanels.kt`  
  `GlassPanel`, `StatusTile`, `StatusChip`, and `MetricStrip`.
- `app/src/main/java/com/glass/safeclip/ui/onboarding/StartScreen.kt`  
  Start/onboarding UI.
- `app/src/main/java/com/glass/safeclip/ui/home/MainHomeScreen.kt`  
  Main waiting/dashboard UI.
- `app/src/main/java/com/glass/safeclip/ui/video/VideoBrowserScreen.kt`  
  Restyled video list and selected video summary.
- `app/src/main/java/com/glass/safeclip/ui/submission/SubmissionDraft.kt`  
  Submission form data and validation rules.
- `app/src/main/java/com/glass/safeclip/ui/submission/SubmissionFormScreen.kt`  
  Local submission form UI.
- `app/src/main/java/com/glass/safeclip/ui/status/SubmissionStatusModels.kt`  
  Local submission record and status label/color mapping.
- `app/src/main/java/com/glass/safeclip/ui/status/SubmissionStatusScreen.kt`  
  Submission history/status UI.
- `app/src/test/java/com/glass/safeclip/ui/submission/SubmissionDraftTest.kt`
- `app/src/test/java/com/glass/safeclip/ui/status/SubmissionStatusUiTextTest.kt`
- `app/src/test/java/com/glass/safeclip/ui/navigation/SafeClipScreenTest.kt`

Modify:

- `app/src/main/java/com/glass/safeclip/MainActivity.kt`  
  Replace binary list/preview state with `SafeClipScreen`, route folder picker and screen callbacks.
- `app/src/main/java/com/glass/safeclip/ui/theme/Color.kt`  
  Add SafeClip dark palette colors.
- `app/src/main/java/com/glass/safeclip/ui/theme/Theme.kt`  
  Use the SafeClip dark scheme by default and disable dynamic color for brand consistency.
- `app/src/main/java/com/glass/safeclip/ui/video/VideoListScreen.kt`  
  Replace or delegate to `VideoBrowserScreen`, keeping compatibility only if useful.
- `app/src/main/java/com/glass/safeclip/ui/video/VideoPreviewScreen.kt`  
  Restyle as the preview/edit screen and add `제출하기` callback.
- `app/src/main/java/com/glass/safeclip/ui/video/VideoListState.kt`  
  Add small derived helpers only if required by home/browser screens.
- `docs/exec-plans/2026-07-13-project-start.md`  
  Record redesign implementation and verification results.

---

### Task 1: Navigation State And Submission Rules

**Files:**
- Create: `app/src/main/java/com/glass/safeclip/ui/navigation/SafeClipScreen.kt`
- Create: `app/src/main/java/com/glass/safeclip/ui/submission/SubmissionDraft.kt`
- Create: `app/src/main/java/com/glass/safeclip/ui/status/SubmissionStatusModels.kt`
- Test: `app/src/test/java/com/glass/safeclip/ui/navigation/SafeClipScreenTest.kt`
- Test: `app/src/test/java/com/glass/safeclip/ui/submission/SubmissionDraftTest.kt`
- Test: `app/src/test/java/com/glass/safeclip/ui/status/SubmissionStatusUiTextTest.kt`

**Interfaces:**
- Produces: `sealed interface SafeClipScreen`
- Produces: `data class SubmissionDraft(...)`
- Produces: `SubmissionDraft.isReadyToSubmit: Boolean`
- Produces: `enum class SubmissionStatus`
- Produces: `object SubmissionStatusUiText`
- Produces: `data class LocalSubmissionRecord(...)`

- [ ] **Step 1: Write navigation test**

Create `app/src/test/java/com/glass/safeclip/ui/navigation/SafeClipScreenTest.kt`:

```kotlin
package com.glass.safeclip.ui.navigation

import com.glass.safeclip.domain.model.VideoCandidate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SafeClipScreenTest {
    @Test
    fun startScreenIsFirstScreen() {
        assertEquals(SafeClipScreen.Start, SafeClipScreen.first())
    }

    @Test
    fun previewScreenCarriesSelectedVideo() {
        val video = VideoCandidate("uri-1", "front.mp4", 1000, 2000, "EVENT")
        val screen = SafeClipScreen.VideoPreview(video)

        assertTrue(screen is SafeClipScreen.VideoPreview)
        assertEquals("front.mp4", screen.video.displayName)
    }
}
```

- [ ] **Step 2: Write submission draft test**

Create `app/src/test/java/com/glass/safeclip/ui/submission/SubmissionDraftTest.kt`:

```kotlin
package com.glass.safeclip.ui.submission

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SubmissionDraftTest {
    @Test
    fun draftIsNotReadyWhenRequiredFieldsAreMissing() {
        assertFalse(SubmissionDraft().isReadyToSubmit)
    }

    @Test
    fun draftIsReadyWhenFieldsAndConsentAreComplete() {
        val draft = SubmissionDraft(
            incidentDateTime = "2026.07.14 10:30",
            locationText = "서울 강남구 테헤란로",
            incidentType = "추돌 사고",
            memo = "급정거 후 추돌",
            reviewConsent = true,
            storageConsent = true,
            dataUseConsent = true
        )

        assertTrue(draft.isReadyToSubmit)
    }
}
```

- [ ] **Step 3: Write status text test**

Create `app/src/test/java/com/glass/safeclip/ui/status/SubmissionStatusUiTextTest.kt`:

```kotlin
package com.glass.safeclip.ui.status

import org.junit.Assert.assertEquals
import org.junit.Test

class SubmissionStatusUiTextTest {
    @Test
    fun statusLabelsUseKoreanMvpLabels() {
        assertEquals("업로드중", SubmissionStatusUiText.labelFor(SubmissionStatus.Uploading))
        assertEquals("검토대기", SubmissionStatusUiText.labelFor(SubmissionStatus.WaitingReview))
        assertEquals("검토중", SubmissionStatusUiText.labelFor(SubmissionStatus.Reviewing))
        assertEquals("자료보완필요", SubmissionStatusUiText.labelFor(SubmissionStatus.NeedsMoreInfo))
        assertEquals("신고자료준비완료", SubmissionStatusUiText.labelFor(SubmissionStatus.ReportPackageReady))
        assertEquals("반려", SubmissionStatusUiText.labelFor(SubmissionStatus.Rejected))
        assertEquals("완료", SubmissionStatusUiText.labelFor(SubmissionStatus.Completed))
    }
}
```

- [ ] **Step 4: Run tests and verify RED**

Run:

```powershell
$env:JAVA_HOME='C:\Program Files\Android\Android Studio\jbr'; $env:Path="$env:JAVA_HOME\bin;$env:Path"; .\gradlew.bat :app:testDebugUnitTest
```

Expected: FAIL with unresolved references for `SafeClipScreen`, `SubmissionDraft`, `SubmissionStatus`, and `SubmissionStatusUiText`.

- [ ] **Step 5: Implement navigation state**

Create `app/src/main/java/com/glass/safeclip/ui/navigation/SafeClipScreen.kt`:

```kotlin
package com.glass.safeclip.ui.navigation

import com.glass.safeclip.data.media.VideoClipExportResult
import com.glass.safeclip.domain.model.VideoCandidate

sealed interface SafeClipScreen {
    data object Start : SafeClipScreen
    data object Home : SafeClipScreen
    data object VideoBrowser : SafeClipScreen
    data class VideoPreview(val video: VideoCandidate) : SafeClipScreen
    data class SubmissionForm(
        val video: VideoCandidate,
        val clip: VideoClipExportResult?
    ) : SafeClipScreen
    data object SubmissionStatus : SafeClipScreen

    companion object {
        fun first(): SafeClipScreen = Start
    }
}
```

- [ ] **Step 6: Implement submission draft**

Create `app/src/main/java/com/glass/safeclip/ui/submission/SubmissionDraft.kt`:

```kotlin
package com.glass.safeclip.ui.submission

data class SubmissionDraft(
    val incidentDateTime: String = "",
    val locationText: String = "",
    val incidentType: String = "",
    val memo: String = "",
    val reviewConsent: Boolean = false,
    val storageConsent: Boolean = false,
    val dataUseConsent: Boolean = false
) {
    val isReadyToSubmit: Boolean
        get() = incidentDateTime.isNotBlank() &&
            locationText.isNotBlank() &&
            incidentType.isNotBlank() &&
            memo.isNotBlank() &&
            reviewConsent &&
            storageConsent &&
            dataUseConsent
}
```

- [ ] **Step 7: Implement status models**

Create `app/src/main/java/com/glass/safeclip/ui/status/SubmissionStatusModels.kt`:

```kotlin
package com.glass.safeclip.ui.status

import com.glass.safeclip.domain.model.VideoCandidate

enum class SubmissionStatus {
    Uploading,
    WaitingReview,
    Reviewing,
    NeedsMoreInfo,
    ReportPackageReady,
    Rejected,
    Completed
}

object SubmissionStatusUiText {
    fun labelFor(status: SubmissionStatus): String {
        return when (status) {
            SubmissionStatus.Uploading -> "업로드중"
            SubmissionStatus.WaitingReview -> "검토대기"
            SubmissionStatus.Reviewing -> "검토중"
            SubmissionStatus.NeedsMoreInfo -> "자료보완필요"
            SubmissionStatus.ReportPackageReady -> "신고자료준비완료"
            SubmissionStatus.Rejected -> "반려"
            SubmissionStatus.Completed -> "완료"
        }
    }
}

data class LocalSubmissionRecord(
    val id: String,
    val video: VideoCandidate,
    val title: String,
    val incidentDateTime: String,
    val locationText: String,
    val status: SubmissionStatus
)
```

- [ ] **Step 8: Run tests and verify GREEN**

Run:

```powershell
$env:JAVA_HOME='C:\Program Files\Android\Android Studio\jbr'; $env:Path="$env:JAVA_HOME\bin;$env:Path"; .\gradlew.bat :app:testDebugUnitTest
```

Expected: PASS.

---

### Task 2: Shared SafeClip Visual System

**Files:**
- Modify: `app/src/main/java/com/glass/safeclip/ui/theme/Color.kt`
- Modify: `app/src/main/java/com/glass/safeclip/ui/theme/Theme.kt`
- Create: `app/src/main/java/com/glass/safeclip/ui/components/SafeClipScaffold.kt`
- Create: `app/src/main/java/com/glass/safeclip/ui/components/SafeClipTopBar.kt`
- Create: `app/src/main/java/com/glass/safeclip/ui/components/SafeClipButtons.kt`
- Create: `app/src/main/java/com/glass/safeclip/ui/components/SafeClipPanels.kt`

**Interfaces:**
- Produces: `SafeClipTheme` branded dark scheme.
- Produces: `SafeClipScaffold`, `SafeClipTopBar`, `GlassPanel`, `PrimaryActionButton`, `SecondaryActionButton`, `StatusTile`, `StatusChip`, `MetricStrip`.

- [ ] **Step 1: Update color palette**

Modify `app/src/main/java/com/glass/safeclip/ui/theme/Color.kt` to add:

```kotlin
val SafeClipBackground = Color(0xFF03101F)
val SafeClipSurface = Color(0xFF071A33)
val SafeClipSurfaceHigh = Color(0xFF0B2747)
val SafeClipPrimaryBlue = Color(0xFF159BFF)
val SafeClipCyan = Color(0xFF32D4FF)
val SafeClipOrange = Color(0xFFFF5A1F)
val SafeClipSuccess = Color(0xFF2FE58F)
val SafeClipWarning = Color(0xFFF6A540)
val SafeClipError = Color(0xFFFF6B6B)
val SafeClipTextPrimary = Color(0xFFF7FBFF)
val SafeClipTextSecondary = Color(0xFF9FB6D8)
val SafeClipBorder = Color(0xFF1D4B77)
```

- [ ] **Step 2: Update theme**

Modify `app/src/main/java/com/glass/safeclip/ui/theme/Theme.kt` so `SafeClipTheme` defaults to `dynamicColor = false` and uses the SafeClip dark palette:

```kotlin
private val SafeClipDarkColorScheme = darkColorScheme(
    primary = SafeClipPrimaryBlue,
    secondary = SafeClipCyan,
    tertiary = SafeClipOrange,
    background = SafeClipBackground,
    surface = SafeClipSurface,
    surfaceVariant = SafeClipSurfaceHigh,
    error = SafeClipError,
    onPrimary = Color.White,
    onSecondary = SafeClipBackground,
    onTertiary = Color.White,
    onBackground = SafeClipTextPrimary,
    onSurface = SafeClipTextPrimary,
    onSurfaceVariant = SafeClipTextSecondary,
    onError = Color.White
)
```

- [ ] **Step 3: Add scaffold**

Create `app/src/main/java/com/glass/safeclip/ui/components/SafeClipScaffold.kt`:

```kotlin
package com.glass.safeclip.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.glass.safeclip.ui.theme.SafeClipBackground

@Composable
fun SafeClipScaffold(
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(SafeClipBackground)
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(contentPadding)
    ) {
        content()
    }
}
```

- [ ] **Step 4: Add top bar**

Create `app/src/main/java/com/glass/safeclip/ui/components/SafeClipTopBar.kt`:

```kotlin
package com.glass.safeclip.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun SafeClipTopBar(
    title: String = "SafeClip",
    subtitle: String,
    trailing: @Composable (() -> Unit)? = null
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = subtitle,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 13.sp
            )
        }
        trailing?.invoke()
    }
}
```

- [ ] **Step 5: Add buttons**

Create `app/src/main/java/com/glass/safeclip/ui/components/SafeClipButtons.kt` with `PrimaryActionButton` and `SecondaryActionButton` as Material3 `Button` / `OutlinedButton` wrappers using 8dp radius and no nested cards.

- [ ] **Step 6: Add panels**

Create `app/src/main/java/com/glass/safeclip/ui/components/SafeClipPanels.kt` with:

```kotlin
@Composable fun GlassPanel(...)
@Composable fun StatusTile(label: String, value: String, state: String, ...)
@Composable fun StatusChip(label: String, tone: StatusTone, ...)
@Composable fun MetricStrip(metrics: List<Pair<String, String>>, ...)
enum class StatusTone { Neutral, Info, Success, Warning, Error }
```

- [ ] **Step 7: Run build check**

Run:

```powershell
$env:JAVA_HOME='C:\Program Files\Android\Android Studio\jbr'; $env:Path="$env:JAVA_HOME\bin;$env:Path"; .\gradlew.bat :app:testDebugUnitTest
```

Expected: PASS.

---

### Task 3: Start And Home Screens

**Files:**
- Create: `app/src/main/java/com/glass/safeclip/ui/onboarding/StartScreen.kt`
- Create: `app/src/main/java/com/glass/safeclip/ui/home/MainHomeScreen.kt`
- Modify: `app/src/main/java/com/glass/safeclip/MainActivity.kt`

**Interfaces:**
- Consumes: shared components from Task 2.
- Produces: `StartScreen(onStart: () -> Unit)`
- Produces: `MainHomeScreen(state: VideoListState, onLoadVideos: () -> Unit, onOpenRecentEvents: () -> Unit, onOpenStatus: () -> Unit)`

- [ ] **Step 1: Add StartScreen**

Create a `StartScreen` that shows:

```text
SafeClip
블랙박스 영상을 쉽고 빠르게 제출
USB-C 연결 / microSD 인식 / 이벤트 영상 가져오기
시작하기
원본 영상은 사용자가 선택할 때만 처리됩니다.
```

Use `SafeClipScaffold`, `GlassPanel`, and `PrimaryActionButton`.

- [ ] **Step 2: Add MainHomeScreen**

Create a `MainHomeScreen` that shows:

```text
SafeClip
블랙박스 이벤트 영상 관리
리더기 연결 / microSD 인식 / 이벤트 폴더 / 저장 용량
블랙박스 이벤트 영상을 간편하게 불러오세요
영상 불러오기
최근 이벤트 보기
전방 영상 / 후방 영상 / 제출 내역 / 설정
```

Use `state.videos.size` to show whether events are loaded.

- [ ] **Step 3: Wire initial navigation in MainActivity**

Modify `MainActivity.kt`:

```kotlin
var screen by remember { mutableStateOf<SafeClipScreen>(SafeClipScreen.first()) }
```

Route:

- `SafeClipScreen.Start` -> `StartScreen(onStart = { screen = SafeClipScreen.Home })`
- `SafeClipScreen.Home` -> `MainHomeScreen(...)`
- `onLoadVideos` launches existing folder picker.
- Folder picker success sets `screen = SafeClipScreen.VideoBrowser`.
- `onOpenRecentEvents` opens `VideoBrowser` only if `state.videos.isNotEmpty()`.

- [ ] **Step 4: Run app tests**

Run:

```powershell
$env:JAVA_HOME='C:\Program Files\Android\Android Studio\jbr'; $env:Path="$env:JAVA_HOME\bin;$env:Path"; .\gradlew.bat :app:testDebugUnitTest
```

Expected: PASS.

---

### Task 4: Video Browser Screen

**Files:**
- Create: `app/src/main/java/com/glass/safeclip/ui/video/VideoBrowserScreen.kt`
- Modify: `app/src/main/java/com/glass/safeclip/MainActivity.kt`
- Optional Modify: `app/src/main/java/com/glass/safeclip/ui/video/VideoListScreen.kt`

**Interfaces:**
- Produces: `VideoBrowserScreen(state: VideoListState, selectedVideo: VideoCandidate?, onSelectVideo: (VideoCandidate) -> Unit, onPlayVideo: (VideoCandidate) -> Unit, onSubmitVideo: (VideoCandidate) -> Unit, onSelectFolder: () -> Unit, onBackHome: () -> Unit)`

- [ ] **Step 1: Add VideoBrowserScreen**

Create a restyled screen with:

- top bar `이벤트 영상 확인`
- folder/video count summary panel
- segmented visual row: `전체`, `전방`, `후방`
- scrollable candidate list
- selected video summary panel
- actions: `영상 재생`, `제출하기`, `폴더 다시 선택`

- [ ] **Step 2: Preserve existing empty/loading/error behavior**

Use the existing `VideoListState` values:

- `isLoading`
- `errorMessage`
- `selectedFolderName`
- `videos`

Show:

- loading text: `영상 목록을 불러오는 중입니다.`
- empty text: `아직 표시할 영상이 없습니다.`
- error text from `state.errorMessage`

- [ ] **Step 3: Wire MainActivity**

In `SafeClipScreen.VideoBrowser`, render `VideoBrowserScreen`.

Set `selectedVideo` as local remembered state. If `state.videos` changes and current selected video is null, choose `state.videos.firstOrNull()`.

Callbacks:

- `onPlayVideo = { screen = SafeClipScreen.VideoPreview(it) }`
- `onSubmitVideo = { screen = SafeClipScreen.SubmissionForm(it, null) }`
- `onBackHome = { screen = SafeClipScreen.Home }`

- [ ] **Step 4: Run build check**

Run:

```powershell
$env:JAVA_HOME='C:\Program Files\Android\Android Studio\jbr'; $env:Path="$env:JAVA_HOME\bin;$env:Path"; .\gradlew.bat :app:testDebugUnitTest
```

Expected: PASS.

---

### Task 5: Preview/Edit Screen Restyle And Submit Entry

**Files:**
- Modify: `app/src/main/java/com/glass/safeclip/ui/video/VideoPreviewScreen.kt`
- Modify: `app/src/main/java/com/glass/safeclip/MainActivity.kt`

**Interfaces:**
- Modify: `VideoPreviewScreen(...)` adds `onSubmit: (VideoCandidate, VideoClipExportResult?) -> Unit`
- Keeps: `onCaptureFrame`, `onExportClip`, playback speed, capture, trim, clip save.

- [ ] **Step 1: Add `savedClip` UI state**

Inside `VideoPreviewScreen`, add:

```kotlin
var savedClip by remember(video.uriString) { mutableStateOf<VideoClipExportResult?>(null) }
```

When clip export succeeds, assign:

```kotlin
savedClip = it
```

- [ ] **Step 2: Add submit button**

Add a `PrimaryActionButton(text = "제출하기")` below the status message.

On click:

```kotlin
onSubmit(video, savedClip)
```

- [ ] **Step 3: Restyle with shared components**

Wrap content in `SafeClipScaffold`. Use `SafeClipTopBar` style title `영상 확인 및 편집`. Keep the `AndroidView` video player weighted as the largest area.

- [ ] **Step 4: Wire MainActivity**

In `SafeClipScreen.VideoPreview(video)`, pass:

```kotlin
onSubmit = { selected, clip ->
    screen = SafeClipScreen.SubmissionForm(selected, clip)
}
```

- [ ] **Step 5: Run build check**

Run:

```powershell
$env:JAVA_HOME='C:\Program Files\Android\Android Studio\jbr'; $env:Path="$env:JAVA_HOME\bin;$env:Path"; .\gradlew.bat :app:testDebugUnitTest
```

Expected: PASS.

---

### Task 6: Submission Form Screen

**Files:**
- Create: `app/src/main/java/com/glass/safeclip/ui/submission/SubmissionFormScreen.kt`
- Modify: `app/src/main/java/com/glass/safeclip/MainActivity.kt`

**Interfaces:**
- Consumes: `SubmissionDraft`, `VideoCandidate`, `VideoClipExportResult?`
- Produces: `SubmissionFormScreen(video: VideoCandidate, clip: VideoClipExportResult?, onBack: () -> Unit, onSubmit: (SubmissionDraft) -> Unit)`

- [ ] **Step 1: Add form screen**

Create fields:

- `사고 일시`
- `위치`
- `사고 유형`
- `메모`

Create consent checkboxes:

- `회사 검토에 동의합니다.`
- `영상 보관에 동의합니다.`
- `교통 위험 데이터 활용에 동의합니다.`

Show selected video summary:

- original file name
- clip path when available
- folder path
- size

- [ ] **Step 2: Disable submit until ready**

Use:

```kotlin
enabled = draft.isReadyToSubmit
```

for the `제출하기` button.

- [ ] **Step 3: Wire MainActivity**

When submit succeeds:

- create `LocalSubmissionRecord`
- append it to `localSubmissions`
- set `screen = SafeClipScreen.SubmissionStatus`

Use title:

```kotlin
draft.incidentType.ifBlank { "블랙박스 영상 제출" }
```

- [ ] **Step 4: Run tests**

Run:

```powershell
$env:JAVA_HOME='C:\Program Files\Android\Android Studio\jbr'; $env:Path="$env:JAVA_HOME\bin;$env:Path"; .\gradlew.bat :app:testDebugUnitTest
```

Expected: PASS.

---

### Task 7: Submission Status Screen

**Files:**
- Create: `app/src/main/java/com/glass/safeclip/ui/status/SubmissionStatusScreen.kt`
- Modify: `app/src/main/java/com/glass/safeclip/MainActivity.kt`

**Interfaces:**
- Consumes: `List<LocalSubmissionRecord>`
- Produces: `SubmissionStatusScreen(records: List<LocalSubmissionRecord>, onBackHome: () -> Unit, onOpenSubmission: (LocalSubmissionRecord) -> Unit)`

- [ ] **Step 1: Add status screen**

Show:

- `제출 현황`
- summary counters for `제출`, `검토중`, `자료생성`, `결과회신`
- record list with title, incident date/time, location, status chip
- empty state: `아직 제출한 영상이 없습니다.`

- [ ] **Step 2: Add status colors**

Use `StatusTone`:

- `Uploading`, `WaitingReview`, `Reviewing` -> `Warning`
- `NeedsMoreInfo`, `Rejected` -> `Error`
- `ReportPackageReady`, `Completed` -> `Success`

- [ ] **Step 3: Wire MainActivity**

Render `SubmissionStatusScreen` for `SafeClipScreen.SubmissionStatus`.

Home shortcut `제출 내역` opens `SafeClipScreen.SubmissionStatus`.

- [ ] **Step 4: Run tests**

Run:

```powershell
$env:JAVA_HOME='C:\Program Files\Android\Android Studio\jbr'; $env:Path="$env:JAVA_HOME\bin;$env:Path"; .\gradlew.bat :app:testDebugUnitTest
```

Expected: PASS.

---

### Task 8: Final Integration, Docs, And Verification

**Files:**
- Modify: `docs/exec-plans/2026-07-13-project-start.md`
- Review: all files touched in Tasks 1-7.

**Interfaces:**
- Produces a buildable redesigned app with local-only submission flow.

- [ ] **Step 1: Update execution log**

Append to `docs/exec-plans/2026-07-13-project-start.md`:

```markdown
## Changed on 2026-07-14

- Implemented Android mobile app redesign based on `docs/superpowers/specs/2026-07-14-mobile-app-redesign.md`.
- Added start, home, video browser, preview/edit submit entry, submission form, and submission status screens.
- Kept company server/admin dashboard out of Android scope.
- Kept existing SAF scan, playback, capture, and clip export logic.
- Verification:
  - `.\gradlew.bat :app:testDebugUnitTest` passed.
  - `.\gradlew.bat :app:assembleDebug` passed.
```

- [ ] **Step 2: Run full unit tests**

Run:

```powershell
$env:JAVA_HOME='C:\Program Files\Android\Android Studio\jbr'; $env:Path="$env:JAVA_HOME\bin;$env:Path"; .\gradlew.bat :app:testDebugUnitTest
```

Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 3: Run debug build**

Run:

```powershell
$env:JAVA_HOME='C:\Program Files\Android\Android Studio\jbr'; $env:Path="$env:JAVA_HOME\bin;$env:Path"; .\gradlew.bat :app:assembleDebug
```

Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 4: Manual smoke test on device**

Steps:

1. Launch app.
2. Tap `시작하기`.
3. Tap `영상 불러오기`.
4. Select SD card or test folder.
5. Confirm `이벤트 영상 확인` shows video candidates.
6. Open one video.
7. Capture a frame.
8. Mark start/end and save a clip.
9. Tap `제출하기`.
10. Fill form and consent.
11. Tap `제출하기`.
12. Confirm `제출 현황` shows a new record.

Expected: No crash, no overlapping text, no status bar overlap, and core existing media actions still work.

---

## Self-Review

- Spec coverage: all mobile screens, shared visual system, direct navigation, local submission records, and out-of-scope admin/Firebase items are covered.
- Empty-field scan: no task uses `TBD`, `TODO`, or unspecified implementation steps.
- Type consistency: `SafeClipScreen`, `SubmissionDraft`, `SubmissionStatus`, and `LocalSubmissionRecord` are introduced before screen tasks consume them.
- Scope check: this plan stays Android-only and does not implement the company server/admin dashboard.
- Repository note: `C:\Users\win11\GitHub\SafeClip` is not currently a Git repository, so commit steps are intentionally omitted.
