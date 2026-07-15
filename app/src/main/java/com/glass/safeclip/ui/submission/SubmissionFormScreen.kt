package com.glass.safeclip.ui.submission

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import com.glass.safeclip.data.media.VideoClipExportResult
import com.glass.safeclip.domain.model.VideoCandidate
import com.glass.safeclip.ui.components.GlassPanel
import com.glass.safeclip.ui.components.PrimaryActionButton
import com.glass.safeclip.ui.components.SafeClipScaffold
import com.glass.safeclip.ui.components.SafeClipTopBar
import com.glass.safeclip.ui.components.SecondaryActionButton
import com.glass.safeclip.ui.video.VideoListText

@Composable
fun SubmissionFormScreen(
    video: VideoCandidate,
    clip: VideoClipExportResult?,
    onBack: () -> Unit,
    onSubmit: (SubmissionDraft) -> Unit
) {
    var draft by remember { mutableStateOf(SubmissionDraft()) }

    SafeClipScaffold {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            SafeClipTopBar(
                title = "제출하기",
                subtitle = "사고 정보와 동의 항목을 확인해주세요",
                trailing = {
                    SecondaryActionButton(text = "뒤로", onClick = onBack)
                }
            )

            GlassPanel(modifier = Modifier.fillMaxWidth()) {
                Text("선택한 영상", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Text(video.displayName)
                Text(
                    text = "원본 폴더: ${video.folderPath}",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 12.sp
                )
                Text(
                    text = "크기: ${VideoListText.fileSizeLabel(video.sizeBytes)}",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 12.sp
                )
                Text(
                    text = clip?.let { "제출용 클립: ${it.savedDisplayPath}" } ?: "제출용 클립 없음: 원본 영상으로 제출 준비",
                    color = MaterialTheme.colorScheme.secondary,
                    fontSize = 12.sp
                )
            }

            GlassPanel(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = draft.incidentDateTime,
                    onValueChange = { draft = draft.copy(incidentDateTime = it) },
                    label = { Text("사고 일시") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = draft.locationText,
                    onValueChange = { draft = draft.copy(locationText = it) },
                    label = { Text("위치") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = draft.incidentType,
                    onValueChange = { draft = draft.copy(incidentType = it) },
                    label = { Text("사고 유형") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = draft.memo,
                    onValueChange = { draft = draft.copy(memo = it) },
                    label = { Text("메모") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3
                )
            }

            GlassPanel(modifier = Modifier.fillMaxWidth()) {
                ConsentRow(
                    text = "회사 검토에 동의합니다.",
                    checked = draft.reviewConsent,
                    onCheckedChange = { draft = draft.copy(reviewConsent = it) }
                )
                ConsentRow(
                    text = "영상 보관에 동의합니다.",
                    checked = draft.storageConsent,
                    onCheckedChange = { draft = draft.copy(storageConsent = it) }
                )
                ConsentRow(
                    text = "교통 위험 데이터 활용에 동의합니다.",
                    checked = draft.dataUseConsent,
                    onCheckedChange = { draft = draft.copy(dataUseConsent = it) }
                )
            }

            PrimaryActionButton(
                text = "제출하기",
                enabled = draft.isReadyToSubmit,
                onClick = { onSubmit(draft) }
            )
        }
    }
}

@Composable
private fun ConsentRow(
    text: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Checkbox(checked = checked, onCheckedChange = onCheckedChange)
        Text(text = text, modifier = Modifier.weight(1f))
    }
}
