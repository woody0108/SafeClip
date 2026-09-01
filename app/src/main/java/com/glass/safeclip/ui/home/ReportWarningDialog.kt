package com.glass.safeclip.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties

internal const val REPORT_WARNING_ACTION_ROW_TAG = "report_warning_action_row"
internal const val REPORT_WARNING_BODY_TAG = "report_warning_body"
internal const val REPORT_WARNING_CHECK_ROW_TAG = "report_warning_check_row"

@Composable
internal fun ReportWarningDialog(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = {},
        properties = DialogProperties(
            dismissOnBackPress = false,
            dismissOnClickOutside = false
        ),
        title = {
            Text(
                text = "교통법규 위반 신고 안내",
                fontSize = 20.sp
            )
        },
        text = {
            Text(
                text = "출처 : 안전신문고\n\n" +
                    "안전신문고로 접수되는 교통법규 위반 신고의 경우 증거주의 원칙에 따라 " +
                    "신고인이 제출한 증거자료(동영상, 사진)에 의해 피신고자의 위반이 명백해야 처분이 이루어질 수 있으며,\n\n" +
                    "교통법규 위반 신고는 위반일로부터 2일이 경과한 후에 신고된 경우 " +
                    "위반이 확인되더라도 경고·계도 처리됨을 알려드립니다.\n\n" +
                    "※ 제보 마지막 날(이틀째 되는 날)이 주말·공휴일에 해당하는 경우 다음날 평일까지 제보 가능",
                modifier = Modifier
                    .testTag(REPORT_WARNING_BODY_TAG)
                    .heightIn(max = 360.dp)
                    .verticalScroll(rememberScrollState())
                    .padding(end = 8.dp),
                fontSize = 16.sp,
                lineHeight = 23.sp
            )
        },
        confirmButton = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag(REPORT_WARNING_ACTION_ROW_TAG)
            ) {
                HorizontalDivider()
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier
                            .testTag(REPORT_WARNING_CHECK_ROW_TAG)
                            .minimumInteractiveComponentSize()
                            .toggleable(
                                value = checked,
                                role = Role.Checkbox,
                                onValueChange = onCheckedChange
                            )
                            .padding(end = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = checked,
                            onCheckedChange = null
                        )
                        Text(
                            text = "다시 표시 안함",
                            fontSize = 16.sp,
                            lineHeight = 20.sp
                        )
                    }
                    TextButton(onClick = onConfirm) {
                        Text("확인")
                    }
                }
            }
        }
    )
}
