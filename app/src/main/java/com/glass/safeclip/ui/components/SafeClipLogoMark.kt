package com.glass.safeclip.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import com.glass.safeclip.ui.theme.SafeClipBorder
import com.glass.safeclip.ui.theme.SafeClipOrange

@Composable
fun SafeClipLogoMark(
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
        drawPath(shield, Brush.verticalGradient(listOf(Color.White, Color(0xFFB9DDF4))))
        drawPath(shield, Color(0xFF68C8FF).copy(alpha = 0.65f), style = Stroke(width = strokeWidth * 0.75f))

        drawCircle(Color(0xFF051126), radius = width * 0.18f, center = Offset(width * 0.5f, height * 0.49f))
        drawCircle(Color(0xFF123E70), radius = width * 0.145f, center = Offset(width * 0.5f, height * 0.49f))
        drawCircle(Color(0xFF159BFF), radius = width * 0.095f, center = Offset(width * 0.5f, height * 0.49f))
        drawCircle(Color(0xFF071A33), radius = width * 0.055f, center = Offset(width * 0.5f, height * 0.49f))
        drawCircle(Color.White.copy(alpha = 0.88f), radius = width * 0.025f, center = Offset(width * 0.45f, height * 0.43f))
        drawLine(Color.White.copy(alpha = 0.78f), Offset(width * 0.5f, height * 0.67f), Offset(width * 0.5f, height * 0.9f), strokeWidth * 0.7f, StrokeCap.Round)
        drawCircle(SafeClipOrange, radius = width * 0.065f, center = Offset(width * 0.73f, height * 0.28f))
    }
}
