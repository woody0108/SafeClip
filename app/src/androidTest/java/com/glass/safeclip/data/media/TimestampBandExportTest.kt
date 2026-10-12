package com.glass.safeclip.data.media

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.media.MediaMetadataRetriever
import android.net.Uri
import androidx.annotation.OptIn
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.transformer.Composition
import androidx.media3.transformer.EditedMediaItem
import androidx.media3.transformer.ExportException
import androidx.media3.transformer.ExportResult
import androidx.media3.transformer.Transformer
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.glass.safeclip.data.recording.EventClipPart
import com.glass.safeclip.data.recording.EventClipPlan
import com.glass.safeclip.data.recording.RecordingTimeAnchor
import com.glass.safeclip.data.recording.RollingSegmentRepository
import java.io.File
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@OptIn(UnstableApi::class)
@RunWith(AndroidJUnit4::class)
class TimestampBandExportTest {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val directory = File(context.cacheDir, "timestamp-test-${System.nanoTime()}").apply { mkdirs() }

    @After
    fun cleanup() {
        directory.deleteRecursively()
    }

    @Test
    fun drawsOnlyInsideWhiteBandAndUpdatesOncePerSecond() {
        val padding = TimestampBandPadding()
        val size = padding.configure(1920, 1080)
        assertEquals(1160, size.height)
        val overlay = EventTimestampOverlay(1_787_725_421_000, padding)
        try {
            val first = overlay.getBitmap(0)
            assertEquals(1920, first.width)
            assertEquals(80, first.height)
            assertEquals(Color.WHITE, first.getPixel(1919, 79))
            assertTrue((0 until first.width).any { x ->
                (0 until first.height).any { y -> Color.red(first.getPixel(x, y)) < 80 }
            })
            val pixels = IntArray(first.width * first.height)
            first.getPixels(pixels, 0, first.width, 0, 0, first.width, first.height)
            val generation = first.generationId
            assertSame(first, overlay.getBitmap(900_000))
            assertEquals(generation, first.generationId)
            val next = overlay.getBitmap(1_000_000)
            val updated = IntArray(next.width * next.height)
            next.getPixels(updated, 0, next.width, 0, 0, next.width, next.height)
            assertFalse(pixels.contentEquals(updated))
        } finally {
            overlay.release()
        }
    }

    @Test
    fun exportedVideoAndCapturedPhotoKeepContentAndExactlyOneBand() = runBlocking<Unit> {
        withTimeout(120_000) {
            val (repository, plan) = fixture()
            val source = File(java.net.URI(plan.parts.single().segment.uriString))
            val sourceBefore = source.readBytes()
            val publisher = TestPublisher()
            val uri = Media3EventClipAssembler(context, repository, publisher)
                .assemble(plan, "event.mp4", null).getOrThrow()
            assertEquals(setOf("event.mp4", "event_TIMESTAMP.mp4"), publisher.files.keys)
            assertArrayEquals(sourceBefore, source.readBytes())
            assertFalse(repository.segments().single().isProtected)
            readFrame(publisher.files.getValue("event.mp4")).also { frame ->
                assertEquals(240, frame.height)
                frame.recycle()
            }
            readFrame(publisher.files.getValue("event_TIMESTAMP.mp4")).also { frame ->
                assertBandAndContent(frame)
                frame.recycle()
            }
            AndroidFrameCaptureStore(context, mediaPublisher = publisher)
                .captureFrame(uri, "event_TIMESTAMP.mp4", 500)
            val photo = publisher.files.values.single { it.extension == "jpg" }
            BitmapFactory.decodeFile(photo.absolutePath).also { frame ->
                assertBandAndContent(frame)
                frame.recycle()
            }
        }
    }

    @Test
    fun unknownTimeSavesOnlyUnstampedOriginal() = runBlocking<Unit> {
        withTimeout(120_000) {
            val (repository, plan) = fixture()
            val publisher = TestPublisher()
            Media3EventClipAssembler(context, repository, publisher)
                .assemble(plan.copy(timeAnchor = null), "unknown.mp4", null).getOrThrow()
            assertEquals(setOf("unknown.mp4"), publisher.files.keys)
            readFrame(publisher.files.getValue("unknown.mp4")).also { frame ->
                assertEquals(240, frame.height)
                frame.recycle()
            }
        }
    }

