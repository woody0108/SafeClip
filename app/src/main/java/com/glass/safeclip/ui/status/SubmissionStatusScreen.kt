package com.glass.safeclip.ui.status

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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.glass.safeclip.ui.components.GlassPanel
import com.glass.safeclip.ui.components.MetricStrip
import com.glass.safeclip.ui.components.PrimaryActionButton
import com.glass.safeclip.ui.components.SafeClipScaffold
import com.glass.safeclip.ui.components.SafeClipTopBar
import com.glass.safeclip.ui.components.SecondaryActionButton
import com.glass.safeclip.ui.components.StatusChip
import com.glass.safeclip.ui.components.StatusTone

@Composable
fun SubmissionStatusScreen(
    records: List<LocalSubmissionRecord>,
    onBackHome: () -> Unit,
    onOpenSubmission: (LocalSubmissionRecord) -> Unit,
    onOpenSubmittedFile: (LocalSubmissionRecord) -> Unit
) {
    var selectedRecord by remember(records) { mutableStateOf<LocalSubmissionRecord?>(null) }

    SafeClipScaffold {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            SafeClipTopBar(
                title = "제출 현황",
                subtitle = "제출한 영상의 처리 상태를 확인하세요",
                trailing = {
                    SecondaryActionButton(text = "홈", onClick = onBackHome)
                }
            )

            MetricStrip(
                metrics = listOf(
                    "제출" to records.size.toString(),
                    "검토중" to records.count { it.status == SubmissionStatus.Reviewing || it.status == SubmissionStatus.WaitingReview }.toString(),
                    "자료생성" to records.count { it.status == SubmissionStatus.ReportPackageReady }.toString(),
                    "결과회신" to records.count { it.status == SubmissionStatus.Completed }.toString()
                )
            )

            if (records.isEmpty()) {
                EmptyStatus(onBackHome = onBackHome)
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(records) { record ->
                        SubmissionRecordRow(
                            record = record,
                            onClick = {
                                selectedRecord = record
                                onOpenSubmission(record)
                            }
                        )
                    }
                }
            }

            selectedRecord?.let { record ->
                SubmissionDetailPanel(
                    record = record,
                    onOpenSubmittedFile = { onOpenSubmittedFile(record) }
                )
            }
        }
    }
}

@Composable
private fun EmptyStatus(onBackHome: () -> Unit) {
    GlassPanel(modifier = Modifier.fillMaxWidth()) {
        Text("아직 제출한 영상이 없습니다.", fontWeight = FontWeight.Bold)
        Text(
            text = "영상을 선택하고 제출 정보를 입력하면 이곳에서 처리 상태를 볼 수 있습니다.",
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        PrimaryActionButton(text = "홈으로 돌아가기", onClick = onBackHome)
    }
}

@Composable
private fun SubmissionRecordRow(
    record: LocalSubmissionRecord,
    onClick: () -> Unit
) {
    GlassPanel(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = record.title,
                modifier = Modifier.weight(1f),
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            if (record.submittedAtText.isNotBlank()) {
                Text(
                    text = record.submittedAtText,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 11.sp,
                    maxLines = 1
                )
            }
        }
        Text(
            text = "${record.incidentDateTime} · ${record.locationText}",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 12.sp
        )
        Text(
            text = record.video.displayName,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 12.sp
        )
        StatusChip(
            label = SubmissionStatusUiText.labelFor(record.status),
            tone = toneForStatus(record.status)
        )
        SecondaryActionButton(text = "상세 보기", onClick = onClick)
    }
}

@Composable
private fun SubmissionDetailPanel(
    record: LocalSubmissionRecord,
    onOpenSubmittedFile: () -> Unit
) {
    GlassPanel(modifier = Modifier.fillMaxWidth()) {
        Text("제출 상세", fontWeight = FontWeight.Bold, fontSize = 18.sp)
        SubmissionDetailText.linesFor(record).forEach { line ->
            Text(
                text = line,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 21.sp
            )
        }
        Text(
            text = "파일 : ${record.video.displayName}",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 12.sp
        )
        SecondaryActionButton(
            text = if (isImageFile(record.video.displayName)) "사진 확인하기" else "영상 확인하기",
            onClick = onOpenSubmittedFile,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

private fun isImageFile(fileName: String): Boolean {
    val lowerName = fileName.lowercase()
    return lowerName.endsWith(".jpg") || lowerName.endsWith(".jpeg")
}

private fun toneForStatus(status: SubmissionStatus): StatusTone {
    return when (status) {
        SubmissionStatus.Uploading,
        SubmissionStatus.WaitingReview,
        SubmissionStatus.Reviewing -> StatusTone.Warning
        SubmissionStatus.NeedsMoreInfo,
        SubmissionStatus.Rejected -> StatusTone.Error
        SubmissionStatus.ReportPackageReady,
        SubmissionStatus.Completed -> StatusTone.Success
    }
}
