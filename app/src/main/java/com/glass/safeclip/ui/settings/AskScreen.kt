package com.glass.safeclip.ui.settings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.glass.safeclip.data.ask.AskItem
import com.glass.safeclip.ui.components.GlassPanel
import com.glass.safeclip.ui.components.PrimaryActionButton
import com.glass.safeclip.ui.components.SafeClipScaffold
import com.glass.safeclip.ui.components.SafeClipTopBar
import com.glass.safeclip.ui.components.SecondaryActionButton
import com.glass.safeclip.ui.components.SegmentedTabButton
import com.glass.safeclip.ui.components.StatusChip
import com.glass.safeclip.ui.components.StatusTone

private val AskQuestionTypes = listOf(
    "아이디 관련 문의",
    "신고 관련 문의",
    "제출/영상 문의",
    "답변/처리 문의",
    "기타 문의"
)

@Composable
fun AskScreen(
    askId: String,
    askItems: List<AskItem>,
    isLoading: Boolean,
    message: String?,
    onBack: () -> Unit,
    onRefresh: () -> Unit,
    onDelete: (AskItem) -> Unit,
    onSubmit: (questionType: String, question: String) -> Unit
) {
    var selectedType by remember { mutableStateOf(AskQuestionTypes.first()) }
    var question by remember { mutableStateOf("") }
    var deleteTarget by remember { mutableStateOf<AskItem?>(null) }

    deleteTarget?.let { target ->
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            title = { Text(text = "문의 삭제") },
            text = {
                Text(
                    text = "이 문의를 삭제하시겠어요? 삭제한 문의와 답변은 다시 확인할 수 없습니다.",
                    lineHeight = 20.sp
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        deleteTarget = null
                        onDelete(target)
                    }
                ) {
                    Text(text = "삭제하기", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { deleteTarget = null }) {
                    Text(text = "취소")
                }
            }
        )
    }

    SafeClipScaffold {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            SafeClipTopBar(
                subtitle = "일반 문의",
                trailing = {
                    SecondaryActionButton(text = "뒤로", onClick = onBack)
                }
            )

            GlassPanel(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "내 문의 내역",
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp
                    )
                    SecondaryActionButton(text = "새로고침", onClick = onRefresh)
                }

                when {
                    isLoading -> {
                        Text(
                            text = "문의 내역을 불러오는 중입니다.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    askItems.isEmpty() -> {
                        Text(
                            text = "아직 접수된 일반 문의가 없습니다.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    else -> {
                        askItems.forEach { item ->
                            AskHistoryItem(
                                item = item,
                                onDelete = { deleteTarget = item }
                            )
                        }
                    }
                }
            }

            GlassPanel(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "문의 접수",
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp
                )
                Text(
                    text = "문의 ID: $askId",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 20.sp
                )
                AskTypeSelector(
                    selectedType = selectedType,
                    onSelect = { selectedType = it }
                )
                OutlinedTextField(
                    value = question,
                    onValueChange = { question = it },
                    label = { Text("문의 내용") },
                    minLines = 5,
                    modifier = Modifier.fillMaxWidth()
                )
                PrimaryActionButton(
                    text = "문의 접수하기",
                    enabled = question.isNotBlank(),
                    onClick = {
                        onSubmit(selectedType, question)
                        question = ""
                    },
                    modifier = Modifier.fillMaxWidth()
                )
                message?.takeIf { it.isNotBlank() }?.let {
                    Text(
                        text = it,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 20.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun AskTypeSelector(
    selectedType: String,
    onSelect: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = "문의 종류",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold
        )
        AskQuestionTypes.forEach { type ->
            SegmentedTabButton(
                text = type,
                selected = type == selectedType,
                onClick = { onSelect(type) },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun AskHistoryItem(
    item: AskItem,
    onDelete: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.55f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f))
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = item.questionType.ifBlank { "일반 문의" },
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
                StatusChip(
                    label = if (item.answer.isBlank()) "답변 대기" else "답변 완료",
                    tone = if (item.answer.isBlank()) StatusTone.Neutral else StatusTone.Info
                )
            }
            Text(
                text = item.questionAtText.ifBlank { "질문 시각 없음" },
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 12.sp
            )
            Text(
                text = item.question.ifBlank { "질문 내용 없음" },
                fontWeight = FontWeight.SemiBold,
                lineHeight = 20.sp
            )
            Text(
                text = if (item.answer.isBlank()) {
                    "답변이 등록되면 이곳에 표시됩니다."
                } else {
                    "답변: ${item.answer}"
                },
                color = if (item.answer.isBlank()) {
                    MaterialTheme.colorScheme.onSurfaceVariant
                } else {
                    MaterialTheme.colorScheme.primary
                },
                lineHeight = 20.sp
            )
            SecondaryActionButton(
                text = "삭제",
                onClick = onDelete,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
