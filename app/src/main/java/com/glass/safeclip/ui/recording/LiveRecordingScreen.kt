package com.glass.safeclip.ui.recording

import androidx.camera.core.Preview
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.glass.safeclip.data.recording.RecordingQuality
import java.util.Locale

@Composable
fun LiveRecordingScreen(
    state: LiveRecordingUiState,
    onStart: () -> Unit,
    onStop: () -> Unit,
    onEvent: () -> Unit,
    onQualityChange: (RecordingQuality) -> Unit,
    onAudioChange: (Boolean) -> Unit,
    onRemoteTestChange: (Boolean) -> Unit,
    onBack: () -> Unit,
    onPreviewReady: (Preview.SurfaceProvider) -> Unit
) {
    val recording = state.phase == RecordingPhase.Recording
    LiveRecordingWindowEffect(recording)

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        AndroidView(
            factory = { context ->
                PreviewView(context).apply {
                    implementationMode = PreviewView.ImplementationMode.COMPATIBLE
                    scaleType = PreviewView.ScaleType.FILL_CENTER
                    onPreviewReady(surfaceProvider)
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .windowInsetsPadding(
                    WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal + WindowInsetsSides.Top)
                )
                .background(Color(0xCC001426))
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(onClick = onBack, enabled = state.phase != RecordingPhase.Stopping) {
                Text("뒤로")
            }
            Text(
                text = when (state.phase) {
                    RecordingPhase.Recording -> "녹화 중"
                    RecordingPhase.Preparing -> "준비 중"
                    RecordingPhase.Stopping -> "종료 중"
                    RecordingPhase.Error -> "오류"
                    RecordingPhase.Ready -> "녹화 준비"
                },
                color = Color.White,
                style = MaterialTheme.typography.titleMedium
            )
            Text(formatElapsed(state.elapsedMs), color = Color.White)
            Text("남은 공간 ${formatBytes(state.freeBytes)}", color = Color(0xFFB7D5EA))
            state.message?.let { Text(it, color = Color(0xFFFFB4AB)) }
        }

        Column(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .windowInsetsPadding(
                    WindowInsets.safeDrawing.only(WindowInsetsSides.End + WindowInsetsSides.Vertical)
                )
                .fillMaxHeight()
                .width(170.dp)
                .background(Color(0xD9001426))
                .verticalScroll(rememberScrollState())
                .padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                state.supportedQualities.forEachIndexed { index, quality ->
                    SegmentedButton(
                        selected = state.selectedQuality == quality,
                        onClick = { onQualityChange(quality) },
                        enabled = state.phase == RecordingPhase.Ready,
                        shape = SegmentedButtonDefaults.itemShape(
                            index = index,
                            count = state.supportedQualities.size
                        ),
                        icon = {},
                        contentPadding = PaddingValues(horizontal = 2.dp),
                        label = { Text(quality.name) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("녹음", color = Color.White, modifier = Modifier.weight(1f))
                Switch(
                    checked = state.audioEnabled,
                    onCheckedChange = onAudioChange,
                    enabled = state.phase == RecordingPhase.Ready
                )
            }
            if (state.phase == RecordingPhase.Ready) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("리모컨 테스트", color = Color.White, modifier = Modifier.weight(1f))
                    Switch(checked = state.remoteTestEnabled, onCheckedChange = onRemoteTestChange)
                }
                state.lastRemoteKeyLabel?.let { Text(it, color = Color(0xFF50D5FF)) }
            }
            Button(
                onClick = if (recording) onStop else onStart,
                enabled = state.phase == RecordingPhase.Ready || recording,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (recording) "녹화 중지" else "녹화 시작")
            }
            Button(
                onClick = onEvent,
                enabled = recording,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("이벤트 저장")
            }
            state.events.takeLast(4).forEach { event ->
                Text(event.message, color = if (event.status == EventWorkStatus.Failed) Color(0xFFFFB4AB) else Color.White)
            }
        }
    }
}

private fun formatElapsed(elapsedMs: Long): String {
    val seconds = elapsedMs / 1_000
    return String.format(Locale.US, "%02d:%02d:%02d", seconds / 3_600, (seconds % 3_600) / 60, seconds % 60)
}

private fun formatBytes(bytes: Long): String {
    val gigabytes = bytes.toDouble() / (1_024 * 1_024 * 1_024)
    return String.format(Locale.US, "%.1f GB", gigabytes)
}
