package com.glass.safeclip.data.plate

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.graphics.Rect
import android.os.SystemClock
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.glass.safeclip.data.media.SafeClipMediaPublisher
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.korean.KoreanTextRecognizerOptions
import java.io.ByteArrayOutputStream
import java.io.File
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.delay
import kotlinx.coroutines.tasks.await

class AndroidPlateEventAnalyzer(
    private val cacheDirectory: File,
    private val publisher: SafeClipMediaPublisher,
    private val clock: () -> Long = SystemClock::elapsedRealtime
) : PlateEventAnalyzer, ImageAnalysis.Analyzer {
    private val recognizer = TextRecognition.getClient(KoreanTextRecognizerOptions.Builder().build())
    private val frameWindow = PlateFrameWindow(maxFrames = 9, retentionMs = 8_000)
    private val active = AtomicBoolean(false)
    @Volatile private var lastSampleElapsedMs = Long.MIN_VALUE

    override fun startSession() {
        frameWindow.clear()
        lastSampleElapsedMs = Long.MIN_VALUE
        active.set(true)
    }

    override fun stopSession() {
        active.set(false)
    }

    override fun analyze(image: ImageProxy) {
        try {
            if (!active.get()) return
            val now = clock()
            if (lastSampleElapsedMs != Long.MIN_VALUE && now - lastSampleElapsedMs < SAMPLE_INTERVAL_MS) return
            lastSampleElapsedMs = now
            val source = image.toBitmap()
            val rotated = source.rotate(image.imageInfo.rotationDegrees)
            val scaled = rotated.limitLongEdge(MAX_LONG_EDGE)
            val bytes = ByteArrayOutputStream().use { output ->
                scaled.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, output)
                output.toByteArray()
            }
            if (scaled !== rotated) scaled.recycle()
            if (rotated !== source) rotated.recycle()
            source.recycle()
            frameWindow.add(PlateFrame(now, bytes))
        } finally {
            image.close()
        }
    }

    override suspend fun analyzeEvent(
        eventId: String,
        triggerElapsedMs: Long,
        triggerEpochMs: Long
    ): PlateAnalysisResult {
        delay(AFTER_EVENT_CAPTURE_MS)
        val frames = frameWindow.snapshotAround(triggerElapsedMs, EVENT_RADIUS_MS)
        val matches = mutableListOf<FrameMatch>()
        frames.forEach { frame ->
            val bitmap = BitmapFactory.decodeByteArray(frame.jpegBytes, 0, frame.jpegBytes.size) ?: return@forEach
            try {
                val result = recognizer.process(InputImage.fromBitmap(bitmap, 0)).await()
                result.textBlocks.flatMap { it.lines }.forEach { line ->
                    val bounds = line.boundingBox ?: return@forEach
                    PlateTextParser.extractCandidates(line.text).forEach { text ->
                        val clipped = bounds.clippedTo(bitmap.width, bitmap.height) ?: return@forEach
                        matches += FrameMatch(
                            frame = frame,
                            bounds = clipped,
                            observation = PlateObservation(
                                text = text,
                                frameOffsetMs = frame.elapsedMs - triggerElapsedMs,
                                pixelArea = clipped.width() * clipped.height(),
                                sharpness = edgeSharpness(bitmap, clipped)
                            )
                        )
                    }
                }
            } finally {
                bitmap.recycle()
            }
        }
        val ranked = PlateCandidateRanker.rank(matches.map(FrameMatch::observation)).take(MAX_CANDIDATES)
        val best = ranked.firstOrNull() ?: return PlateAnalysisResult(emptyList())
        val bestMatch = matches.first { it.observation == best.bestObservation }
        val saved = saveImages(eventId, triggerEpochMs, bestMatch)
        return PlateAnalysisResult(
            candidates = ranked.map(RankedPlateCandidate::text),
            contextImageUri = saved.first,
            plateImageUri = saved.second
        )
    }

    private suspend fun saveImages(eventId: String, triggerEpochMs: Long, match: FrameMatch): Pair<String, String> {
        val bitmap = BitmapFactory.decodeByteArray(match.frame.jpegBytes, 0, match.frame.jpegBytes.size)
            ?: error("번호판 분석 프레임을 열지 못했습니다.")
        val contextFile = File(cacheDirectory, "plate_${eventId}_context.jpg")
        val cropFile = File(cacheDirectory, "plate_${eventId}_crop.jpg")
        try {
            contextFile.outputStream().use { bitmap.compress(Bitmap.CompressFormat.JPEG, 94, it) }
            val padded = match.bounds.withPadding(bitmap.width, bitmap.height)
            val crop = Bitmap.createBitmap(bitmap, padded.left, padded.top, padded.width(), padded.height())
            try {
                cropFile.outputStream().use { crop.compress(Bitmap.CompressFormat.JPEG, 96, it) }
            } finally {
                crop.recycle()
            }
            val stamp = FILE_TIME.format(Instant.ofEpochMilli(triggerEpochMs).atZone(SEOUL))
            val contextUri = publisher.publishImage(contextFile, "PLATE_${stamp}_CONTEXT.jpg", "image/jpeg")
            val cropUri = publisher.publishImage(cropFile, "PLATE_${stamp}_CROP.jpg", "image/jpeg")
            return contextUri.toString() to cropUri.toString()
        } finally {
            bitmap.recycle()
            contextFile.delete()
            cropFile.delete()
        }
    }

    override fun close() {
        active.set(false)
        frameWindow.clear()
        recognizer.close()
    }

    private data class FrameMatch(val frame: PlateFrame, val bounds: Rect, val observation: PlateObservation)

    private companion object {
        const val SAMPLE_INTERVAL_MS = 750L
        const val AFTER_EVENT_CAPTURE_MS = 3_000L
        const val EVENT_RADIUS_MS = 3_500L
        const val MAX_LONG_EDGE = 1_920
        const val JPEG_QUALITY = 92
        const val MAX_CANDIDATES = 3
        val SEOUL: ZoneId = ZoneId.of("Asia/Seoul")
        val FILE_TIME: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")
    }
}

