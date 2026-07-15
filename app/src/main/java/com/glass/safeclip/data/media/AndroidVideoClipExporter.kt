package com.glass.safeclip.data.media

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.annotation.OptIn
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.transformer.Composition
import androidx.media3.transformer.EditedMediaItem
import androidx.media3.transformer.ExportException
import androidx.media3.transformer.ExportResult
import androidx.media3.transformer.Transformer
import com.glass.safeclip.data.file.LastSelectedEventFolderStore
import com.glass.safeclip.domain.model.VideoCandidate
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext

@OptIn(UnstableApi::class)
class AndroidVideoClipExporter(
    private val context: Context,
    eventFolderStore: LastSelectedEventFolderStore? = null
) {
    private val eventFolder = eventFolderStore?.let { SafeClipEventFolder(context, it) }

    suspend fun exportClip(
        video: VideoCandidate,
        selection: VideoClipSelection
    ): VideoClipExportResult {
        require(selection.isComplete) { "저장할 시작/끝 구간을 먼저 지정해주세요." }

        val startMs = selection.startMs!!
        val endMs = selection.endMs!!
        val outputFileName = ClipFileName.forVideo(video.displayName, startMs, endMs)
        val sourceCopy = withContext(Dispatchers.IO) {
            createTempSourceCopy(video)
        }
        val tempOutput = createTempOutputFile(outputFileName)

        return try {
            val inputItem = MediaItem.Builder()
                .setUri(Uri.fromFile(sourceCopy))
                .setClippingConfiguration(
                    MediaItem.ClippingConfiguration.Builder()
                        .setStartPositionMs(startMs)
                        .setEndPositionMs(endMs)
                        .build()
                )
                .build()
            val editedItem = EditedMediaItem.Builder(inputItem).build()

            // Transformer는 생성/시작/취소가 같은 스레드에서 일어나야 하므로 메인 스레드에서만 다룬다.
            withContext(Dispatchers.Main.immediate) {
                runTransformer(editedItem, tempOutput)
            }

            val published = withContext(Dispatchers.IO) {
                publishClip(tempOutput, outputFileName)
            }

            VideoClipExportResult.from(
                outputFile = published.outputFile,
                outputUriString = published.outputUriString,
                savedDisplayPath = published.displayPath,
                originalVideo = video,
                selection = selection
            )
        } finally {
            deleteTempFiles(sourceCopy, tempOutput)
        }
    }

    private suspend fun runTransformer(
        editedItem: EditedMediaItem,
        outputFile: File
    ) {
        suspendCancellableCoroutine { continuation ->
            val transformer = Transformer.Builder(context)
                .addListener(object : Transformer.Listener {
                    override fun onCompleted(composition: Composition, exportResult: ExportResult) {
                        if (continuation.isActive) {
                            continuation.resume(Unit)
                        }
                    }

                    override fun onError(
                        composition: Composition,
                        exportResult: ExportResult,
                        exportException: ExportException
                    ) {
                        if (continuation.isActive) {
                            continuation.resumeWithException(
                                IllegalStateException(
                                    "클립 저장 중 영상 변환에 실패했습니다. MP4가 아닌 블랙박스 영상은 기기에서 자르기 저장을 지원하지 않을 수 있습니다.",
                                    exportException
                                )
                            )
                        }
                    }
                })
                .build()

            continuation.invokeOnCancellation {
                transformer.cancel()
            }

            try {
                transformer.start(editedItem, outputFile.absolutePath)
            } catch (error: Throwable) {
                if (continuation.isActive) {
                    continuation.resumeWithException(error)
                }
            }
        }
    }

    private fun createTempSourceCopy(video: VideoCandidate): File {
        val sourceFileName = ClipTempFileName.forSourceCopy(video.displayName)
        val sourceCopy = createUniqueTempFile(sourceFileName)
        val sourceUri = Uri.parse(video.uriString)
        context.contentResolver.openInputStream(sourceUri)?.use { input ->
            FileOutputStream(sourceCopy).use { output ->
                input.copyTo(output)
            }
        } ?: error("원본 영상을 임시 파일로 복사하지 못했습니다.")
        return sourceCopy
    }

    private fun createTempOutputFile(fileName: String): File {
        return createUniqueTempFile(fileName)
    }

    private fun createUniqueTempFile(fileName: String): File {
        val clipDirectory = File(context.cacheDir ?: context.filesDir, "safeclip-clip-temp")
        clipDirectory.mkdirs()

        val candidate = File(clipDirectory, fileName)
        if (!candidate.exists()) return candidate

        val nameWithoutExtension = fileName.substringBeforeLast('.')
        val extension = fileName.substringAfterLast('.', "mp4")
        var index = 2
        while (true) {
            val next = File(clipDirectory, "${nameWithoutExtension}_$index.$extension")
            if (!next.exists()) return next
            index += 1
        }
    }

    private fun publishClip(tempFile: File, fileName: String): PublishedClip {
        publishClipToEventFolder(tempFile, fileName)?.let { return it }

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            publishClipToMediaStore(tempFile, fileName)
        } else {
            publishClipToAppVisibleFolder(tempFile, fileName)
        }
    }

    private fun publishClipToEventFolder(tempFile: File, fileName: String): PublishedClip? {
        val folder = eventFolder?.loadOrCreate() ?: return null
        val clipFile = folder.createFile("video/mp4", fileName) ?: return null
        context.contentResolver.openOutputStream(clipFile.uri)?.use { output ->
            FileInputStream(tempFile).use { input ->
                input.copyTo(output)
            }
        } ?: return null

        val displayPath = SafeClipMediaSaveLocation.eventFolderDisplayPath(fileName)
        return PublishedClip(
            outputFile = File(displayPath),
            outputUriString = clipFile.uri.toString(),
            displayPath = displayPath
        )
    }

    private fun publishClipToMediaStore(tempFile: File, fileName: String): PublishedClip {
        val resolver = context.contentResolver
        val values = ContentValues().apply {
            put(MediaStore.Video.Media.DISPLAY_NAME, fileName)
            put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
            put(MediaStore.MediaColumns.RELATIVE_PATH, SafeClipMediaSaveLocation.videoClipRelativePath)
            put(MediaStore.MediaColumns.IS_PENDING, 1)
        }
        val clipUri = resolver.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, values)
            ?: error("클립 영상을 저장할 위치를 만들지 못했습니다.")

        try {
            resolver.openOutputStream(clipUri)?.use { output ->
                FileInputStream(tempFile).use { input ->
                    input.copyTo(output)
                }
            } ?: error("클립 영상을 저장하지 못했습니다.")

            values.clear()
            values.put(MediaStore.MediaColumns.IS_PENDING, 0)
            resolver.update(clipUri, values, null, null)

            val displayPath = SafeClipMediaSaveLocation.videoClipDisplayPath(fileName)
            return PublishedClip(
                outputFile = File(displayPath),
                outputUriString = clipUri.toString(),
                displayPath = displayPath
            )
        } catch (error: Throwable) {
            resolver.delete(clipUri, null, null)
            throw error
        }
    }

    private fun publishClipToAppVisibleFolder(tempFile: File, fileName: String): PublishedClip {
        val baseDirectory = context.getExternalFilesDir(Environment.DIRECTORY_MOVIES) ?: context.filesDir
        val clipDirectory = File(baseDirectory, SafeClipMediaSaveLocation.albumName)
        clipDirectory.mkdirs()
        val outputFile = File(clipDirectory, fileName)
        FileInputStream(tempFile).use { input ->
            FileOutputStream(outputFile).use { output ->
                input.copyTo(output)
            }
        }
        return PublishedClip(
            outputFile = outputFile,
            outputUriString = Uri.fromFile(outputFile).toString(),
            displayPath = outputFile.absolutePath
        )
    }

    private fun deleteTempFiles(vararg files: File) {
        files.forEach { it.delete() }
    }

    private data class PublishedClip(
        val outputFile: File,
        val outputUriString: String?,
        val displayPath: String
    )
}
