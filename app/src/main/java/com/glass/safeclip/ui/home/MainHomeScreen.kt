package com.glass.safeclip.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.glass.safeclip.data.file.ManagedFolderFile
import com.glass.safeclip.data.file.ManagedFolderFileSummary
import com.glass.safeclip.ui.components.GlassPanel
import com.glass.safeclip.ui.components.PrimaryActionButton
import com.glass.safeclip.ui.components.SafeClipScaffold
import com.glass.safeclip.ui.components.SafeClipTopBar
import com.glass.safeclip.ui.components.SecondaryActionButton
import com.glass.safeclip.ui.folder.FolderViewKind
import com.glass.safeclip.ui.theme.SafeClipError
import com.glass.safeclip.ui.theme.SafeClipSuccess
import com.glass.safeclip.ui.video.VideoListState

@Composable
fun MainHomeScreen(
    state: VideoListState,
    eventFolderFiles: List<ManagedFolderFile>,
    currentFolderFiles: List<ManagedFolderFile>,
    folderPermissionGranted: Boolean,
    cameraPermissionGranted: Boolean,
    submissionCount: Int,
    onLoadVideos: () -> Unit,
    onRequestCameraPermission: () -> Unit,
    onOpenRecentEvents: () -> Unit,
    onOpenFolder: (FolderViewKind) -> Unit,
    onOpenStatus: () -> Unit,
    onOpenSettings: () -> Unit,
    onBack: () -> Unit
) {
    val importActions = HomeImportActions.from(
        selectedFolderName = state.selectedFolderName,
        folderPermissionGranted = folderPermissionGranted,
        cameraPermissionGranted = cameraPermissionGranted
    )
    val eventFolderSummary = ManagedFolderFileSummary.from(eventFolderFiles)
    val currentFolderSummary = ManagedFolderFileSummary.from(currentFolderFiles)
    val statusSummary = HomeStatusSummary.from(
        savedEventVideoCount = eventFolderSummary.videoCount,
        savedEventPhotoCount = eventFolderSummary.photoCount,
        currentFolderVideoCount = currentFolderSummary.videoCount,
        currentFolderPhotoCount = currentFolderSummary.photoCount,
        submissionCount = submissionCount
    )

    SafeClipScaffold {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            SafeClipTopBar(
                subtitle = "블랙박스 이벤트 영상 관리",
                trailing = {
                    SecondaryActionButton(text = "뒤로", onClick = onBack)
                }
            )

            GlassPanel(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "블랙박스 폴더를 먼저 선택하세요",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    lineHeight = 30.sp
                )
                Text(
                    text = importActions.selectedFolderText
                        ?: "USB-C 리더기 또는 microSD 카드의 이벤트 영상 폴더를 선택하면 최근 영상을 확인할 수 있습니다.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 22.sp
                )
                HomeActionButton(
                    text = importActions.folderButtonText,
                    isPrimary = importActions.folderButtonIsPrimary,
                    onClick = onLoadVideos,
                    modifier = Modifier.fillMaxWidth()
                )
                HomeActionButton(
                    text = importActions.recentButtonText,
                    isPrimary = importActions.recentButtonIsPrimary,
                    onClick = onOpenRecentEvents,
                    enabled = importActions.recentButtonEnabled,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Row(
                modifier = Modifier.height(IntrinsicSize.Min),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                FolderStatusTile(
                    label = "이벤트 폴더",
                    value = statusSummary.savedEventCountText,
                    enabled = importActions.eventFolderEnabled,
                    onOpenFolder = { onOpenFolder(FolderViewKind.SafeClipSaved) },
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                )
                FolderStatusTile(
                    label = "현재 폴더",
                    value = statusSummary.currentFolderCountText,
                    enabled = folderPermissionGranted,
                    onOpenFolder = { onOpenFolder(FolderViewKind.CurrentFolder) },
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                HomeShortcut("제출 내역", statusSummary.submissionCountText, Modifier.weight(1f), onClick = onOpenStatus)
                HomeShortcut("설정", "환경 관리", Modifier.weight(1f), onClick = onOpenSettings)
            }

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                PermissionStatusTile(
                    label = "폴더 권한",
                    granted = folderPermissionGranted,
                    actionText = "폴더 선택",
                    onAction = onLoadVideos,
                    modifier = Modifier.weight(1f)
                )
                PermissionStatusTile(
                    label = "카메라 권한",
                    granted = cameraPermissionGranted,
                    actionText = "권한 요청",
                    onAction = onRequestCameraPermission,
                    modifier = Modifier.weight(1f)
                )
            }

            state.errorMessage?.let {
                Text(text = it, color = MaterialTheme.colorScheme.error)
            }
        }
    }
}

@Composable
private fun PermissionStatusTile(
    label: String,
    granted: Boolean,
    actionText: String,
    onAction: () -> Unit,
    modifier: Modifier = Modifier
) {
    GlassPanel(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = label,
                fontWeight = FontWeight.Bold,
                fontSize = HomeTileTextStyle.TITLE_FONT_SIZE_SP.sp
            )
            Box(
                modifier = Modifier
                    .size(11.dp)
                    .background(if (granted) SafeClipSuccess else SafeClipError, CircleShape)
            )
        }
        Text(
            text = if (granted) "ON" else "OFF",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = HomeTileTextStyle.SUBTITLE_FONT_SIZE_SP.sp
        )
        SecondaryActionButton(text = actionText, onClick = onAction, enabled = !granted)
    }
}

@Composable
private fun FolderStatusTile(
    label: String,
    value: String,
    enabled: Boolean,
    onOpenFolder: () -> Unit,
    modifier: Modifier = Modifier
) {
    GlassPanel(modifier = modifier) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                fontWeight = FontWeight.Bold,
                fontSize = HomeTileTextStyle.TITLE_FONT_SIZE_SP.sp
            )
            Text(
                text = value,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = HomeTileTextStyle.SUBTITLE_FONT_SIZE_SP.sp,
                lineHeight = (HomeTileTextStyle.SUBTITLE_FONT_SIZE_SP + 5).sp
            )
        }
        Spacer(modifier = Modifier.weight(0.01f))
        SecondaryActionButton(text = "폴더 보기", onClick = onOpenFolder, enabled = enabled)
    }
}

@Composable
private fun HomeActionButton(
    text: String,
    isPrimary: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    if (isPrimary) {
        PrimaryActionButton(text = text, onClick = onClick, modifier = modifier, enabled = enabled)
    } else {
        SecondaryActionButton(text = text, onClick = onClick, modifier = modifier, enabled = enabled)
    }
}

@Composable
private fun HomeShortcut(
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {}
) {
    GlassPanel(modifier = modifier) {
        Text(text = title, fontWeight = FontWeight.Bold, fontSize = HomeTileTextStyle.TITLE_FONT_SIZE_SP.sp)
        Text(text = subtitle, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = HomeTileTextStyle.SUBTITLE_FONT_SIZE_SP.sp)
        SecondaryActionButton(text = "열기", onClick = onClick)
    }
}
