package com.glass.safeclip.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
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

private val LightColorScheme = lightColorScheme(
    primary = Purple40,
    secondary = PurpleGrey40,
    tertiary = Pink40
)

@Composable
fun SafeClipTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor -> SafeClipDarkColorScheme
        darkTheme -> SafeClipDarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