private fun Bitmap.rotate(degrees: Int): Bitmap {
    if (degrees == 0) return this
    return Bitmap.createBitmap(this, 0, 0, width, height, Matrix().apply { postRotate(degrees.toFloat()) }, true)
}

private fun Bitmap.limitLongEdge(maxLongEdge: Int): Bitmap {
    val longEdge = maxOf(width, height)
    if (longEdge <= maxLongEdge) return this
    val scale = maxLongEdge.toFloat() / longEdge
    return Bitmap.createScaledBitmap(this, (width * scale).toInt(), (height * scale).toInt(), true)
}

private fun Rect.clippedTo(width: Int, height: Int): Rect? {
    val clipped = Rect(left.coerceAtLeast(0), top.coerceAtLeast(0), right.coerceAtMost(width), bottom.coerceAtMost(height))
    return clipped.takeIf { it.width() > 8 && it.height() > 4 }
}

private fun Rect.withPadding(width: Int, height: Int): Rect {
    val horizontal = (this.width() * 0.35f).toInt()
    val vertical = (this.height() * 0.7f).toInt()
    return Rect(
        (left - horizontal).coerceAtLeast(0),
        (top - vertical).coerceAtLeast(0),
        (right + horizontal).coerceAtMost(width),
        (bottom + vertical).coerceAtMost(height)
    )
}

private fun edgeSharpness(bitmap: Bitmap, bounds: Rect): Double {
    val step = maxOf(1, minOf(bounds.width(), bounds.height()) / 30)
    var total = 0.0
    var count = 0
    var y = bounds.top
    while (y < bounds.bottom - step) {
        var x = bounds.left
        while (x < bounds.right - step) {
            val current = bitmap.getPixel(x, y).luminance()
            total += kotlin.math.abs(current - bitmap.getPixel(x + step, y).luminance())
            total += kotlin.math.abs(current - bitmap.getPixel(x, y + step).luminance())
            count += 2
            x += step
        }
        y += step
    }
    return if (count == 0) 0.0 else total / count
}

private fun Int.luminance(): Double {
    val red = (this shr 16) and 0xff
    val green = (this shr 8) and 0xff
    val blue = this and 0xff
    return red * 0.299 + green * 0.587 + blue * 0.114
}
