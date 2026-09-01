package com.glass.safeclip.ui.onboarding

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.glass.safeclip.data.auth.AuthProviderOption
import com.glass.safeclip.data.auth.EmailAuthInput
import com.glass.safeclip.data.auth.EmailLoginInput
import com.glass.safeclip.ui.components.GlassPanel
import com.glass.safeclip.ui.components.PrimaryActionButton
import com.glass.safeclip.ui.components.SafeClipScaffold
import com.glass.safeclip.ui.theme.SafeClipBorder
import com.glass.safeclip.ui.theme.SafeClipCyan
import com.glass.safeclip.ui.theme.SafeClipOrange
import com.glass.safeclip.ui.theme.SafeClipSuccess
import com.glass.safeclip.ui.theme.SafeClipSurface
import com.glass.safeclip.ui.theme.SafeClipTextSecondary

internal object StartScreenLayout {
    val BrandVerticalOffset = (-52).dp
}

@Composable
fun BootLoadingScreen() {
    SafeClipScaffold {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            SafeClipLogoIcon(modifier = Modifier.size(86.dp))
            Spacer(modifier = Modifier.height(18.dp))
            Text(
                text = "SafeClip",
                color = Color.White,
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(22.dp))
            SmoothLoadingDots()
        }
    }
}

@Composable
private fun SmoothLoadingDots() {
    val transition = rememberInfiniteTransition(label = "boot-loading-dots")
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = 3f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200),
            repeatMode = RepeatMode.Restart
        ),
        label = "boot-loading-phase"
    )
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(3) { index ->
            val distance = kotlin.math.abs(phase - index)
            val alpha = (1f - distance.coerceIn(0f, 1f) * 0.55f).coerceIn(0.45f, 1f)
            Text(
                text = ".",
                color = SafeClipCyan.copy(alpha = alpha),
                fontSize = 38.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun StartScreen(
    linkedDisplayName: String?,
    linkedEmail: String?,
    authMessage: String?,
    onGoogleLogin: () -> Unit,
    onEmailLogin: (String, String) -> Unit,
    onStart: () -> Unit
) {
    var showLoginChoiceDialog by remember { mutableStateOf(false) }
    var showEmailLoginDialog by remember { mutableStateOf(false) }
    val hasLinkedAccount = !linkedDisplayName.isNullOrBlank() || !linkedEmail.isNullOrBlank()
    val linkedAccountId = if (hasLinkedAccount) {
        StartAccountText.from(
            guestId = "",
            displayName = linkedDisplayName,
            email = linkedEmail
        ).idLine
    } else {
        null
    }

    LaunchedEffect(hasLinkedAccount) {
        if (hasLinkedAccount) {
            showEmailLoginDialog = false
        }
    }

    if (showLoginChoiceDialog) {
        AuthChoiceDialog(
            title = "로그인",
            description = "가입한 계정으로 로그인하면 기존 제출 내역을 이어서 확인할 수 있습니다.",
            googleText = AuthProviderOption.Google.loginText,
            emailText = AuthProviderOption.Email.loginText,
            authMessage = authMessage,
            onGoogle = {
                showLoginChoiceDialog = false
                onGoogleLogin()
            },
            onEmail = {
                showLoginChoiceDialog = false
                showEmailLoginDialog = true
            },
            onDismiss = { showLoginChoiceDialog = false }
        )
    }
    if (showEmailLoginDialog) {
        EmailLoginDialog(
            authMessage = authMessage,
            onSubmit = { email, password ->
                onEmailLogin(email, password)
            },
            onDismiss = { showEmailLoginDialog = false }
        )
    }

    SafeClipScaffold {
        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            Column(
                modifier = Modifier
                    .align(Alignment.Center)
                    .offset(y = StartScreenLayout.BrandVerticalOffset)
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SafeClipLogoIcon(modifier = Modifier.size(54.dp))
                    Text(
                        text = "SafeClip",
                        color = Color.White,
                        style = MaterialTheme.typography.displaySmall,
                        fontWeight = FontWeight.Bold
                    )
                }
                Text(
                    text = "블랙박스 영상을 쉽고 빠르게 제출",
                    color = SafeClipCyan,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.SemiBold,
                    lineHeight = 28.sp,
                    textAlign = TextAlign.Center
                )
            }

            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                linkedAccountId?.let { idLine ->
                    LinkedAccountIdRow(
                        idLine = idLine,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                OutlinedButton(
                    onClick = { showLoginChoiceDialog = true },
                    enabled = !hasLinkedAccount,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, SafeClipCyan.copy(alpha = 0.85f)),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = SafeClipCyan)
                ) {
                    Text(
                        text = if (hasLinkedAccount) "로그인됨" else "로그인",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                PrimaryActionButton(
                    text = "시작하기",
                    onClick = onStart,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(64.dp)
                )
            }
        }
    }
}

@Composable
private fun LinkedAccountIdRow(
    idLine: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
        color = SafeClipSurface.copy(alpha = 0.88f),
        border = BorderStroke(1.dp, SafeClipBorder.copy(alpha = 0.75f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = idLine,
                modifier = Modifier.weight(1f),
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = "로그인됨",
                color = SafeClipSuccess,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun ConnectingScreen() {
    SafeClipScaffold {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            SafeClipLogoIcon(modifier = Modifier.size(72.dp))
            Spacer(modifier = Modifier.height(24.dp))
            CircularProgressIndicator(color = SafeClipCyan)
            Spacer(modifier = Modifier.height(22.dp))
            Text(
                text = "연결중...",
                color = Color.White,
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "SafeClip 폴더와 이전 선택 정보를 확인하고 있습니다.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 14.sp,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
    }
}

@Composable
private fun AccountActionRow(
    guestId: String,
    linkedDisplayName: String?,
    linkedEmail: String?,
    onSignUp: () -> Unit,
    onLogin: () -> Unit,
    modifier: Modifier = Modifier
) {
    val accountText = StartAccountText.from(
        guestId = guestId,
        displayName = linkedDisplayName,
        email = linkedEmail
    )
    if (!accountText.signupEnabled && !accountText.loginEnabled) return

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        OutlinedButton(
            onClick = onSignUp,
            enabled = accountText.signupEnabled,
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(8.dp),
            border = BorderStroke(1.dp, SafeClipCyan.copy(alpha = 0.85f)),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = SafeClipCyan)
        ) {
            Text(text = "회원가입", fontSize = 13.sp, fontWeight = FontWeight.Bold)
        }
        OutlinedButton(
            onClick = onLogin,
            enabled = accountText.loginEnabled,
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(8.dp),
            border = BorderStroke(1.dp, SafeClipBorder),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = SafeClipCyan)
        ) {
            Text(text = "로그인", fontSize = 13.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun GuestIdentityRow(
    guestId: String,
    linkedDisplayName: String?,
    linkedEmail: String?,
    authMessage: String?,
    modifier: Modifier = Modifier
) {
    val accountText = StartAccountText.from(
        guestId = guestId,
        displayName = linkedDisplayName,
        email = linkedEmail,
        authMessage = authMessage
    )

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        color = SafeClipSurface.copy(alpha = 0.88f),
        border = BorderStroke(1.dp, SafeClipBorder.copy(alpha = 0.75f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = accountText.idLine,
                modifier = Modifier.weight(1f),
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold
            )
            accountText.statusBadge?.let { badge ->
                Text(
                    text = badge,
                    color = MaterialTheme.colorScheme.error,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun AuthChoiceDialog(
    title: String,
    description: String,
    googleText: String,
    emailText: String,
    authMessage: String?,
    onGoogle: () -> Unit,
    onEmail: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = description,
                    lineHeight = 20.sp
                )
                if (!authMessage.isNullOrBlank()) {
                    Text(
                        text = authMessage,
                        color = SafeClipCyan,
                        fontSize = 13.sp,
                        lineHeight = 18.sp
                    )
                }
                PrimaryActionButton(
                    text = googleText,
                    onClick = onGoogle,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedButton(
                    onClick = onEmail,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, SafeClipBorder)
                ) {
                    Text(text = emailText, fontWeight = FontWeight.Bold)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(text = "나중에")
            }
        }
    )
}

@Composable
private fun EmailSignUpDialog(
    authMessage: String?,
    onSubmit: (String, String, String, String) -> Unit,
    onDismiss: () -> Unit
) {
    var displayName by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordConfirmation by remember { mutableStateOf("") }
    var validationMessage by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = "이메일 회원가입") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "이메일과 6자 이상 비밀번호로 SafeClip 계정을 연결합니다.",
                    lineHeight = 20.sp
                )
                if (!authMessage.isNullOrBlank()) {
                    Text(
                        text = authMessage,
                        color = SafeClipCyan,
                        fontSize = 13.sp,
                        lineHeight = 18.sp
                    )
                }
                if (!validationMessage.isNullOrBlank()) {
                    Text(
                        text = validationMessage.orEmpty(),
                        color = MaterialTheme.colorScheme.error,
                        fontSize = 13.sp,
                        lineHeight = 18.sp
                    )
                }
                OutlinedTextField(
                    value = displayName,
                    onValueChange = { displayName = it },
                    label = { Text(text = "이름") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
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
                OutlinedTextField(
                    value = passwordConfirmation,
                    onValueChange = { passwordConfirmation = it },
                    label = { Text(text = "비밀번호 확인") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val input = EmailAuthInput.create(
                        displayName = displayName,
                        email = email,
                        password = password,
                        passwordConfirmation = passwordConfirmation
                    )
                    if (input.errorMessage != null) {
                        validationMessage = input.errorMessage
                    } else {
                        onSubmit(input.displayName, input.email, input.password, passwordConfirmation)
                    }
                }
            ) {
                Text(text = "가입하기", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = "취소")
            }
        }
    )
}

@Composable
private fun EmailLoginDialog(
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
                Text(
                    text = "가입한 이메일과 비밀번호로 로그인합니다.",
                    lineHeight = 20.sp
                )
                if (!loginMessage.isNullOrBlank()) {
                    Text(
                        text = loginMessage.orEmpty(),
                        color = MaterialTheme.colorScheme.error,
                        fontSize = 13.sp,
                        lineHeight = 18.sp
                    )
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
            TextButton(onClick = onDismiss) {
                Text(text = "취소")
            }
        }
    )
}

@Composable
private fun SafeClipLogoIcon(
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height
        val strokeWidth = width * 0.055f
        val cornerLength = width * 0.22f

        drawRoundRect(
            brush = Brush.linearGradient(
                colors = listOf(Color(0xFF06224A), Color(0xFF00A8C8)),
                start = Offset.Zero,
                end = Offset(width, height)
            ),
            cornerRadius = CornerRadius(width * 0.22f, height * 0.22f)
        )
        drawRoundRect(
            color = SafeClipBorder.copy(alpha = 0.95f),
            size = Size(width, height),
            cornerRadius = CornerRadius(width * 0.22f, height * 0.22f),
            style = Stroke(width = strokeWidth)
        )

        val bracketColor = Color.White.copy(alpha = 0.86f)
        drawLine(bracketColor, Offset(width * 0.18f, height * 0.18f), Offset(width * 0.18f + cornerLength, height * 0.18f), strokeWidth, StrokeCap.Round)
        drawLine(bracketColor, Offset(width * 0.18f, height * 0.18f), Offset(width * 0.18f, height * 0.18f + cornerLength), strokeWidth, StrokeCap.Round)
        drawLine(bracketColor, Offset(width * 0.82f, height * 0.18f), Offset(width * 0.82f - cornerLength, height * 0.18f), strokeWidth, StrokeCap.Round)
        drawLine(bracketColor, Offset(width * 0.82f, height * 0.18f), Offset(width * 0.82f, height * 0.18f + cornerLength), strokeWidth, StrokeCap.Round)
        drawLine(bracketColor, Offset(width * 0.18f, height * 0.82f), Offset(width * 0.18f + cornerLength, height * 0.82f), strokeWidth, StrokeCap.Round)
        drawLine(bracketColor, Offset(width * 0.18f, height * 0.82f), Offset(width * 0.18f, height * 0.82f - cornerLength), strokeWidth, StrokeCap.Round)
        drawLine(bracketColor, Offset(width * 0.82f, height * 0.82f), Offset(width * 0.82f - cornerLength, height * 0.82f), strokeWidth, StrokeCap.Round)
        drawLine(bracketColor, Offset(width * 0.82f, height * 0.82f), Offset(width * 0.82f, height * 0.82f - cornerLength), strokeWidth, StrokeCap.Round)

        val shield = Path().apply {
            moveTo(width * 0.5f, height * 0.2f)
            cubicTo(width * 0.36f, height * 0.28f, width * 0.27f, height * 0.28f, width * 0.25f, height * 0.38f)
            cubicTo(width * 0.23f, height * 0.58f, width * 0.35f, height * 0.72f, width * 0.5f, height * 0.8f)
            cubicTo(width * 0.65f, height * 0.72f, width * 0.77f, height * 0.58f, width * 0.75f, height * 0.38f)
            cubicTo(width * 0.73f, height * 0.28f, width * 0.64f, height * 0.28f, width * 0.5f, height * 0.2f)
            close()
        }
        drawPath(
            path = shield,
            brush = Brush.verticalGradient(listOf(Color.White, Color(0xFFB9DDF4)))
        )
        drawPath(
            path = shield,
            color = Color(0xFF68C8FF).copy(alpha = 0.65f),
            style = Stroke(width = strokeWidth * 0.75f)
        )

        drawCircle(Color(0xFF051126), radius = width * 0.18f, center = Offset(width * 0.5f, height * 0.49f))
        drawCircle(Color(0xFF123E70), radius = width * 0.145f, center = Offset(width * 0.5f, height * 0.49f))
        drawCircle(Color(0xFF159BFF), radius = width * 0.095f, center = Offset(width * 0.5f, height * 0.49f))
        drawCircle(Color(0xFF071A33), radius = width * 0.055f, center = Offset(width * 0.5f, height * 0.49f))
        drawCircle(Color.White.copy(alpha = 0.88f), radius = width * 0.025f, center = Offset(width * 0.45f, height * 0.43f))

        drawLine(
            color = Color.White.copy(alpha = 0.78f),
            start = Offset(width * 0.5f, height * 0.67f),
            end = Offset(width * 0.5f, height * 0.9f),
            strokeWidth = strokeWidth * 0.7f,
            cap = StrokeCap.Round
        )
        drawCircle(SafeClipOrange, radius = width * 0.065f, center = Offset(width * 0.73f, height * 0.28f))
    }
}

@Composable
private fun ConnectionStatusBox(
    title: String,
    connected: Boolean,
    modifier: Modifier = Modifier
) {
    val stateText = if (connected) "ON" else "OFF"
    val stateLabel = if (connected) "연결됨" else "대기"
    val stateColor = if (connected) SafeClipSuccess else SafeClipTextSecondary
    val borderColor = if (connected) SafeClipSuccess else SafeClipBorder
    val backgroundColor = if (connected) SafeClipSuccess.copy(alpha = 0.12f) else SafeClipSurface

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
        color = backgroundColor,
        border = BorderStroke(1.dp, borderColor.copy(alpha = 0.85f))
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = title,
                color = Color.White,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stateLabel,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 12.sp
                )
                Text(
                    text = stateText,
                    color = stateColor,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
