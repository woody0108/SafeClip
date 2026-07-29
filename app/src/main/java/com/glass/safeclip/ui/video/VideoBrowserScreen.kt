package com.glass.safeclip.ui.video

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.glass.safeclip.data.file.ManagedFolderFile
import com.glass.safeclip.data.file.ManagedFolderVideoCandidate
import com.glass.safeclip.ui.components.GlassPanel
import com.glass.safeclip.ui.components.PrimaryActionButton
import com.glass.safeclip.ui.components.SafeClipScaffold
import com.glass.safeclip.ui.components.SafeClipTopBar
import com.glass.safeclip.ui.components.SecondaryActionButton
import com.glass.safeclip.ui.components.SegmentedTabButton
import com.glass.safeclip.ui.components.StatusChip
import com.glass.safeclip.ui.components.StatusTone
import com.glass.safeclip.ui.folder.FolderFileFilter

@Composable
fun VideoBrowserScreen(
    state: VideoListState,
    blackboxFiles: List<ManagedFolderFile>,
    safeClipFiles: List<ManagedFolderFile>,
    initialSource: VideoBrowserSource,
    onPlayVideo: (ManagedFolderFile, VideoBrowserSource) -> Unit,
    onPreviewImage: (ManagedFolderFile, VideoBrowserSource) -> Unit,
    onSubmitFile: (ManagedFolderFile, VideoBrowserSource) -> Unit,
    onBackHome: () -> Unit
) {
    var selectedSource by remember(initialSource) { mutableStateOf(initialSource) }
    var selectedFilter by remember { mutableStateOf(FolderFileFilter.All) }
    val blackboxDisplayFiles = blackboxFiles.ifEmpty {
        state.videos.map { video ->
            ManagedFolderFile(
                uriString = video.uriString,
                displayName = video.displayName,
                mimeType = "video/mp4",
                sizeBytes = video.sizeBytes
            )
        }
    }
    val displayFiles = selectedSource.select(
        blackboxFiles = blackboxDisplayFiles,
        safeClipFiles = safeClipFiles
    )
    val visibleFiles = selectedFilter.apply(displayFiles)
    var selectedFile by remember(visibleFiles) { mutableStateOf(visibleFiles.firstOrNull()) }

    SafeClipScaffold {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            SafeClipTopBar(
                title = "파일 확인",
                subtitle = when (selectedSource) {
                    VideoBrowserSource.All -> "블랙박스 폴더 + SafeClip 폴더"
                    VideoBrowserSource.Blackbox -> state.selectedFolderName ?: "블랙박스 폴더"
                    VideoBrowserSource.SafeClip -> "SafeClip 폴더"
                },
                trailing = {
                    SecondaryActionButton(text = "홈", onClick = onBackHome)
                }
            )

            GlassPanel(modifier = Modifier.fillMaxWidth()) {
                VideoSourceTabs(
                    selectedSource = selectedSource,
                    onSourceSelected = {
                        selectedSource = it
                        selectedFilter = FolderFileFilter.All
                    }
                )
                Text(
                    text = "파일 후보 ${visibleFiles.size}개",
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp
                )
                if (state.isLoading) {
                    Text("영상 목록을 불러오는 중입니다.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                state.errorMessage?.let {
                    Text(text = it, color = MaterialTheme.colorScheme.error)
                }
            }

            when {
                state.isLoading -> Text("영상 목록을 불러오는 중입니다.")
                visibleFiles.isEmpty() -> EmptyVideoBrowser(onBackHome = onBackHome)
                else -> {
                    VideoFileFilterTabs(
                        selectedFilter = selectedFilter,
                        onFilterSelected = { selectedFilter = it }
                    )
                    VideoCandidateList(
                        files = visibleFiles,
                        selectedFile = selectedFile,
                        onSelectFile = { selectedFile = it },
                        modifier = Modifier.weight(1f)
                    )
                    SelectedVideoPanel(
                        file = selectedFile ?: visibleFiles.first(),
                        onPlayVideo = { onPlayVideo(it, selectedSource) },
                        onPreviewImage = { onPreviewImage(it, selectedSource) },
                        onSubmitFile = { onSubmitFile(it, selectedSource) }
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyVideoBrowser(
    onBackHome: () -> Unit
) {
    GlassPanel(modifier = Modifier.fillMaxWidth()) {
        Text("아직 표시할 영상이 없습니다.", fontWeight = FontWeight.Bold)
        Text(
            text = "다른 블랙박스 폴더를 선택하거나 microSD 연결 상태를 확인해주세요.",
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        SecondaryActionButton(text = "홈으로", onClick = onBackHome)
    }
}

@Composable
private fun VideoFileFilterTabs(
    selectedFilter: FolderFileFilter,
    onFilterSelected: (FolderFileFilter) -> Unit
) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        FolderFileFilter.values().forEach { filter ->
            SegmentedTabButton(
                text = filter.label,
                onClick = { onFilterSelected(filter) },
                modifier = Modifier.weight(1f),
                selected = filter == selectedFilter
            )
        }
    }
}

@Composable
private fun VideoSourceTabs(
    selectedSource: VideoBrowserSource,
    onSourceSelected: (VideoBrowserSource) -> Unit
) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        VideoBrowserSource.values().forEach { source ->
            SegmentedTabButton(
                text = source.label,
                onClick = { onSourceSelected(source) },
                modifier = Modifier.weight(1f),
                selected = source == selectedSource
            )
        }
    }
}

@Composable
private fun VideoCandidateList(
    files: List<ManagedFolderFile>,
    selectedFile: ManagedFolderFile?,
    onSelectFile: (ManagedFolderFile) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(files) { file ->
            val selected = selectedFile?.uriString == file.uriString
            GlassPanel(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onSelectFile(file) }
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    StatusChip(
                        label = if (selected) "선택됨" else if (ManagedFolderVideoCandidate.canPreviewImage(file)) "사진" else "영상",
                        tone = if (selected) StatusTone.Success else StatusTone.Info
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = file.displayName, fontWeight = FontWeight.Bold)
                        Text(
                            text = VideoListText.fileSizeLabel(file.sizeBytes),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SelectedVideoPanel(
    file: ManagedFolderFile,
    onPlayVideo: (ManagedFolderFile) -> Unit,
    onPreviewImage: (ManagedFolderFile) -> Unit,
    onSubmitFile: (ManagedFolderFile) -> Unit
) {
    val canPreviewImage = ManagedFolderVideoCandidate.canPreviewImage(file)
    val canPlayVideo = ManagedFolderVideoCandidate.canUseVideoActions(file)
    GlassPanel(modifier = Modifier.fillMaxWidth()) {
        Text("선택한 이벤트 미리보기", fontWeight = FontWeight.Bold, fontSize = 18.sp)
        Text(text = file.displayName)
        Text(
            text = VideoListText.fileSizeLabel(file.sizeBytes),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 12.sp
        )
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            SecondaryActionButton(
                text = if (canPreviewImage) "사진 미리보기" else "영상 재생",
                onClick = {
                    if (canPreviewImage) onPreviewImage(file) else onPlayVideo(file)
                },
                enabled = canPreviewImage || canPlayVideo,
                modifier = Modifier.weight(1f)
            )
            PrimaryActionButton(
                text = "제출하기",
                onClick = { onSubmitFile(file) },
                enabled = ManagedFolderVideoCandidate.canSubmitFile(file),
                modifier = Modifier.weight(1f)
            )
        }
    }
}
