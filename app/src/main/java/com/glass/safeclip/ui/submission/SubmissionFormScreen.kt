package com.glass.safeclip.ui.submission

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Checkbox
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.glass.safeclip.data.media.VideoClipExportResult
import com.glass.safeclip.data.file.ManagedFolderFile
import com.glass.safeclip.data.submission.AttachmentSelectionResult
import com.glass.safeclip.data.submission.SubmissionAttachment
import com.glass.safeclip.data.submission.SubmissionAttachmentCandidateDisplay
import com.glass.safeclip.data.submission.SubmissionAttachmentFilter
import com.glass.safeclip.data.submission.SubmissionAttachmentKind
import com.glass.safeclip.data.submission.SubmissionAttachmentRules
import com.glass.safeclip.data.submission.SubmissionFileMetadata
import com.glass.safeclip.data.submission.SubmissionMetadataAutofill
import com.glass.safeclip.data.submission.NasSubmissionUploadProgress
import com.glass.safeclip.ui.components.GlassPanel
import com.glass.safeclip.ui.components.PrimaryActionButton
import com.glass.safeclip.ui.components.SafeClipScaffold
import com.glass.safeclip.ui.components.SafeClipTopBar
import com.glass.safeclip.ui.components.SecondaryActionButton
import com.glass.safeclip.ui.components.SegmentedTabButton
import com.glass.safeclip.ui.theme.SafeClipBorder
import com.glass.safeclip.ui.theme.SafeClipCyan
import com.glass.safeclip.ui.video.VideoListText
import java.util.Calendar

