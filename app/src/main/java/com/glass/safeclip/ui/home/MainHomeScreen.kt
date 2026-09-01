package com.glass.safeclip.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.AccountCircle
import androidx.compose.material.icons.rounded.Settings
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
import com.glass.safeclip.ui.components.TopBarIconButton
import com.glass.safeclip.ui.folder.FolderViewKind
import com.glass.safeclip.ui.video.VideoListState

@Composable
fun MainHomeScreen(
    state: VideoListState,
    eventFolderFiles: List<ManagedFolderFile>,
    currentFolderFiles: List<ManagedFolderFile>,
    folderPermissionGranted: Boolean,
    cameraPermissionGranted: Boolean,
    mediaLibraryPermissionGranted: Boolean,
    submissionCount: Int,
    askAnswerCount: Int,
    onOpenFolderPermissionSettings: () -> Unit,
    onRequestMediaLibraryPermission: () -> Unit,
    onOpenRecentEvents: () -> Unit,
    onOpenLiveRecording: () -> Unit,
    onOpenFolder: (FolderViewKind) -> Unit,
    onOpenStatus: () -> Unit,
    onOpenMyPage: () -> Unit,
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
    val folderTileActions = HomeFolderTileActions.from(
        folderPermissionGranted = folderPermissionGranted,
        safeClipVideoCount = eventFolderSummary.videoCount,
        safeClipPhotoCount = eventFolderSummary.photoCount
    )
    val statusSummary = HomeStatusSummary.from(
        savedMediaPermissionGranted = mediaLibraryPermissionGranted,
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
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TopBarIconButton(
                            icon = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = "뒤로가기",
                            onClick = onBack
                        )
                        TopBarIconButton(
                            icon = Icons.Rounded.AccountCircle,
                            contentDescription = "마이페이지",
                            onClick = onOpenMyPage
                        )
                        TopBarIconButton(
                            icon = Icons.Rounded.Settings,
                            contentDescription = "설정",
                            onClick = onOpenSettings
                        )
                    }
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
                        ?: "USB-C 리더기 또는 microSD 카드의 블랙박스 폴더를 선택하면 영상과 사진을 확인할 수 있습니다.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 22.sp
                )
                HomeActionButton(
                    text = importActions.folderButtonText,
                    isPrimary = importActions.folderButtonIsPrimary,
                    onClick = onOpenFolderPermissionSettings,
                    modifier = Modifier.fillMaxWidth()
                )
                if (importActions.showRecentButton) {
                    HomeActionButton(
                        text = importActions.recentButtonText,
                        isPrimary = importActions.recentButtonIsPrimary,
                        onClick = onOpenRecentEvents,
                        enabled = importActions.recentButtonEnabled,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            Row(
                modifier = Modifier.height(IntrinsicSize.Min),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                FolderStatusTile(
                    label = "SafeClip 폴더",
                    value = statusSummary.savedEventCountText,
                    enabled = true,
                    isPrimary = !mediaLibraryPermissionGranted || folderTileActions.safeClipFolderViewIsPrimary,
                    actionText = if (mediaLibraryPermissionGranted) "폴더 보기" else "권한 추가",
                    onOpenFolder = {
                        if (mediaLibraryPermissionGranted) {
                            onOpenFolder(FolderViewKind.SafeClipSaved)
                        } else {
                            onRequestMediaLibraryPermission()
                        }
                    },
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                )
                FolderStatusTile(
                    label = "블랙박스 폴더",
                    value = statusSummary.currentFolderCountText,
                    enabled = folderPermissionGranted,
                    isPrimary = folderTileActions.blackboxFolderViewIsPrimary,
                    onOpenFolder = { onOpenFolder(FolderViewKind.CurrentFolder) },
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                HomeShortcut("제출 내역", statusSummary.submissionCountText, Modifier.weight(1f), onClick = onOpenStatus)
                HomeShortcut(
                    title = "마이페이지",
                    subtitle = if (askAnswerCount > 0) "문의 답변 ${askAnswerCount}개" else "계정 및 문의",
                    modifier = Modifier.weight(1f),
                    onClick = onOpenMyPage
                )
            }

            PrimaryActionButton(
                text = "실시간 녹화",
                onClick = onOpenLiveRecording,
                modifier = Modifier.fillMaxWidth()
            )

            state.errorMessage?.let {
                Text(text = it, color = MaterialTheme.colorScheme.error)
            }
        }
    }
}

@Composable
private fun FolderStatusTile(
    label: String,
    value: String,
    enabled: Boolean,
    isPrimary: Boolean,
    actionText: String = "폴더 보기",
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
        HomeActionButton(
            text = actionText,
            isPrimary = isPrimary,
            onClick = onOpenFolder,
            enabled = enabled
        )
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
