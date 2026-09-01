package com.glass.safeclip.ui.video

import android.net.Uri
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.annotation.OptIn
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.glass.safeclip.data.media.PlaybackSpeedOptions
import com.glass.safeclip.data.media.EventClipFileName
import com.glass.safeclip.data.media.SafeClipMediaSaveLocation
import com.glass.safeclip.data.media.VideoClipExportResult
import com.glass.safeclip.data.media.VideoClipSelection
import com.glass.safeclip.data.media.VideoClipText
import com.glass.safeclip.domain.model.VideoCandidate
import com.glass.safeclip.ui.components.GlassPanel
import com.glass.safeclip.ui.components.PrimaryActionButton
import com.glass.safeclip.ui.components.SafeClipScaffold
import com.glass.safeclip.ui.components.SafeClipTopBar
import com.glass.safeclip.ui.components.SecondaryActionButton
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

object VideoPreviewText {
    fun captureSuccessMessage(path: String): String {
        return "캡쳐 저장 완료: $path"
    }

    fun captureFailureMessage(reason: String?): String {
        return if (reason.isNullOrBlank()) {
            "현재 장면을 캡쳐하지 못했습니다."
        } else {
            "현재 장면을 캡쳐하지 못했습니다. $reason"
        }
    }

    fun clipExportSuccessMessage(path: String): String {
        return "클립 저장 완료: $path"
    }

    fun clipExportFailureMessage(reason: String?): String {
        return if (reason.isNullOrBlank()) {
            "클립을 저장하지 못했습니다."
        } else {
            "클립을 저장하지 못했습니다. $reason"
        }
    }

    fun mutedCopySuccessMessage(path: String): String {
        return "음성 제거 사본 저장 완료: $path"
    }

    fun mutedCopyFailureMessage(reason: String?): String {
        return if (reason.isNullOrBlank()) {
            "음성 제거 사본을 만들지 못했습니다."
        } else {
            "음성 제거 사본을 만들지 못했습니다. $reason"
        }
    }
}