@Composable
fun SubmissionFormScreen(
    clip: VideoClipExportResult?,
    initialAttachment: SubmissionAttachment,
    availableFiles: List<ManagedFolderFile>,
    availableFolderPath: String,
    eventFiles: List<ManagedFolderFile>,
    uploadProgress: NasSubmissionUploadProgress?,
    uploadFailureMessage: String?,
    onLoadRepresentativeMetadata: suspend (SubmissionAttachment) -> SubmissionFileMetadata?,
    onBack: () -> Unit,
    onSubmit: (SubmissionDraft, SubmissionAttachment, List<SubmissionAttachment>) -> Unit
) {
    var draft by remember { mutableStateOf(SubmissionDraft()) }
    var step by remember { mutableStateOf(SubmissionStep.Files) }
    var showConsentDialog by remember { mutableStateOf(false) }
    var incidentDateTimeFields by remember {
        mutableStateOf(SubmissionIncidentDateTimeFields.fromCombined(draft.incidentDateTime))
    }
    var attachments by remember(initialAttachment.uriString) {
        mutableStateOf(listOf(initialAttachment))
    }
    var representativeAttachment by remember(initialAttachment.uriString) {
        mutableStateOf(initialAttachment)
    }
    var dateTimeAutofilled by remember { mutableStateOf(false) }
    var locationAutofilled by remember { mutableStateOf(false) }
    var attachmentMessage by remember { mutableStateOf<String?>(null) }
    var previewSelection by remember(initialAttachment.uriString) {
        mutableStateOf(SubmissionAttachmentPreviewSelection.initial(initialAttachment))
    }
    var selectedSource by remember { mutableStateOf(SubmissionAttachmentSource.SelectedFolder) }
    var selectedFilter by remember { mutableStateOf(SubmissionAttachmentFilter.All) }
    var showAllCandidates by remember { mutableStateOf(false) }
    var candidatePanelState by remember { mutableStateOf(SubmissionCollapsiblePanelState(expanded = true)) }
    var previewPanelState by remember { mutableStateOf(SubmissionCollapsiblePanelState(expanded = true)) }
    val selectedFolderAttachments = availableFiles.mapNotNull { file ->
        SubmissionAttachment.fromManagedFile(file, availableFolderPath)
    }
    val eventFolderAttachments = eventFiles.mapNotNull { file ->
        SubmissionAttachment.fromManagedFile(file, "SafeClip 폴더")
    }
    val sourceAttachments = when (selectedSource) {
        SubmissionAttachmentSource.SelectedFolder -> selectedFolderAttachments
        SubmissionAttachmentSource.EventFolder -> eventFolderAttachments
    }
    val candidateAttachments = selectedFilter.apply(sourceAttachments)
        .filterNot { candidate -> attachments.any { it.uriString == candidate.uriString } }
    val visibleCandidateAttachments = SubmissionAttachmentCandidateDisplay.visible(
        candidates = candidateAttachments,
        showAll = showAllCandidates
    )
    val videoCount = attachments.count { it.kind == SubmissionAttachmentKind.Video }
    val photoCount = attachments.count { it.kind == SubmissionAttachmentKind.Photo }

    LaunchedEffect(representativeAttachment.uriString) {
        val metadata = onLoadRepresentativeMetadata(representativeAttachment)
        val result = SubmissionMetadataAutofill.apply(
            draft = draft,
            metadata = metadata,
            replaceDateTime = dateTimeAutofilled,
            replaceLocation = locationAutofilled
        )
        draft = result.draft
        incidentDateTimeFields = SubmissionIncidentDateTimeFields.fromCombined(result.draft.incidentDateTime)
        dateTimeAutofilled = result.filledDateTime || (dateTimeAutofilled && result.draft.incidentDateTime.isNotBlank())
        locationAutofilled = result.filledLocation || (locationAutofilled && result.draft.locationText.isNotBlank())
        if (result.filledAny) {
            attachmentMessage = "대표 파일 정보로 사고일시/위치를 채웠습니다."
        }
    }

    SafeClipScaffold {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            SafeClipTopBar(
                title = "제출하기",
                subtitle = if (step == SubmissionStep.Files) "제출할 파일을 확인해주세요" else "사고 정보를 입력해주세요",
                trailing = {
                    SecondaryActionButton(
                        text = "뒤로",
                        onClick = {
                            if (step == SubmissionStep.Details) {
                                step = SubmissionStep.Files
                            } else {
                                onBack()
                            }
                        }
                    )
                }
            )

            uploadProgress?.let {
                UploadProgressDialog(progress = it)
            }
            uploadFailureMessage?.takeIf { it.isNotBlank() }?.let {
                GlassPanel(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "제출이 중단되었습니다.",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                    Text(
                        text = it,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 21.sp
                    )
                    Text(
                        text = "내용은 유지되어 있습니다. 제출하기를 다시 누르면 같은 제출 묶음으로 재시도합니다.",
                        color = MaterialTheme.colorScheme.secondary,
                        lineHeight = 21.sp
                    )
                }
            }

            if (step == SubmissionStep.Files) {
                GlassPanel(modifier = Modifier.fillMaxWidth()) {
                    Text("첨부 파일", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    Text(
                        text = clip?.let { "제출용 클립: ${it.savedDisplayPath}" } ?: "제출용 클립 없음: 원본 영상으로 제출 준비",
                        color = MaterialTheme.colorScheme.secondary,
                        fontSize = 12.sp
                    )
                    Text(
                        text = "영상 $videoCount/${SubmissionAttachmentRules.MaxVideos}개, 사진 $photoCount/${SubmissionAttachmentRules.MaxPhotos}개",
                        color = MaterialTheme.colorScheme.secondary,
                        fontSize = 12.sp
                    )
                    attachments.forEach { attachment ->
                        AttachmentRow(
                            attachment = attachment,
                            representative = attachment.uriString == representativeAttachment.uriString,
                            onMakeRepresentative = {
                                representativeAttachment = attachment
                                previewSelection = previewSelection.select(attachment)
                                attachmentMessage = "대표 파일을 변경했습니다."
                            },
                            onPreview = {
                                previewSelection = previewSelection.select(attachment)
                            },
                            onRemove = {
                                if (attachment.uriString != representativeAttachment.uriString) {
                                    attachments = attachments.filterNot { it.uriString == attachment.uriString }
                                    attachmentMessage = "첨부에서 제외했습니다."
                                } else {
                                    attachmentMessage = "대표 파일은 먼저 다른 파일로 변경한 뒤 해제할 수 있습니다."
                                }
                            }
                        )
                    }
                    attachmentMessage?.let {
                        Text(text = it, color = MaterialTheme.colorScheme.secondary, fontSize = 12.sp)
                    }
                }

                GlassPanel(modifier = Modifier.fillMaxWidth()) {
                    CollapsiblePanelHeader(
                        title = "추가 가능한 파일",
                        state = candidatePanelState,
                        onToggle = { candidatePanelState = candidatePanelState.toggle() }
                    )
                    if (candidatePanelState.showContent) {
                        Text(
                            text = "영상은 500MB 미만 2개까지, JPG 사진은 50MB 미만 5개까지 선택할 수 있습니다.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 12.sp
                        )
                        AttachmentSourceTabs(
                            selected = selectedSource,
                            onSelected = {
                                selectedSource = it
                                showAllCandidates = false
                            }
                        )
                        AttachmentFilterTabs(
                            selected = selectedFilter,
                            onSelected = {
                                selectedFilter = it
                                showAllCandidates = false
                            }
                        )
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = if (showAllCandidates) 360.dp else 250.dp)
                                .verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            visibleCandidateAttachments.forEach { candidate ->
                                AttachmentRow(
                                    attachment = candidate,
                                    representative = false,
                                    onMakeRepresentative = null,
                                    onRemove = null,
                                    extraActionText = "추가",
                                    onPreview = {
                                        previewSelection = previewSelection.selectCandidate(candidate)
                                    },
                                    onExtraAction = {
                                        when (val result = SubmissionAttachmentRules.add(attachments, candidate)) {
                                            is AttachmentSelectionResult.Accepted -> {
                                                attachments = result.attachments
                                                attachmentMessage = "첨부 파일을 추가했습니다."
                                            }
                                            is AttachmentSelectionResult.Rejected -> {
                                                attachmentMessage = result.message
                                            }
                                        }
                                    }
                                )
                            }
                        }
                        if (candidateAttachments.isEmpty()) {
                            Text(
                                text = "추가할 수 있는 영상/JPG 파일이 없습니다.",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 12.sp
                            )
                        } else if (candidateAttachments.size > SubmissionAttachmentCandidateDisplay.PreviewLimit) {
                            SecondaryActionButton(
                                text = if (showAllCandidates) "접기" else "전체보기 (${candidateAttachments.size}개)",
                                onClick = { showAllCandidates = !showAllCandidates },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }

                AttachmentPreviewPanel(
                    selection = previewSelection,
                    state = previewPanelState,
                    onToggle = { previewPanelState = previewPanelState.toggle() }
                )

                PrimaryActionButton(
                    text = "제출하기",
                    enabled = SubmissionStep.Files.canContinue(draft, attachments) && uploadProgress == null,
                    onClick = { step = SubmissionStep.Details }
                )
            } else {
                GlassPanel(modifier = Modifier.fillMaxWidth()) {
                    IncidentDateTimeInputs(
                        fields = incidentDateTimeFields,
                        onFieldsChange = { fields ->
                            dateTimeAutofilled = false
                            incidentDateTimeFields = fields
                            draft = draft.copy(incidentDateTime = fields.combined())
                        }
                    )
                    OutlinedTextField(
                        value = draft.locationText,
                        onValueChange = {
                            locationAutofilled = false
                            draft = draft.copy(locationText = it)
                        },
                        label = { Text("위치") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = draft.incidentType,
                        onValueChange = { draft = draft.copy(incidentType = it) },
                        label = { Text("신고 유형") },
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

                PrimaryActionButton(
                    text = "제출하기",
                    enabled = SubmissionStep.Details.canContinue(draft, attachments) && uploadProgress == null,
                    onClick = { showConsentDialog = true }
                )
            }

            if (showConsentDialog) {
                AlertDialog(
                    onDismissRequest = { showConsentDialog = false },
                    title = { Text("제출 동의") },
                    text = {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
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
                    },
                    confirmButton = {
                        PrimaryActionButton(
                            text = "제출하기",
                            enabled = draft.canSubmitWith(attachments) && uploadProgress == null,
                            onClick = {
                                showConsentDialog = false
                                val orderedAttachments = listOf(representativeAttachment) +
                                    attachments.filterNot { it.uriString == representativeAttachment.uriString }
                                onSubmit(draft, representativeAttachment, orderedAttachments)
                            }
                        )
                    },
                    dismissButton = {
                        SecondaryActionButton(
                            text = "취소",
                            onClick = { showConsentDialog = false }
                        )
                    }
                )
            }
        }
    }
}

@Composable
private fun UploadProgressDialog(
    progress: NasSubmissionUploadProgress
) {
    AlertDialog(
        onDismissRequest = {},
        title = { Text("제출 중") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = progress.fileLabel,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 12.sp
                )
                progress.progressFraction?.let { fraction ->
                    LinearProgressIndicator(
                        progress = { fraction },
                        modifier = Modifier.fillMaxWidth(),
                        color = SafeClipCyan
                    )
                    Text(
                        text = "${progress.currentFilePercent}% · ${progress.remainingTimeText}",
                        color = MaterialTheme.colorScheme.secondary,
                        fontSize = 12.sp
                    )
                } ?: run {
                    LinearProgressIndicator(
                        modifier = Modifier.fillMaxWidth(),
                        color = SafeClipCyan
                    )
                    Text(
                        text = progress.remainingTimeText,
                        color = MaterialTheme.colorScheme.secondary,
                        fontSize = 12.sp
                    )
                }
            }
        },
        confirmButton = {
            Text(
                text = "업로드가 끝날 때까지 기다려주세요.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 12.sp
            )
        }
    )
}

private enum class SubmissionAttachmentSource(val label: String) {
    SelectedFolder("블랙박스 폴더"),
    EventFolder("SafeClip 폴더")
}

@Composable
private fun AttachmentSourceTabs(
    selected: SubmissionAttachmentSource,
    onSelected: (SubmissionAttachmentSource) -> Unit
) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        SubmissionAttachmentSource.values().forEach { source ->
            SegmentedTabButton(
                text = source.label,
                onClick = { onSelected(source) },
                selected = source == selected,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun AttachmentFilterTabs(
    selected: SubmissionAttachmentFilter,
    onSelected: (SubmissionAttachmentFilter) -> Unit
) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        SubmissionAttachmentFilter.values().forEach { filter ->
            SegmentedTabButton(
                text = filter.label,
                onClick = { onSelected(filter) },
                selected = filter == selected,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun AttachmentRow(
    attachment: SubmissionAttachment,
    representative: Boolean,
    onMakeRepresentative: (() -> Unit)?,
    onRemove: (() -> Unit)?,
    onPreview: () -> Unit,
    extraActionText: String? = null,
    onExtraAction: (() -> Unit)? = null
) {
    val actions = SubmissionAttachmentRowActions.from(representative)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onPreview),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = attachment.displayName, fontWeight = FontWeight.SemiBold)
            Text(
                text = if (attachment.kind == SubmissionAttachmentKind.Video) "영상" else "사진",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 12.sp
            )
        }
        if (extraActionText != null && onExtraAction != null) {
            SecondaryActionButton(text = extraActionText, onClick = onExtraAction)
        } else {
            AttachmentRowActionButton(
                text = if (representative) "대표" else "대표로",
                selected = actions.representativeButtonSelected,
                onClick = { onMakeRepresentative?.invoke() }
            )
            if (actions.showRemoveButton) {
                AttachmentRowActionButton(
                    text = "해제",
                    selected = false,
                    onClick = { onRemove?.invoke() }
                )
            }
        }
    }
}

@Composable
private fun AttachmentRowActionButton(
    text: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    if (selected) {
        SegmentedTabButton(
            text = text,
            selected = true,
            onClick = onClick
        )
    } else {
        SecondaryActionButton(
            text = text,
            onClick = onClick
        )
    }
}

@Composable
private fun AttachmentPreviewPanel(
    selection: SubmissionAttachmentPreviewSelection,
    state: SubmissionCollapsiblePanelState,
    onToggle: () -> Unit
) {
    val attachment = selection.selected
    GlassPanel(modifier = Modifier.fillMaxWidth()) {
        CollapsiblePanelHeader(
            title = selection.title,
            state = state,
            onToggle = onToggle
        )
        if (!state.showContent) {
            return@GlassPanel
        }
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp),
            shape = RoundedCornerShape(8.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, SafeClipBorder)
        ) {
            if (attachment.kind == SubmissionAttachmentKind.Photo) {
                AttachmentPhotoThumbnail(attachment = attachment)
            } else {
                AttachmentVideoThumbnailPlaceholder(attachment = attachment)
            }
        }
        Text(attachment.displayName, fontWeight = FontWeight.SemiBold)
        Text(
            text = "${if (attachment.kind == SubmissionAttachmentKind.Video) "영상" else "사진"} · ${VideoListText.fileSizeLabel(attachment.sizeBytes)}",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 12.sp
        )
    }
}

