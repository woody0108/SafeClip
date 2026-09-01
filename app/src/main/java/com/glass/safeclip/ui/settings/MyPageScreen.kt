package com.glass.safeclip.ui.settings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.glass.safeclip.data.auth.AuthProviderOption
import com.glass.safeclip.data.auth.EmailLoginInput
import com.glass.safeclip.ui.components.GlassPanel
import com.glass.safeclip.ui.components.PrimaryActionButton
import com.glass.safeclip.ui.components.SafeClipScaffold
import com.glass.safeclip.ui.components.SafeClipTopBar
import com.glass.safeclip.ui.components.SecondaryActionButton
import com.glass.safeclip.ui.components.TopBarIconButton
import com.glass.safeclip.ui.onboarding.EmailLoginDialogMessage
import com.glass.safeclip.ui.theme.SafeClipBorder
import com.glass.safeclip.ui.theme.SafeClipCyan
import com.glass.safeclip.ui.theme.SafeClipError

@Composable
fun MyPageScreen(
    guestId: String,
    linkedEmail: String?,
    linkedDisplayName: String?,
    linkedProvider: String?,
    message: String?,
    permissionItems: List<SettingsPermissionItem>,
    inquiryCount: Int,
    answerCount: Int,
    onBack: () -> Unit,
    onGoogleLogin: () -> Unit,
    onEmailLogin: (String, String) -> Unit,
    onSignOut: () -> Unit,
    onOpenSettings: () -> Unit,
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
    var showLoginChoiceDialog by remember { mutableStateOf(false) }
    var showEmailLoginDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    LaunchedEffect(accountActions.signOutEnabled) {
        if (accountActions.signOutEnabled) {
            showLoginChoiceDialog = false
            showEmailLoginDialog = false
        }
    }

    if (showLoginChoiceDialog) {
        LoginChoiceDialog(
            message = message,
            onGoogleLogin = {
                showLoginChoiceDialog = false
                onGoogleLogin()
            },
            onEmailLogin = {
                showLoginChoiceDialog = false
                showEmailLoginDialog = true
            },
            onDismiss = { showLoginChoiceDialog = false }
        )
    }
    if (showEmailLoginDialog) {
        MyPageEmailLoginDialog(
            authMessage = message,
            onSubmit = onEmailLogin,
            onDismiss = { showEmailLoginDialog = false }
        )
    }
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
                subtitle = "계정과 이용 내역",
                trailing = {
                    TopBarIconButton(
                        icon = Icons.AutoMirrored.Rounded.ArrowBack,
                        contentDescription = "뒤로가기",
                        onClick = onBack
                    )
                }
            )

            GlassPanel(modifier = Modifier.fillMaxWidth()) {
                Text(text = "내 계정", fontWeight = FontWeight.Bold, fontSize = 20.sp)
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
                    Text(text = it, color = MaterialTheme.colorScheme.onSurfaceVariant, lineHeight = 21.sp)
                }
                message?.takeIf { it.isNotBlank() }?.let {
                    Text(text = it, color = SafeClipCyan, lineHeight = 20.sp)
                }
                if (accountActions.signOutEnabled) {
                    SecondaryActionButton(
                        text = accountActions.signOutText,
                        onClick = onSignOut,
                        modifier = Modifier.fillMaxWidth()
                    )
                } else {
                    PrimaryActionButton(
                        text = "로그인",
                        onClick = { showLoginChoiceDialog = true },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            GlassPanel(modifier = Modifier.fillMaxWidth()) {
                Text(text = "앱 권한", fontWeight = FontWeight.Bold, fontSize = 20.sp)
                Text(
                    text = SettingsPermissionItems.summary(permissionItems),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 21.sp
                )
                SecondaryActionButton(
                    text = "권한 및 설정 관리",
                    onClick = onOpenSettings,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            GlassPanel(modifier = Modifier.fillMaxWidth()) {
                Text(text = "문의 내역", fontWeight = FontWeight.Bold, fontSize = 20.sp)
                Text(
                    text = "문의 ${inquiryCount}건 · 답변 ${answerCount}건",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 21.sp
                )
                SecondaryActionButton(
                    text = "문의 내역 열기",
                    onClick = onOpenAsk,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            if (accountActions.signOutEnabled) {
                GlassPanel(modifier = Modifier.fillMaxWidth()) {
                    Text(text = "회원 관리", fontWeight = FontWeight.Bold, fontSize = 20.sp)
                    Text(
                        text = DeleteAccountText.warning,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 21.sp
                    )
                    OutlinedButton(
                        onClick = { showDeleteDialog = true },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, SafeClipError.copy(alpha = 0.85f))
                    ) {
                        Text(text = "회원탈퇴", color = SafeClipError, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun LoginChoiceDialog(
    message: String?,
    onGoogleLogin: () -> Unit,
    onEmailLogin: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = "로그인") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(text = "가입한 계정으로 로그인하면 기존 제출 내역을 이어서 확인할 수 있습니다.")
                message?.takeIf { it.isNotBlank() }?.let {
                    Text(text = it, color = SafeClipCyan, lineHeight = 20.sp)
                }
                PrimaryActionButton(
                    text = AuthProviderOption.Google.loginText,
                    onClick = onGoogleLogin,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedButton(
                    onClick = onEmailLogin,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, SafeClipBorder)
                ) {
                    Text(text = AuthProviderOption.Email.loginText, fontWeight = FontWeight.Bold)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(text = "나중에") }
        }
    )
}

@Composable
private fun MyPageEmailLoginDialog(
    authMessage: String?,
    onSubmit: (String, String) -> Unit,
    onDismiss: () -> Unit
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var validationMessage by remember { mutableStateOf<String?>(null) }
    val loginMessage = validationMessage ?: EmailLoginDialogMessage.from(authMessage)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = "이메일 로그인") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                loginMessage?.takeIf { it.isNotBlank() }?.let {
                    Text(text = it, color = SafeClipError, lineHeight = 20.sp)
                }
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text(text = "이메일") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text(text = "비밀번호") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val input = EmailLoginInput.create(email = email, password = password)
                    if (input.errorMessage != null) {
                        validationMessage = input.errorMessage
                    } else {
                        onSubmit(input.email, input.password)
                    }
                }
            ) {
                Text(text = "로그인", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(text = "취소") }
        }
    )
}

@Composable
private fun DeleteAccountDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = "회원탈퇴") },
        text = { Text(text = DeleteAccountText.warning, lineHeight = 20.sp) },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(text = "탈퇴하기", color = SafeClipError, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(text = "취소") }
        }
    )
}