    @Test
    fun timestampPublishFailureKeepsTheOriginalAndReleasesSegments() = runBlocking<Unit> {
        withTimeout(120_000) {
            val (repository, plan) = fixture()
            val publisher = TestPublisher(failTimestamp = true)
            val result = Media3EventClipAssembler(context, repository, publisher)
                .assemble(plan, "event.mp4", null)
            assertTrue(result.isFailure)
            assertTrue(result.exceptionOrNull()!!.message!!.contains("원본 영상은 저장"))
            assertEquals(setOf("event.mp4"), publisher.files.keys)
            assertTrue(publisher.files.getValue("event.mp4").length() > 0)
            assertFalse(repository.segments().single().isProtected)
        }
    }

    private fun assertBandAndContent(bitmap: Bitmap) {
        assertEquals(320, bitmap.width)
        assertEquals(264, bitmap.height)
        val white = bitmap.getPixel(310, 5)
        assertTrue(Color.red(white) > 220 && Color.green(white) > 220 && Color.blue(white) > 220)
        // 원본의 네 모서리 색상이 남아 있어야 한다. 시간 띠가 영상 위를 덮으면 실패한다.
        for (y in listOf(30, 255)) {
            val left = bitmap.getPixel(8, y)
            val right = bitmap.getPixel(310, y)
            assertTrue(Color.blue(left) > Color.red(left) + 100)
            assertTrue(Color.green(right) > Color.red(right) + 100)
        }
    }

    private fun readFrame(file: File): Bitmap {
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(file.absolutePath)
            requireNotNull(retriever.getFrameAtTime(500_000, MediaMetadataRetriever.OPTION_CLOSEST))
        } finally {
            retriever.release()
        }
    }

    private suspend fun fixture(): Pair<RollingSegmentRepository, EventClipPlan> {
        val image = File(directory, "source.jpg")
        val bitmap = Bitmap.createBitmap(320, 240, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(Color.RED)
        val paint = Paint()
        paint.color = Color.BLUE
        canvas.drawRect(0f, 0f, 32f, 240f, paint)
        paint.color = Color.GREEN
        canvas.drawRect(288f, 0f, 320f, 240f, paint)
        image.outputStream().use { bitmap.compress(Bitmap.CompressFormat.JPEG, 100, it) }
        bitmap.recycle()
        val repository = RollingSegmentRepository(File(directory, "segments"))
        val pending = repository.createPending(10_000)
        pending.file.delete()
        val item = EditedMediaItem.Builder(
            MediaItem.Builder().setUri(Uri.fromFile(image)).setImageDurationMs(2_000).build()
        ).setFrameRate(30).build()
        withContext(Dispatchers.Main.immediate) {
            suspendCancellableCoroutine<Unit> { continuation ->
                val transformer = Transformer.Builder(context)
                    .addListener(object : Transformer.Listener {
                        override fun onCompleted(composition: Composition, exportResult: ExportResult) {
                            if (continuation.isActive) continuation.resume(Unit)
                        }
                        override fun onError(composition: Composition, exportResult: ExportResult, exportException: ExportException) {
                            if (continuation.isActive) continuation.resumeWithException(exportException)
                        }
                    }).build()
                continuation.invokeOnCancellation { transformer.cancel() }
                transformer.start(item, pending.file.absolutePath)
            }
        }
        val segment = repository.finalize(pending, 12_000)
        return repository to EventClipPlan(
            10_000, 12_000, listOf(EventClipPart(segment, 0, 2_000)), false,
            RecordingTimeAnchor(10_000, 1_787_725_421_000)
        )
    }

    private inner class TestPublisher(private val failTimestamp: Boolean = false) : SafeClipMediaPublisher {
        val files = linkedMapOf<String, File>()
        override suspend fun publishVideo(source: File, displayName: String): Uri {
            if (failTimestamp && displayName.contains("_TIMESTAMP")) error("Simulated storage failure")
            return publish(source, displayName)
        }
        override suspend fun publishImage(source: File, displayName: String, mimeType: String): Uri = publish(source, displayName)
        private fun publish(source: File, displayName: String): Uri {
            val file = File(directory, displayName)
            source.copyTo(file)
            files[displayName] = file
            return Uri.fromFile(file)
        }
    }
}
