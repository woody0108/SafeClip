package com.glass.safeclip.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val SafeClipDarkColorScheme = darkColorScheme(
    primary = SafeClipPrimaryBlue,
    secondary = SafeClipCyan,
    tertiary = SafeClipOrange,
    background = SafeClipBackground,
    surface = SafeClipSurface,
    surfaceVariant = SafeClipSurfaceHigh,
    error = SafeClipError,
    onPrimary = Color.White,
    onSecondary = SafeClipBackground,
    onTertiary = Color.White,
    onBackground = SafeClipTextPrimary,
    onSurface = SafeClipTextPrimary,
    onSurfaceVariant = SafeClipTextSecondary,
    onError = Color.White
)

@Composable
fun SafeClipTheme(
    @Suppress("UNUSED_PARAMETER") darkTheme: Boolean = true,
    @Suppress("UNUSED_PARAMETER") dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    MaterialTheme(
        // SafeClip 화면은 검푸른 고정 배경을 사용하므로 기기 테마와 무관하게
        // 같은 고대비 색상표를 적용해야 텍스트가 항상 읽힙니다.
        colorScheme = SafeClipDarkColorScheme,
        typography = Typography,
        content = content
    )
}
