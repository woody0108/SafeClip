# SD Card Video List Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build the first SafeClip Android slice that lets a user select an SD card or USB-reader folder and see recent supported blackbox video candidates.

**Architecture:** Keep file-selection UI thin and move video filtering/sorting into testable Kotlin classes. Android Storage Access Framework returns a tree URI; an Android adapter converts that tree into lightweight video document records; Compose screens render the app state.

**Tech Stack:** Kotlin, Jetpack Compose, Android Storage Access Framework, AndroidX DocumentFile, JUnit 4.

## Global Constraints

- MVP target is Android submission MVP: video selection, consent, upload, and submission status.
- First implementation slice is only SD card / USB reader folder selection and recent video list.
- Do not implement phone camera recording or background camera service.
- Use Android Storage Access Framework; do not assume direct filesystem paths for USB reader files.
- Candidate extensions: `.mp4`, `.mov`, `.avi`, `.ts`.
- Preferred folder hints: `EVENT`, `EMERGENCY`, `RO`, `MOVIE`, `PARKING`, `NORMAL`.
- Sort candidate videos by last modified time descending, then file name descending.
- This folder is not currently a Git repository, so commit steps are replaced by file review steps.

---

## File Structure

- Modify `gradle/libs.versions.toml`: add AndroidX DocumentFile dependency coordinates.
- Modify `app/build.gradle.kts`: add the DocumentFile dependency.
- Create `app/src/main/java/com/glass/safeclip/domain/model/VideoCandidate.kt`: immutable video candidate model used by UI and data code.
- Create `app/src/main/java/com/glass/safeclip/data/file/VideoCandidateRules.kt`: pure Kotlin filtering and sorting rules.
- Create `app/src/main/java/com/glass/safeclip/data/file/VideoDocument.kt`: lightweight file-tree document model for scanner input.
- Create `app/src/main/java/com/glass/safeclip/data/file/VideoScanner.kt`: recursively scans lightweight documents and returns sorted candidates.
- Create `app/src/main/java/com/glass/safeclip/data/file/AndroidDocumentTreeVideoSource.kt`: SAF / DocumentFile adapter for real Android folder trees.
- Create `app/src/main/java/com/glass/safeclip/ui/video/VideoListState.kt`: UI state types.
- Create `app/src/main/java/com/glass/safeclip/ui/video/VideoListScreen.kt`: Compose UI for picker entry, selected folder summary, video list, empty state, and error state.
- Modify `app/src/main/java/com/glass/safeclip/MainActivity.kt`: wire ActivityResult folder picker, URI permission, scanner, and Compose screen.
- Create `app/src/test/java/com/glass/safeclip/data/file/VideoCandidateRulesTest.kt`: unit tests for extension filtering and sort order.
- Create `app/src/test/java/com/glass/safeclip/data/file/VideoScannerTest.kt`: unit tests for recursive scanning and unsupported file exclusion.

---

### Task 1: Video Candidate Rules

**Files:**
- Create: `app/src/main/java/com/glass/safeclip/domain/model/VideoCandidate.kt`
- Create: `app/src/main/java/com/glass/safeclip/data/file/VideoCandidateRules.kt`
- Test: `app/src/test/java/com/glass/safeclip/data/file/VideoCandidateRulesTest.kt`

**Interfaces:**
- Produces: `data class VideoCandidate(val uriString: String, val displayName: String, val sizeBytes: Long?, val lastModifiedMillis: Long?, val folderPath: String)`
- Produces: `object VideoCandidateRules`
- Produces: `fun VideoCandidateRules.isSupportedVideoFile(displayName: String): Boolean`
- Produces: `fun VideoCandidateRules.sortRecentFirst(candidates: List<VideoCandidate>): List<VideoCandidate>`

- [ ] **Step 1: Write the failing tests**

Create `app/src/test/java/com/glass/safeclip/data/file/VideoCandidateRulesTest.kt`:

```kotlin
package com.glass.safeclip.data.file

import com.glass.safeclip.domain.model.VideoCandidate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class VideoCandidateRulesTest {
    @Test
    fun supportsCommonBlackboxVideoExtensionsCaseInsensitive() {
        assertTrue(VideoCandidateRules.isSupportedVideoFile("front_001.MP4"))
        assertTrue(VideoCandidateRules.isSupportedVideoFile("rear_001.mov"))
        assertTrue(VideoCandidateRules.isSupportedVideoFile("event_001.AVI"))
        assertTrue(VideoCandidateRules.isSupportedVideoFile("clip_001.ts"))
    }

    @Test
    fun rejectsNonVideoAndExtensionlessNames() {
        assertFalse(VideoCandidateRules.isSupportedVideoFile("readme.txt"))
        assertFalse(VideoCandidateRules.isSupportedVideoFile("thumbnail.jpg"))
        assertFalse(VideoCandidateRules.isSupportedVideoFile("EVENT_FILE"))
    }

    @Test
    fun sortsByLastModifiedDescendingThenNameDescending() {
        val candidates = listOf(
            VideoCandidate("uri-a", "A.mp4", 100, 2000, "EVENT"),
            VideoCandidate("uri-c", "C.mp4", 100, 3000, "EVENT"),
            VideoCandidate("uri-b", "B.mp4", 100, 3000, "EVENT")
        )

        val sorted = VideoCandidateRules.sortRecentFirst(candidates)

        assertEquals(listOf("C.mp4", "B.mp4", "A.mp4"), sorted.map { it.displayName })
    }
}
```

- [ ] **Step 2: Run tests to verify they fail**

Run:

```powershell
.\gradlew.bat :app:testDebugUnitTest
```

Expected: FAIL because `VideoCandidate` and `VideoCandidateRules` do not exist.

- [ ] **Step 3: Add minimal implementation**

Create `app/src/main/java/com/glass/safeclip/domain/model/VideoCandidate.kt`:

```kotlin
package com.glass.safeclip.domain.model

data class VideoCandidate(
    val uriString: String,
    val displayName: String,
    val sizeBytes: Long?,
    val lastModifiedMillis: Long?,
    val folderPath: String
)
```

Create `app/src/main/java/com/glass/safeclip/data/file/VideoCandidateRules.kt`:

```kotlin
package com.glass.safeclip.data.file

import com.glass.safeclip.domain.model.VideoCandidate
import java.util.Locale

object VideoCandidateRules {
    private val supportedExtensions = setOf("mp4", "mov", "avi", "ts")

    fun isSupportedVideoFile(displayName: String): Boolean {
        val extension = displayName.substringAfterLast('.', missingDelimiterValue = "")
            .lowercase(Locale.ROOT)
        return extension in supportedExtensions
    }

    fun sortRecentFirst(candidates: List<VideoCandidate>): List<VideoCandidate> {
        return candidates.sortedWith(
            compareByDescending<VideoCandidate> { it.lastModifiedMillis ?: Long.MIN_VALUE }
                .thenByDescending { it.displayName }
        )
    }
}
```

- [ ] **Step 4: Run tests to verify they pass**

Run:

```powershell
.\gradlew.bat :app:testDebugUnitTest
```

Expected: PASS.

- [ ] **Step 5: Review changed files**

Run:

```powershell
Get-ChildItem -LiteralPath .\app\src\main\java\com\glass\safeclip\data\file,.\app\src\main\java\com\glass\safeclip\domain\model,.\app\src\test\java\com\glass\safeclip\data\file -Recurse -File | Select-Object FullName
```

Expected: the three files from this task exist.

---

### Task 2: Recursive Video Scanner

**Files:**
- Create: `app/src/main/java/com/glass/safeclip/data/file/VideoDocument.kt`
- Create: `app/src/main/java/com/glass/safeclip/data/file/VideoScanner.kt`
- Test: `app/src/test/java/com/glass/safeclip/data/file/VideoScannerTest.kt`

