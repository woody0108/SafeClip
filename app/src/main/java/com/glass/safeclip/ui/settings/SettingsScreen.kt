package com.glass.safeclip.ui.settings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import com.glass.safeclip.ui.components.GlassPanel
import com.glass.safeclip.ui.components.SafeClipScaffold
import com.glass.safeclip.ui.components.SafeClipTopBar
import com.glass.safeclip.ui.components.SecondaryActionButton
import com.glass.safeclip.ui.theme.SafeClipError

@Composable
fun SettingsScreen(
    guestId: String,
    linkedEmail: String?,
    linkedDisplayName: String?,
    linkedProvider: String?,
    message: String?,
    onBack: () -> Unit,
    onSignOut: () -> Unit,
    onOpenAsk: () -> Unit,
    onDeleteAccount: () -> Unit
) {
    val accountText = SettingsAccountText.from(
        guestId = guestId,
        linkedEmail = linkedEmail,
        linkedDisplayName = linkedDisplayName,
        linkedProvider = linkedProvider
    )
    val accountActions = SettingsAccountActions.from(
        linkedEmail = linkedEmail,
        linkedDisplayName = linkedDisplayName,
        linkedProvider = linkedProvider
    )
    var showDeleteDialog by remember { mutableStateOf(false) }

    if (showDeleteDialog) {
        DeleteAccountDialog(
            onConfirm = {
                showDeleteDialog = false
                onDeleteAccount()
            },
            onDismiss = { showDeleteDialog = false }
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
                subtitle = "설정",
                trailing = {
                    SecondaryActionButton(text = "뒤로", onClick = onBack)
                }
            )

            GlassPanel(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "계정 정보",
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp
                )
                Text(
                    text = accountText.idLine,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 16.sp
                )
                Text(
                    text = accountText.statusLine,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 21.sp
                )
                accountText.providerLine?.let {
                    Text(
                        text = it,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 21.sp
                    )
                }
                message?.takeIf { it.isNotBlank() }?.let {
                    Text(
                        text = it,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 20.sp
                    )
                }
            }

            GlassPanel(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "계정 전환",
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp
                )
                Text(
                    text = "로그아웃해도 이 기기의 Guest ID는 유지됩니다.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 21.sp
                )
                SecondaryActionButton(
                    text = accountActions.signOutText,
                    onClick = onSignOut,
                    enabled = accountActions.signOutEnabled
                )
            }

            GlassPanel(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "일반 문의",
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp
                )
                Text(
                    text = "앱 사용 중 궁금한 점이나 요청하고 싶은 내용을 남겨주세요.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 21.sp
                )
                SecondaryActionButton(
                    text = "문의 화면 열기",
                    onClick = onOpenAsk,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            GlassPanel(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "회원 관리",
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp
                )
                Text(
                    text = "회원탈퇴를 누르면 연결된 Firebase 계정 삭제를 시도합니다. 비회원 상태에서는 로컬 Guest ID만 사용 중이라 서버 계정 삭제가 필요하지 않습니다.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 21.sp
                )
                OutlinedButton(
                    onClick = { showDeleteDialog = true },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, SafeClipError.copy(alpha = 0.85f))
                ) {
                    Text(
                        text = "회원탈퇴",
                        color = SafeClipError,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun DeleteAccountDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = "회원탈퇴") },
        text = {
            Text(
                text = "계정 연결을 삭제하시겠어요? 제출 내역 복구가 어려워질 수 있습니다.",
                lineHeight = 20.sp
            )
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(text = "탈퇴하기", color = SafeClipError, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = "취소")
            }
        }
    )
}
