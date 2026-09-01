package com.glass.safeclip.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.glass.safeclip.R

private val SafeClipFontFamily = FontFamily(
    Font(R.font.noto_sans_kr_regular, FontWeight.Normal),
    Font(R.font.noto_sans_kr_medium, FontWeight.Medium),
    Font(R.font.noto_sans_kr_medium, FontWeight.SemiBold),
    Font(R.font.noto_sans_kr_bold, FontWeight.Bold)
)

private val BaseTypography = Typography()

private fun TextStyle.withSafeClipFont(): TextStyle = copy(
    fontFamily = SafeClipFontFamily,
    letterSpacing = 0.sp
)

val Typography = Typography(
    displayLarge = BaseTypography.displayLarge.withSafeClipFont(),
    displayMedium = BaseTypography.displayMedium.withSafeClipFont(),
    displaySmall = BaseTypography.displaySmall.withSafeClipFont(),
    headlineLarge = BaseTypography.headlineLarge.withSafeClipFont(),
    headlineMedium = BaseTypography.headlineMedium.withSafeClipFont(),
    headlineSmall = BaseTypography.headlineSmall.withSafeClipFont(),
    titleLarge = BaseTypography.titleLarge.withSafeClipFont(),
    titleMedium = BaseTypography.titleMedium.withSafeClipFont(),
    titleSmall = BaseTypography.titleSmall.withSafeClipFont(),
    bodyLarge = BaseTypography.bodyLarge.withSafeClipFont(),
    bodyMedium = BaseTypography.bodyMedium.withSafeClipFont(),
    bodySmall = BaseTypography.bodySmall.withSafeClipFont(),
    labelLarge = BaseTypography.labelLarge.withSafeClipFont(),
    labelMedium = BaseTypography.labelMedium.withSafeClipFont(),
    labelSmall = BaseTypography.labelSmall.withSafeClipFont()
)