**Interfaces:**
- Consumes: `VideoCandidate`
- Consumes: `VideoCandidateRules.isSupportedVideoFile(displayName: String): Boolean`
- Consumes: `VideoCandidateRules.sortRecentFirst(candidates: List<VideoCandidate>): List<VideoCandidate>`
- Produces: `data class VideoDocument(...)`
- Produces: `class VideoScanner`
- Produces: `fun VideoScanner.scan(root: VideoDocument): List<VideoCandidate>`

- [ ] **Step 1: Write the failing tests**

Create `app/src/test/java/com/glass/safeclip/data/file/VideoScannerTest.kt`:

```kotlin
package com.glass.safeclip.data.file

import org.junit.Assert.assertEquals
import org.junit.Test

class VideoScannerTest {
    @Test
    fun scanReturnsOnlySupportedVideosFromNestedFolders() {
        val root = VideoDocument.folder(
            displayName = "BLACKBOX",
            uriString = "tree-root",
            children = listOf(
                VideoDocument.folder(
                    displayName = "EVENT",
                    uriString = "tree-event",
                    children = listOf(
                        VideoDocument.file("front.mp4", "uri-front", 100, 3000),
                        VideoDocument.file("rear.avi", "uri-rear", 90, 2000),
                        VideoDocument.file("note.txt", "uri-note", 10, 4000)
                    )
                ),
                VideoDocument.folder(
                    displayName = "NORMAL",
                    uriString = "tree-normal",
                    children = listOf(
                        VideoDocument.file("drive.ts", "uri-drive", 80, 1000)
                    )
                )
            )
        )

        val result = VideoScanner().scan(root)

        assertEquals(listOf("front.mp4", "rear.avi", "drive.ts"), result.map { it.displayName })
        assertEquals(listOf("BLACKBOX/EVENT", "BLACKBOX/EVENT", "BLACKBOX/NORMAL"), result.map { it.folderPath })
    }

    @Test
    fun scanSortsRecentVideosFirstAcrossFolders() {
        val root = VideoDocument.folder(
            displayName = "BLACKBOX",
            uriString = "tree-root",
            children = listOf(
                VideoDocument.folder(
                    displayName = "EVENT",
                    uriString = "tree-event",
                    children = listOf(VideoDocument.file("old.mp4", "uri-old", 100, 1000))
                ),
                VideoDocument.folder(
                    displayName = "PARKING",
                    uriString = "tree-parking",
                    children = listOf(VideoDocument.file("new.mp4", "uri-new", 100, 5000))
                )
            )
        )

        val result = VideoScanner().scan(root)

        assertEquals(listOf("new.mp4", "old.mp4"), result.map { it.displayName })
    }
}
```

- [ ] **Step 2: Run tests to verify they fail**

Run:

```powershell
.\gradlew.bat :app:testDebugUnitTest
```

Expected: FAIL because `VideoDocument` and `VideoScanner` do not exist.

- [ ] **Step 3: Add minimal implementation**

Create `app/src/main/java/com/glass/safeclip/data/file/VideoDocument.kt`:

```kotlin
package com.glass.safeclip.data.file

data class VideoDocument(
    val displayName: String,
    val uriString: String,
    val isDirectory: Boolean,
    val sizeBytes: Long?,
    val lastModifiedMillis: Long?,
    val children: List<VideoDocument>
) {
    companion object {
        fun folder(displayName: String, uriString: String, children: List<VideoDocument>): VideoDocument {
            return VideoDocument(
                displayName = displayName,
                uriString = uriString,
                isDirectory = true,
                sizeBytes = null,
                lastModifiedMillis = null,
                children = children
            )
        }

        fun file(displayName: String, uriString: String, sizeBytes: Long?, lastModifiedMillis: Long?): VideoDocument {
            return VideoDocument(
                displayName = displayName,
                uriString = uriString,
                isDirectory = false,
                sizeBytes = sizeBytes,
                lastModifiedMillis = lastModifiedMillis,
                children = emptyList()
            )
        }
    }
}
```