@OptIn(UnstableApi::class)
@Composable
fun VideoPreviewScreen(
    video: VideoCandidate,
    onBack: () -> Unit,
    onCaptureFrame: suspend (Uri, String, Long) -> Result<File>,
    onExportClip: suspend (VideoCandidate, VideoClipSelection) -> Result<VideoClipExportResult>,
    canRemoveAudio: Boolean,
    onRemoveAudio: suspend (Uri, String) -> Result<Uri>,
    onSubmit: (VideoCandidate, VideoClipExportResult?) -> Unit
) {
    val context = LocalContext.current
    val videoUri = remember(video.uriString) { Uri.parse(video.uriString) }
    val player = remember {
        ExoPlayer.Builder(context).build()
    }
    val scope = rememberCoroutineScope()
    var selectedSpeed by remember { mutableFloatStateOf(1.0f) }
    var clipSelection by remember(video.uriString) { mutableStateOf(VideoClipSelection()) }
    var savedClip by remember(video.uriString) { mutableStateOf<VideoClipExportResult?>(null) }
    var statusMessage by remember { mutableStateOf<String?>(null) }
    var isExporting by remember { mutableStateOf(false) }
    var isRemovingAudio by remember { mutableStateOf(false) }

    LaunchedEffect(videoUri) {
        player.setMediaItem(MediaItem.fromUri(videoUri))
        player.prepare()
    }

    DisposableEffect(Unit) {
        onDispose {
            player.release()
        }
    }

    SafeClipScaffold(contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 6.dp)) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(7.dp)
        ) {
            SafeClipTopBar(
                title = "영상 확인 및 편집",
                subtitle = video.displayName,
                trailing = {
                    SecondaryActionButton(
                        text = "목록",
                        onClick = onBack,
                        modifier = Modifier
                    )
                }
            )

            AndroidView(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                factory = { viewContext ->
                    PlayerView(viewContext).apply {
                        this.player = player
                        layoutParams = FrameLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )
                    }
                },
                update = { playerView ->
                    playerView.player = player
                }
            )

            GlassPanel(modifier = Modifier.fillMaxWidth()) {
                Text("재생 속도", style = MaterialTheme.typography.labelSmall)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    PlaybackSpeedOptions.supported.forEach { option ->
                        val selected = selectedSpeed == option.speed
                        CompactControlChip(
                            text = option.label,
                            selected = selected,
                            onClick = {
                                selectedSpeed = option.speed
                                player.setPlaybackSpeed(option.speed)
                            }
                        )
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    CompactControlChip(
                        text = "현재 장면 캡쳐",
                        selected = false,
                        onClick = {
                            val positionMs = player.currentPosition
                            scope.launch {
                                statusMessage = "캡쳐 중입니다."
                                val result = withContext(Dispatchers.IO) {
                                    onCaptureFrame(videoUri, video.displayName, positionMs)
                                }
                                statusMessage = result.fold(
                                    onSuccess = { VideoPreviewText.captureSuccessMessage(it.absolutePath) },
                                    onFailure = { VideoPreviewText.captureFailureMessage(it.message) }
                                )
                            }
                        }
                    )
                    CompactControlChip(
                        text = "시작 지정",
                        selected = false,
                        enabled = !isExporting,
                        onClick = {
                            clipSelection = clipSelection.withStart(player.currentPosition)
                            statusMessage = null
                        }
                    )
                    CompactControlChip(
                        text = "끝 지정",
                        selected = false,
                        enabled = !isExporting,
                        onClick = {
                            clipSelection = clipSelection.withEnd(player.currentPosition)
                            statusMessage = null
                        }
                    )
                    CompactControlChip(
                        text = "클립 저장",
                        selected = false,
                        enabled = clipSelection.isComplete && !isExporting,
                        onClick = {
                            scope.launch {
                                isExporting = true
                                statusMessage = "클립 저장 중입니다. 영상이 크면 조금 걸릴 수 있습니다."
                                val result = onExportClip(video, clipSelection)
                                statusMessage = result.fold(
                                    onSuccess = {
                                        savedClip = it
                                        VideoPreviewText.clipExportSuccessMessage(it.savedDisplayPath)
                                    },
                                    onFailure = { VideoPreviewText.clipExportFailureMessage(it.message) }
                                )
                                isExporting = false
                            }
                        }
                    )
                }

                if (canRemoveAudio && !video.displayName.endsWith("_MUTED.mp4", ignoreCase = true)) {
                    CompactControlChip(
                        text = if (isRemovingAudio) "음성 제거 중" else "음성 제거 사본 만들기",
                        selected = false,
                        enabled = !isExporting && !isRemovingAudio,
                        onClick = {
                            scope.launch {
                                isRemovingAudio = true
                                statusMessage = "음성 제거 사본을 만드는 중입니다."
                                val result = onRemoveAudio(videoUri, video.displayName)
                                statusMessage = result.fold(
                                    onSuccess = {
                                        VideoPreviewText.mutedCopySuccessMessage(
                                            SafeClipMediaSaveLocation.displayPath(
                                                EventClipFileName.muted(video.displayName)
                                            )
                                        )
                                    },
                                    onFailure = { VideoPreviewText.mutedCopyFailureMessage(it.message) }
                                )
                                isRemovingAudio = false
                            }
                        }
                    )
                }

                Text(text = VideoClipText.rangeLabel(clipSelection), style = MaterialTheme.typography.labelSmall)
                savedClip?.let {
                    Text(text = "제출용 클립 준비됨", color = MaterialTheme.colorScheme.secondary, fontSize = 12.sp)
                }
                statusMessage?.let {
                    Text(text = it, style = MaterialTheme.typography.labelSmall)
                }
                PrimaryActionButton(
                    text = "제출하기",
                    onClick = { onSubmit(video, savedClip) }
                )
            }
        }
    }
}

@Composable
private fun CompactControlChip(
    text: String,
    selected: Boolean,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    Surface(
        modifier = Modifier
            .alpha(if (enabled) 1f else 0.45f)
            .clickable(enabled = enabled && !selected, onClick = onClick),
        shape = RoundedCornerShape(6.dp),
        color = if (selected) colorScheme.primaryContainer else colorScheme.surface,
        border = BorderStroke(1.dp, if (selected) colorScheme.primary else colorScheme.outline)
    ) {
        Box(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
        ) {
            Text(
                text = text,
                fontSize = 10.sp,
                lineHeight = 11.sp,
                color = if (selected) colorScheme.onPrimaryContainer else colorScheme.onSurface
            )
        }
    }
}
