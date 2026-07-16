package com.glass.safeclip.ui.folder

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.glass.safeclip.data.file.ManagedFileOperation
import com.glass.safeclip.data.file.ManagedFolderFile
import com.glass.safeclip.data.file.ManagedFolderVideoCandidate
import com.glass.safeclip.ui.components.GlassPanel
import com.glass.safeclip.ui.components.PrimaryActionButton
import com.glass.safeclip.ui.components.SafeClipScaffold
import com.glass.safeclip.ui.components.SafeClipTopBar
import com.glass.safeclip.ui.components.SecondaryActionButton
import com.glass.safeclip.ui.theme.SafeClipBorder
import com.glass.safeclip.ui.theme.SafeClipCyan
import com.glass.safeclip.ui.video.VideoListText

@Composable
fun FolderManagerScreen(
    kind: FolderViewKind,
    files: List<ManagedFolderFile>,
    isRefreshing: Boolean,
    message: String?,
    onBack: () -> Unit,
    onDelete: (ManagedFolderFile) -> Unit,
    onStartOperation: (ManagedFolderFile, ManagedFileOperation) -> Unit,
    onPlayVideo: (ManagedFolderFile) -> Unit,
    onPreviewImage: (ManagedFolderFile) -> Unit,
    onSubmitVideo: (ManagedFolderFile) -> Unit
) {
    var selectedFilter by remember(kind) { mutableStateOf(FolderFileFilter.All) }
    val visibleFiles = selectedFilter.apply(files)
    var selectedFile by remember(visibleFiles) { mutableStateOf(visibleFiles.firstOrNull()) }

    SafeClipScaffold {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(bottom = 190.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                SafeClipTopBar(
                    subtitle = FolderManagerText.title(kind),
                    trailing = { SecondaryActionButton(text = "뒤로", onClick = onBack) }
                )

                GlassPanel(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = FolderManagerText.title(kind),
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp
                    )
                    Text(
                        text = "파일을 선택하면 삭제, 복사, 이동을 실행할 수 있습니다.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 20.sp
                    )
                    if (isRefreshing) {
                        Text(
                            text = FolderManagerText.loadingMessage(),
                            color = SafeClipCyan
                        )
                    } else if (visibleFiles.isEmpty()) {
                        Text(
                            text = FolderManagerText.emptyMessage(kind),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                FolderFilterTabs(
                    selectedFilter = selectedFilter,
                    onFilterSelected = { selectedFilter = it }
                )

                visibleFiles.forEach { file ->
                    ManagedFileRow(
                        file = file,
                        selected = file == selectedFile,
                        onClick = { selectedFile = file }
                    )
                }

                message?.let {
                    Text(text = it, color = SafeClipCyan)
                }
            }

            selectedFile?.let { file ->
                SelectedFileActionPanel(
                    file = file,
                    onDelete = onDelete,
                    onStartOperation = onStartOperation,
                    onPlayVideo = onPlayVideo,
                    onPreviewImage = onPreviewImage,
                    onSubmitVideo = onSubmitVideo,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                )
            }
        }
    }
}

@Composable
private fun FolderFilterTabs(
    selectedFilter: FolderFileFilter,
    onFilterSelected: (FolderFileFilter) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        FolderFileFilter.values().forEach { filter ->
            val selected = filter == selectedFilter
            Surface(
                modifier = Modifier
                    .weight(1f)
                    .clickable { onFilterSelected(filter) },
                shape = RoundedCornerShape(8.dp),
                color = if (selected) SafeClipCyan.copy(alpha = 0.18f) else MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, if (selected) SafeClipCyan else SafeClipBorder)
            ) {
                Text(
                    text = filter.label,
                    modifier = Modifier.padding(vertical = 10.dp),
                    color = if (selected) SafeClipCyan else MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        }
    }
}

@Composable
private fun SelectedFileActionPanel(
    file: ManagedFolderFile,
    onDelete: (ManagedFolderFile) -> Unit,
    onStartOperation: (ManagedFolderFile, ManagedFileOperation) -> Unit,
    onPlayVideo: (ManagedFolderFile) -> Unit,
    onPreviewImage: (ManagedFolderFile) -> Unit,
    onSubmitVideo: (ManagedFolderFile) -> Unit,
    modifier: Modifier = Modifier
) {
    val videoActionsEnabled = ManagedFolderVideoCandidate.canUseVideoActions(file)
    val imagePreviewEnabled = ManagedFolderVideoCandidate.canPreviewImage(file)
    val previewEnabled = videoActionsEnabled || imagePreviewEnabled
    val submitEnabled = ManagedFolderVideoCandidate.canSubmitFile(file)

    GlassPanel(modifier = modifier) {
        Text(text = "선택됨 : ${file.displayName}", fontWeight = FontWeight.SemiBold)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            SecondaryActionButton(text = "삭제", onClick = { onDelete(file) }, modifier = Modifier.weight(1f))
            SecondaryActionButton(
                text = "복사",
                onClick = { onStartOperation(file, ManagedFileOperation.Copy) },
                modifier = Modifier.weight(1f)
            )
            PrimaryActionButton(
                text = "이동",
                onClick = { onStartOperation(file, ManagedFileOperation.Move) },
                modifier = Modifier.weight(1f)
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            SecondaryActionButton(
                text = ManagedFolderVideoCandidate.primaryPreviewActionText(file),
                onClick = {
                    if (imagePreviewEnabled) {
                        onPreviewImage(file)
                    } else {
                        onPlayVideo(file)
                    }
                },
                enabled = previewEnabled,
                modifier = Modifier.weight(1f)
            )
            PrimaryActionButton(
                text = "제출하기",
                onClick = { onSubmitVideo(file) },
                enabled = submitEnabled,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun ManagedFileRow(
    file: ManagedFolderFile,
    selected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, if (selected) SafeClipCyan else SafeClipBorder)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = file.displayName,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.fillMaxWidth()
            )
            Text(
                text = VideoListText.fileSizeLabel(file.sizeBytes),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 12.sp
            )
        }
    }
}
