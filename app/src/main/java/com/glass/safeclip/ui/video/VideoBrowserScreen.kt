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
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.glass.safeclip.domain.model.VideoCandidate
import com.glass.safeclip.ui.components.GlassPanel
import com.glass.safeclip.ui.components.PrimaryActionButton
import com.glass.safeclip.ui.components.SafeClipScaffold
import com.glass.safeclip.ui.components.SafeClipTopBar
import com.glass.safeclip.ui.components.SecondaryActionButton
import com.glass.safeclip.ui.components.StatusChip
import com.glass.safeclip.ui.components.StatusTone

@Composable
fun VideoBrowserScreen(
    state: VideoListState,
    selectedVideo: VideoCandidate?,
    onSelectVideo: (VideoCandidate) -> Unit,
    onPlayVideo: (VideoCandidate) -> Unit,
    onSubmitVideo: (VideoCandidate) -> Unit,
    onBackHome: () -> Unit
) {
    SafeClipScaffold {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            SafeClipTopBar(
                title = "이벤트 영상 확인",
                subtitle = state.selectedFolderName ?: "블랙박스 폴더를 선택해주세요",
                trailing = {
                    SecondaryActionButton(text = "홈", onClick = onBackHome)
                }
            )

            GlassPanel(modifier = Modifier.fillMaxWidth()) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    StatusChip(label = "전체", tone = StatusTone.Info)
                }
                Text(
                    text = "영상 후보 ${state.videos.size}개",
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
                state.videos.isEmpty() -> EmptyVideoBrowser(onBackHome = onBackHome)
                else -> {
                    VideoCandidateList(
                        videos = state.videos,
                        selectedVideo = selectedVideo,
                        onSelectVideo = onSelectVideo,
                        modifier = Modifier.weight(1f)
                    )
                    SelectedVideoPanel(
                        video = selectedVideo ?: state.videos.first(),
                        onPlayVideo = onPlayVideo,
                        onSubmitVideo = onSubmitVideo
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
private fun VideoCandidateList(
    videos: List<VideoCandidate>,
    selectedVideo: VideoCandidate?,
    onSelectVideo: (VideoCandidate) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(videos) { video ->
            val selected = selectedVideo?.uriString == video.uriString
            GlassPanel(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onSelectVideo(video) }
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    StatusChip(
                        label = if (selected) "선택됨" else "영상",
                        tone = if (selected) StatusTone.Success else StatusTone.Info
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = video.displayName, fontWeight = FontWeight.Bold)
                        Text(
                            text = "${VideoListText.fileSizeLabel(video.sizeBytes)} · ${video.folderPath}",
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
    video: VideoCandidate,
    onPlayVideo: (VideoCandidate) -> Unit,
    onSubmitVideo: (VideoCandidate) -> Unit
) {
    GlassPanel(modifier = Modifier.fillMaxWidth()) {
        Text("선택한 이벤트 미리보기", fontWeight = FontWeight.Bold, fontSize = 18.sp)
        Text(text = video.displayName)
        Text(
            text = "폴더: ${video.folderPath}",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 12.sp
        )
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            SecondaryActionButton(
                text = "영상 재생",
                onClick = { onPlayVideo(video) },
                modifier = Modifier.weight(1f)
            )
            PrimaryActionButton(
                text = "제출하기",
                onClick = { onSubmitVideo(video) },
                modifier = Modifier.weight(1f)
            )
        }
    }
}