@Composable
private fun CollapsiblePanelHeader(
    title: String,
    state: SubmissionCollapsiblePanelState,
    onToggle: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp,
            modifier = Modifier.weight(1f)
        )
        SecondaryActionButton(
            text = state.toggleLabel,
            onClick = onToggle
        )
    }
}

@Composable
private fun IncidentDateTimeInputs(
    fields: SubmissionIncidentDateTimeFields,
    onFieldsChange: (SubmissionIncidentDateTimeFields) -> Unit
) {
    val context = LocalContext.current
    val calendar = remember { Calendar.getInstance() }
    val datePicker = remember(fields) {
        DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                onFieldsChange(
                    fields.copy(
                        date = SubmissionIncidentDateTimeFields.formatDate(
                            year = year,
                            month = month + 1,
                            day = dayOfMonth
                        )
                    )
                )
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        )
    }
    val timePicker = remember(fields) {
        TimePickerDialog(
            context,
            { _, hourOfDay, minute ->
                onFieldsChange(
                    fields.copy(
                        time = SubmissionIncidentDateTimeFields.formatTime(hourOfDay, minute)
                    )
                )
            },
            calendar.get(Calendar.HOUR_OF_DAY),
            calendar.get(Calendar.MINUTE),
            true
        )
    }

    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(
            value = fields.date,
            onValueChange = {
                onFieldsChange(fields.copy(date = SubmissionIncidentDateTimeFields.sanitizeDate(it)))
            },
            label = { Text("사고 날짜") },
            modifier = Modifier.weight(1f)
        )
        SecondaryActionButton(
            text = "달력",
            onClick = { datePicker.show() }
        )
    }
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(
            value = fields.time,
            onValueChange = {
                onFieldsChange(fields.copy(time = SubmissionIncidentDateTimeFields.sanitizeTime(it)))
            },
            label = { Text("사고 시간") },
            modifier = Modifier.weight(1f)
        )
        SecondaryActionButton(
            text = "시간",
            onClick = { timePicker.show() }
        )
    }
}