Create `app/src/main/java/com/glass/safeclip/data/file/VideoScanner.kt`:

```kotlin
package com.glass.safeclip.data.file

import com.glass.safeclip.domain.model.VideoCandidate

class VideoScanner {
    fun scan(root: VideoDocument): List<VideoCandidate> {
        val candidates = mutableListOf<VideoCandidate>()
        scanInto(document = root, folderPath = root.displayName, candidates = candidates)
        return VideoCandidateRules.sortRecentFirst(candidates)
    }

    private fun scanInto(
        document: VideoDocument,
        folderPath: String,
        candidates: MutableList<VideoCandidate>
    ) {
        if (document.isDirectory) {
            document.children.forEach { child ->
                val childPath = if (child.isDirectory) "$folderPath/${child.displayName}" else folderPath
                scanInto(child, childPath, candidates)
            }
            return
        }

        if (VideoCandidateRules.isSupportedVideoFile(document.displayName)) {
            candidates += VideoCandidate(
                uriString = document.uriString,
                displayName = document.displayName,
                sizeBytes = document.sizeBytes,
                lastModifiedMillis = document.lastModifiedMillis,
                folderPath = folderPath
            )
        }
    }
}
```

- [ ] **Step 4: Run tests to verify they pass**

Run:

```powershell
.\gradlew.bat :app:testDebugUnitTest
```

Expected: PASS.

- [ ] **Step 5: Review changed files**

Run:

```powershell
Get-ChildItem -LiteralPath .\app\src\main\java\com\glass\safeclip\data\file,.\app\src\test\java\com\glass\safeclip\data\file -Recurse -File | Select-Object FullName
```

Expected: scanner files and test files exist.

---

### Task 3: Android DocumentFile Adapter

**Files:**
- Modify: `gradle/libs.versions.toml`
- Modify: `app/build.gradle.kts`
- Create: `app/src/main/java/com/glass/safeclip/data/file/AndroidDocumentTreeVideoSource.kt`
- Test: `app/src/test/java/com/glass/safeclip/data/file/VideoDocumentAdapterRulesTest.kt`

**Interfaces:**
- Consumes: `VideoDocument.folder(displayName: String, uriString: String, children: List<VideoDocument>): VideoDocument`
- Consumes: `VideoDocument.file(displayName: String, uriString: String, sizeBytes: Long?, lastModifiedMillis: Long?): VideoDocument`
- Produces: `object VideoDocumentAdapterRules`
- Produces: `fun VideoDocumentAdapterRules.safeDisplayName(rawName: String?, fallback: String): String`
- Produces: `class AndroidDocumentTreeVideoSource`
- Produces: `fun AndroidDocumentTreeVideoSource.loadTree(treeUri: android.net.Uri): VideoDocument?`

- [ ] **Step 1: Write the failing test for adapter-safe naming**

Create `app/src/test/java/com/glass/safeclip/data/file/VideoDocumentAdapterRulesTest.kt`:

```kotlin
package com.glass.safeclip.data.file

import org.junit.Assert.assertEquals
import org.junit.Test

class VideoDocumentAdapterRulesTest {
    @Test
    fun safeDisplayNameUsesFallbackWhenNameIsBlank() {
        assertEquals("선택한 폴더", VideoDocumentAdapterRules.safeDisplayName(null, "선택한 폴더"))
        assertEquals("선택한 폴더", VideoDocumentAdapterRules.safeDisplayName("", "선택한 폴더"))
        assertEquals("EVENT", VideoDocumentAdapterRules.safeDisplayName("EVENT", "선택한 폴더"))
    }
}
```

- [ ] **Step 2: Run tests to verify they fail**

Run:

```powershell
.\gradlew.bat :app:testDebugUnitTest
```

Expected: FAIL because `VideoDocumentAdapterRules` does not exist.

- [ ] **Step 3: Add DocumentFile dependency**

Modify `gradle/libs.versions.toml`:

```toml
[versions]
documentfile = "1.0.1"

[libraries]
androidx-documentfile = { group = "androidx.documentfile", name = "documentfile", version.ref = "documentfile" }
```

Keep the existing version and library entries; add only these new entries.

Modify `app/build.gradle.kts` dependencies:

```kotlin
implementation(libs.androidx.documentfile)
```

- [ ] **Step 4: Add adapter rules and Android source**

Create `app/src/main/java/com/glass/safeclip/data/file/AndroidDocumentTreeVideoSource.kt`:

```kotlin
package com.glass.safeclip.data.file

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile

object VideoDocumentAdapterRules {
    fun safeDisplayName(rawName: String?, fallback: String): String {
        return rawName?.takeIf { it.isNotBlank() } ?: fallback
    }
}

class AndroidDocumentTreeVideoSource(private val context: Context) {
    fun loadTree(treeUri: Uri): VideoDocument? {
        val root = DocumentFile.fromTreeUri(context, treeUri) ?: return null
        return root.toVideoDocument(fallbackName = "선택한 폴더")
    }

    private fun DocumentFile.toVideoDocument(fallbackName: String): VideoDocument {
        val name = VideoDocumentAdapterRules.safeDisplayName(this.name, fallbackName)
        return if (isDirectory) {
            VideoDocument.folder(
                displayName = name,
                uriString = uri.toString(),
                children = listFiles().map { child -> child.toVideoDocument(fallbackName = "이름 없는 항목") }
            )
        } else {
            VideoDocument.file(
                displayName = name,
                uriString = uri.toString(),
                sizeBytes = length().takeIf { it >= 0 },
                lastModifiedMillis = lastModified().takeIf { it > 0 }
            )
        }
    }
}
```

- [ ] **Step 5: Run tests and compile**

Run:

```powershell
.\gradlew.bat :app:testDebugUnitTest
.\gradlew.bat :app:assembleDebug
```

Expected: tests PASS and debug build succeeds.

- [ ] **Step 6: Review changed files**

Run:

```powershell
Get-Content -LiteralPath .\gradle\libs.versions.toml
Get-Content -LiteralPath .\app\build.gradle.kts
Get-ChildItem -LiteralPath .\app\src\main\java\com\glass\safeclip\data\file -Recurse -File | Select-Object FullName
```

Expected: dependency and adapter are present.

---

### Task 4: Compose Video List Screen

**Files:**
- Create: `app/src/main/java/com/glass/safeclip/ui/video/VideoListState.kt`
- Create: `app/src/main/java/com/glass/safeclip/ui/video/VideoListScreen.kt`
- Test: `app/src/test/java/com/glass/safeclip/ui/video/VideoListStateTest.kt`

**Interfaces:**
- Consumes: `VideoCandidate`
- Produces: `data class VideoListState(...)`
- Produces: `object VideoListText`
- Produces: `fun VideoListText.fileSizeLabel(sizeBytes: Long?): String`
- Produces: `@Composable fun VideoListScreen(state: VideoListState, onSelectFolder: () -> Unit)`

- [ ] **Step 1: Write the failing state-formatting test**

Create `app/src/test/java/com/glass/safeclip/ui/video/VideoListStateTest.kt`:

```kotlin
package com.glass.safeclip.ui.video

import org.junit.Assert.assertEquals
import org.junit.Test

class VideoListStateTest {
    @Test
    fun fileSizeLabelFormatsUnknownKbAndMb() {
        assertEquals("크기 알 수 없음", VideoListText.fileSizeLabel(null))
        assertEquals("512 KB", VideoListText.fileSizeLabel(512 * 1024))
        assertEquals("2.0 MB", VideoListText.fileSizeLabel(2 * 1024 * 1024))
    }
}
```

- [ ] **Step 2: Run tests to verify they fail**

Run:

```powershell
.\gradlew.bat :app:testDebugUnitTest
```

Expected: FAIL because `VideoListText` does not exist.

- [ ] **Step 3: Add state and screen**

Create `app/src/main/java/com/glass/safeclip/ui/video/VideoListState.kt`:

