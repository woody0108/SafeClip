package com.glass.safeclip.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.glass.safeclip.ui.theme.SafeClipBorder
import com.glass.safeclip.ui.theme.SafeClipPrimaryBlue
import com.glass.safeclip.ui.theme.SafeClipOrange
import com.glass.safeclip.ui.theme.SafeClipSurface
import com.glass.safeclip.ui.theme.SafeClipTextPrimary
import com.glass.safeclip.ui.theme.SafeClipTextSecondary

@Composable
fun PrimaryActionButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = SafeClipOrange,
            contentColor = MaterialTheme.colorScheme.onTertiary
        ),
        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 14.dp)
    ) {
        Text(text = text, fontWeight = FontWeight.Bold, fontSize = 17.sp)
    }
}

@Composable
fun SecondaryActionButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, SafeClipBorder),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Text(text = text, fontWeight = FontWeight.SemiBold)
    }
}

enum class SegmentedTabTone {
    Active,
    Inactive
}

data class SegmentedTabVisual(
    val tone: SegmentedTabTone,
    val containerAlpha: Float,
    val contentAlpha: Float,
    val boldText: Boolean
) {
    companion object {
        fun from(selected: Boolean): SegmentedTabVisual {
            return if (selected) {
                SegmentedTabVisual(
                    tone = SegmentedTabTone.Active,
                    containerAlpha = 1f,
                    contentAlpha = 1f,
                    boldText = true
                )
            } else {
                SegmentedTabVisual(
                    tone = SegmentedTabTone.Inactive,
                    containerAlpha = 0.72f,
                    contentAlpha = 0.72f,
                    boldText = false
                )
            }
        }
    }
}

@Composable
fun SegmentedTabButton(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val visual = SegmentedTabVisual.from(selected)
    OutlinedButton(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, if (selected) SafeClipPrimaryBlue else SafeClipBorder),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = if (selected) {
                SafeClipPrimaryBlue.copy(alpha = visual.containerAlpha)
            } else {
                SafeClipSurface.copy(alpha = visual.containerAlpha)
            },
            contentColor = if (selected) {
                SafeClipTextPrimary.copy(alpha = visual.contentAlpha)
            } else {
                SafeClipTextSecondary.copy(alpha = visual.contentAlpha)
            }
        ),
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 11.dp)
    ) {
        Text(
            text = text,
            fontWeight = if (visual.boldText) FontWeight.Bold else FontWeight.SemiBold
        )
    }
}