@Composable
private fun AttachmentPhotoThumbnail(
    attachment: SubmissionAttachment
) {
    val context = LocalContext.current
    val imageBitmap = remember(attachment.uriString) {
        runCatching {
            context.contentResolver.openInputStream(Uri.parse(attachment.uriString))?.use { input ->
                BitmapFactory.decodeStream(input)?.asImageBitmap()
            }
        }.getOrNull()
    }

    if (imageBitmap != null) {
        Image(
            bitmap = imageBitmap,
            contentDescription = attachment.displayName,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )
    } else {
        AttachmentPreviewFallback(text = "사진 미리보기를 불러오지 못했습니다.")
    }
}

@Composable
private fun AttachmentVideoThumbnailPlaceholder(
    attachment: SubmissionAttachment
) {
    val context = LocalContext.current
    val imageBitmap = remember(attachment.uriString) {
        runCatching {
            val retriever = MediaMetadataRetriever()
            try {
                retriever.setDataSource(context, Uri.parse(attachment.uriString))
                retriever.getFrameAtTime(0L)?.asImageBitmap()
            } finally {
                retriever.release()
            }
        }.getOrNull()
    }

    if (imageBitmap != null) {
        Image(
            bitmap = imageBitmap,
            contentDescription = attachment.displayName,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )
    } else {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("영상 대표화면", color = SafeClipCyan, fontWeight = FontWeight.Bold)
                Text(
                    text = attachment.displayName,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 12.sp
                )
            }
        }
    }
}

@Composable
private fun AttachmentPreviewFallback(text: String) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 12.sp
        )
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