```kotlin
package com.glass.safeclip.ui.video

import com.glass.safeclip.domain.model.VideoCandidate
import java.util.Locale

data class VideoListState(
    val selectedFolderName: String? = null,
    val isLoading: Boolean = false,
    val videos: List<VideoCandidate> = emptyList(),
    val errorMessage: String? = null
)

object VideoListText {
    fun fileSizeLabel(sizeBytes: Long?): String {
        if (sizeBytes == null) return "크기 알 수 없음"
        val kb = sizeBytes / 1024.0
        if (kb < 1024.0) return "${kb.toInt()} KB"
        val mb = kb / 1024.0
        return String.format(Locale.US, "%.1f MB", mb)
    }
}
```

Create `app/src/main/java/com/glass/safeclip/ui/video/VideoListScreen.kt`:

```kotlin
package com.glass.safeclip.ui.video

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.glass.safeclip.domain.model.VideoCandidate

@Composable
fun VideoListScreen(
    state: VideoListState,
    onSelectFolder: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(text = "SafeClip", style = MaterialTheme.typography.headlineMedium)
        Text(
            text = "블랙박스 SD카드 또는 USB 리더기 폴더를 선택하면 최근 영상 후보를 보여줍니다.",
            style = MaterialTheme.typography.bodyMedium
        )
        Button(onClick = onSelectFolder) {
            Text("SD카드 폴더 선택")
        }
        state.selectedFolderName?.let {
            Text(text = "선택한 폴더: $it", style = MaterialTheme.typography.bodySmall)
        }
        state.errorMessage?.let {
            Text(text = it, color = MaterialTheme.colorScheme.error)
        }
        when {
            state.isLoading -> Text("영상 목록을 불러오는 중입니다.")
            state.videos.isEmpty() -> Text("아직 표시할 영상이 없습니다.")
            else -> VideoCandidateList(videos = state.videos)
        }
    }
}

@Composable
private fun VideoCandidateList(videos: List<VideoCandidate>) {
    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        items(videos) { video ->
            VideoCandidateRow(video = video)
        }
    }
}

@Composable
private fun VideoCandidateRow(video: VideoCandidate) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(text = video.displayName, style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(4.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(text = VideoListText.fileSizeLabel(video.sizeBytes))
                Text(text = video.folderPath)
            }
        }
    }
}
```

- [ ] **Step 4: Run tests to verify they pass**

Run:

```powershell
.\gradlew.bat :app:testDebugUnitTest
```

Expected: PASS.

- [ ] **Step 5: Build debug APK**

Run:

```powershell
.\gradlew.bat :app:assembleDebug
```

Expected: BUILD SUCCESSFUL.

---

### Task 5: Wire Folder Picker In MainActivity

**Files:**
- Modify: `app/src/main/java/com/glass/safeclip/MainActivity.kt`
- Test: `app/src/test/java/com/glass/safeclip/ui/video/FolderPickerResultTextTest.kt`

**Interfaces:**
- Consumes: `AndroidDocumentTreeVideoSource.loadTree(treeUri: Uri): VideoDocument?`
- Consumes: `VideoScanner.scan(root: VideoDocument): List<VideoCandidate>`
- Consumes: `VideoListScreen(state: VideoListState, onSelectFolder: () -> Unit)`
- Produces: `object FolderPickerResultText`
- Produces: `fun FolderPickerResultText.messageForVideoCount(count: Int): String`

- [ ] **Step 1: Write a failing result-text test**

Create `app/src/test/java/com/glass/safeclip/ui/video/FolderPickerResultTextTest.kt`:

```kotlin
package com.glass.safeclip.ui.video

import org.junit.Assert.assertEquals
import org.junit.Test

class FolderPickerResultTextTest {
    @Test
    fun messageForVideoCountExplainsEmptyAndNonEmptyResults() {
        assertEquals("선택한 폴더에서 지원 영상 파일을 찾지 못했습니다.", FolderPickerResultText.messageForVideoCount(0))
        assertEquals("영상 후보 3개를 찾았습니다.", FolderPickerResultText.messageForVideoCount(3))
    }
}
```

