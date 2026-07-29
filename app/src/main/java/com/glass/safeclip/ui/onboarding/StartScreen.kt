package com.glass.safeclip.ui.onboarding

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import com.glass.safeclip.ui.components.GlassPanel
import com.glass.safeclip.ui.components.PrimaryActionButton
import com.glass.safeclip.ui.components.SafeClipScaffold
import com.glass.safeclip.ui.theme.SafeClipBorder
import com.glass.safeclip.ui.theme.SafeClipCyan
import com.glass.safeclip.ui.theme.SafeClipOrange
import com.glass.safeclip.ui.theme.SafeClipSuccess
import com.glass.safeclip.ui.theme.SafeClipSurface
import com.glass.safeclip.ui.theme.SafeClipTextSecondary

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
    guestId: String,
    linkedDisplayName: String?,
    linkedEmail: String?,
    authMessage: String?,
    onGoogleSignUp: () -> Unit,
    onEmailSignUp: (String, String) -> Unit,
    onStart: () -> Unit
) {
    var showSignUpDialog by remember { mutableStateOf(false) }
    var showEmailDialog by remember { mutableStateOf(false) }

    if (showSignUpDialog) {
        SignUpDialog(
            authMessage = authMessage,
            onGoogleSignUp = {
                showSignUpDialog = false
                onGoogleSignUp()
            },
            onEmailSignUp = {
                showSignUpDialog = false
                showEmailDialog = true
            },
            onDismiss = { showSignUpDialog = false }
        )
    }
    if (showEmailDialog) {
        EmailSignUpDialog(
            authMessage = authMessage,
            onSubmit = { email, password ->
                showEmailDialog = false
                onEmailSignUp(email, password)
            },
            onDismiss = { showEmailDialog = false }
        )
    }

    SafeClipScaffold {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(22.dp)) {
                Spacer(modifier = Modifier.height(28.dp))
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        SafeClipLogoIcon(modifier = Modifier.size(46.dp))
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

                GlassPanel(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "USB-C 리더기와 microSD 카드를 연결한 뒤 영상을 확인하고 제출하세요.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 22.sp
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        ConnectionStatusBox(
                            title = "USB-C 연결",
                            connected = false,
                            modifier = Modifier
                                .weight(1f)
                                .height(86.dp)
                        )
                        ConnectionStatusBox(
                            title = "microSD 인식",
                            connected = false,
                            modifier = Modifier
                                .weight(1f)
                                .height(86.dp)
                        )
                    }
                }
            }

            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                GuestIdentityRow(
                    guestId = guestId,
                    linkedDisplayName = linkedDisplayName,
                    linkedEmail = linkedEmail,
                    onSignUp = { showSignUpDialog = true }
                )
                if (!authMessage.isNullOrBlank()) {
                    Text(
                        text = authMessage,
                        color = SafeClipCyan,
                        fontSize = 13.sp,
                        lineHeight = 18.sp,
                        textAlign = TextAlign.Center
                    )
                }
                PrimaryActionButton(
                    text = "시작하기",
                    onClick = onStart,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(64.dp)
                )
                Text(
                    text = "원본 영상은 사용자가 선택할 때만 처리됩니다.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 13.sp
                )
            }
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
private fun GuestIdentityRow(
    guestId: String,
    linkedDisplayName: String?,
    linkedEmail: String?,
    onSignUp: () -> Unit,
    modifier: Modifier = Modifier
) {
    val accountText = StartAccountText.from(
        guestId = guestId,
        displayName = linkedDisplayName,
        email = linkedEmail
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
            OutlinedButton(
                onClick = onSignUp,
                enabled = accountText.signupEnabled,
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(1.dp, SafeClipCyan.copy(alpha = 0.85f)),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = SafeClipCyan)
            ) {
                Text(text = accountText.actionText, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun SignUpDialog(
    authMessage: String?,
    onGoogleSignUp: () -> Unit,
    onEmailSignUp: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = "계정 연동") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "Guest ID를 Google 또는 이메일 계정에 연결하면 나중에 다시 설치해도 제출 내역을 찾기 쉬워집니다.",
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
                    text = "Google로 계속하기",
                    onClick = onGoogleSignUp,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedButton(
                    onClick = onEmailSignUp,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, SafeClipBorder)
                ) {
                    Text(text = "이메일로 가입하기", fontWeight = FontWeight.Bold)
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
    onSubmit: (String, String) -> Unit,
    onDismiss: () -> Unit
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

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
            TextButton(onClick = { onSubmit(email, password) }) {
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
