package com.glass.safeclip.data.media

import android.graphics.Color
import android.graphics.Typeface
import android.text.Spannable
import android.text.SpannableString
import android.text.style.BackgroundColorSpan
import android.text.style.ForegroundColorSpan
import android.text.style.StyleSpan
import androidx.annotation.OptIn
import androidx.media3.common.OverlaySettings
import androidx.media3.common.util.UnstableApi
import androidx.media3.effect.StaticOverlaySettings
import androidx.media3.effect.TextOverlay

@OptIn(UnstableApi::class)
class EventTimestampOverlay(
    private val startEpochMillis: Long
) : TextOverlay() {
    private val settings = StaticOverlaySettings.Builder()
        .setBackgroundFrameAnchor(0.96f, -0.92f)
        .setOverlayFrameAnchor(1f, -1f)
        .setScale(0.42f, 0.42f)
        .build()

    override fun getText(presentationTimeUs: Long): SpannableString {
        val text = SpannableString(
            " ${EventVideoTimestamp.format(startEpochMillis + presentationTimeUs / 1_000L)} "
        )
        text.setSpan(ForegroundColorSpan(Color.WHITE), 0, text.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        text.setSpan(BackgroundColorSpan(Color.argb(176, 0, 0, 0)), 0, text.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        text.setSpan(StyleSpan(Typeface.BOLD), 0, text.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        return text
    }

    override fun getOverlaySettings(presentationTimeUs: Long): OverlaySettings = settings
}
