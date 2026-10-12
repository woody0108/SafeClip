package com.glass.safeclip.data.media

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import androidx.annotation.OptIn
import androidx.media3.common.OverlaySettings
import androidx.media3.common.util.UnstableApi
import androidx.media3.effect.BitmapOverlay
import androidx.media3.effect.StaticOverlaySettings

@OptIn(UnstableApi::class)
class EventTimestampOverlay(
    startEpochMillis: Long,
    private val padding: TimestampBandPadding
) : BitmapOverlay() {
    private val clock = EventTimestampClock(startEpochMillis)
    private val settings = StaticOverlaySettings.Builder()
        .setBackgroundFrameAnchor(-1f, 1f)
        .setOverlayFrameAnchor(-1f, 1f)
        .build()
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.BLACK
        typeface = Typeface.create("sans-serif", Typeface.NORMAL)
    }
    private var bitmap: Bitmap? = null
    private var renderedSecond: Long? = null

    override fun getBitmap(presentationTimeUs: Long): Bitmap {
        val layout = padding.layout
        var band = bitmap
        if (band == null || band.width != layout.width || band.height != layout.bandHeight) {
            band?.recycle()
            band = Bitmap.createBitmap(layout.width, layout.bandHeight, Bitmap.Config.ARGB_8888)
            bitmap = band
            renderedSecond = null
        }
        val epochMillis = clock.epochMillisAt(presentationTimeUs)
        val second = Math.floorDiv(epochMillis, 1_000L)
        if (renderedSecond != second) {
            band.eraseColor(Color.WHITE)
            val canvas = Canvas(band)
            val text = "촬영일시: ${EventVideoTimestamp.format(epochMillis)}"
            val inset = layout.bandHeight * 0.2f
            paint.textSize = layout.bandHeight * 0.52f
            val availableWidth = layout.width - inset * 2
            val textWidth = paint.measureText(text)
            if (textWidth > availableWidth) paint.textSize *= availableWidth / textWidth
            val metrics = paint.fontMetrics
            val baseline = (layout.bandHeight - metrics.ascent - metrics.descent) / 2f
            canvas.drawText(text, inset, baseline, paint)
            renderedSecond = second
        }
        return band
    }

    override fun getOverlaySettings(presentationTimeUs: Long): OverlaySettings = settings

    override fun release() {
        try {
            super.release()
        } finally {
            bitmap?.recycle()
            bitmap = null
            renderedSecond = null
        }
    }
}