- [ ] **Step 2: Run tests to verify they fail**

Run:

```powershell
.\gradlew.bat :app:testDebugUnitTest
```

Expected: FAIL because `FolderPickerResultText` does not exist.

- [ ] **Step 3: Add result text object**

Append this object to `app/src/main/java/com/glass/safeclip/ui/video/VideoListState.kt`:

```kotlin
object FolderPickerResultText {
    fun messageForVideoCount(count: Int): String {
        return if (count == 0) {
            "선택한 폴더에서 지원 영상 파일을 찾지 못했습니다."
        } else {
            "영상 후보 ${count}개를 찾았습니다."
        }
    }
}
```

- [ ] **Step 4: Replace MainActivity template with folder picker flow**

Replace `app/src/main/java/com/glass/safeclip/MainActivity.kt` with:

```kotlin
package com.glass.safeclip

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.glass.safeclip.data.file.AndroidDocumentTreeVideoSource
import com.glass.safeclip.data.file.VideoScanner
import com.glass.safeclip.ui.theme.SafeClipTheme
import com.glass.safeclip.ui.video.FolderPickerResultText
import com.glass.safeclip.ui.video.VideoListScreen
import com.glass.safeclip.ui.video.VideoListState

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val source = AndroidDocumentTreeVideoSource(this)
        val scanner = VideoScanner()

        setContent {
            SafeClipTheme {
                var state by remember { mutableStateOf(VideoListState()) }
                val folderPicker = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.OpenDocumentTree()
                ) { uri: Uri? ->
                    if (uri == null) {
                        state = state.copy(errorMessage = "폴더 선택이 취소되었습니다.")
                        return@rememberLauncherForActivityResult
                    }

                    contentResolver.takePersistableUriPermission(
                        uri,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION
                    )

                    state = state.copy(isLoading = true, errorMessage = null)
                    val root = source.loadTree(uri)
                    val videos = root?.let { scanner.scan(it) }.orEmpty()
                    state = VideoListState(
                        selectedFolderName = root?.displayName ?: "선택한 폴더",
                        isLoading = false,
                        videos = videos,
                        errorMessage = FolderPickerResultText.messageForVideoCount(videos.size)
                            .takeIf { videos.isEmpty() }
                    )
                }

                VideoListScreen(
                    state = state,
                    onSelectFolder = { folderPicker.launch(null) }
                )
            }
        }
    }
}
```

- [ ] **Step 5: Run tests and build**

Run:

```powershell
.\gradlew.bat :app:testDebugUnitTest
.\gradlew.bat :app:assembleDebug
```

Expected: tests PASS and debug build succeeds.

- [ ] **Step 6: Manual Android Studio check**

Run the app on a device or emulator from Android Studio.

Expected:

- App opens with SafeClip title.
- Tapping "SD카드 폴더 선택" opens Android folder picker.
- Choosing a folder returns to the app.
- Supported video files appear in the list.
- Unsupported files do not appear.
- Empty folders show "선택한 폴더에서 지원 영상 파일을 찾지 못했습니다."

---

## Self-Review

**Spec coverage:**

- Folder picker: Task 5.
- SAF / DocumentFile access: Task 3.
- Candidate video filtering: Task 1.
- Recursive folder scanning: Task 2.
- Recent video list UI: Task 4.
- No phone camera or background recording: Global Constraints.

**Known intentional gaps:**

- Video preview is not implemented in this slice.
- Upload and Firebase are not implemented in this slice.
- Consent and submission history are not implemented in this slice.
- Manual testing with a real USB-C reader is still required after build success.

**Placeholder scan:**

- No unfinished marker or placeholder implementation step remains.

**Type consistency:**

- `VideoCandidate`, `VideoDocument`, `VideoScanner`, `VideoListState`, and `VideoListScreen` names are used consistently across tasks.
