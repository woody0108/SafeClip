package com.glass.safeclip.data.submission

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.DataOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.UUID

class NasSubmissionUploadClient(
    private val context: Context,
    private val uploadUrl: String,
    private val uploadKey: String
) {
    fun isConfigured(): Boolean {
        return uploadUrl.isNotBlank() && uploadKey.isNotBlank()
    }

    suspend fun uploadAll(
        submissionId: String,
        submitterLabel: String,
        attachments: List<SubmissionAttachment>,
        onProgress: suspend (NasSubmissionUploadProgress) -> Unit = {}
    ): List<SubmissionAttachment> {
        if (!isConfigured()) {
            throw IllegalStateException("NAS 업로드 주소와 키가 설정되지 않았습니다.")
        }
        return withContext(Dispatchers.IO) {
            attachments.mapIndexed { index, attachment ->
                attachment.withUploadResponse(
                    uploadOne(
                        submissionId = submissionId,
                        submitterLabel = submitterLabel,
                        attachment = attachment,
                        fileIndex = index + 1,
                        totalFiles = attachments.size,
                        onProgress = onProgress
                    )
                )
            }
        }
    }

    private suspend fun uploadOne(
        submissionId: String,
        submitterLabel: String,
        attachment: SubmissionAttachment,
        fileIndex: Int,
        totalFiles: Int,
        onProgress: suspend (NasSubmissionUploadProgress) -> Unit
    ): NasSubmissionUploadResponse {
        val boundary = "SafeClip-${UUID.randomUUID()}"
        val fileSizeBytes = attachment.sizeBytes
        val connection = (URL(uploadUrl).openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            doOutput = true
            doInput = true
            connectTimeout = 20_000
            readTimeout = 120_000
            setRequestProperty("X-SafeClip-Upload-Key", uploadKey)
            setRequestProperty("Content-Type", "multipart/form-data; boundary=$boundary")
            if (fileSizeBytes != null && fileSizeBytes > 0L) {
                setFixedLengthStreamingMode(
                    NasSubmissionMultipartBody.contentLength(
                        boundary = boundary,
                        submissionId = submissionId,
                        submitterLabel = submitterLabel,
                        originalFileName = attachment.displayName,
                        deviceLabel = "android",
                        mimeType = attachment.mimeType,
                        fileSizeBytes = fileSizeBytes
                    )
                )
            }
        }

        try {
            DataOutputStream(connection.outputStream).use { output ->
                output.write(NasSubmissionMultipartBody.textPart(boundary, "submission_id", submissionId))
                output.write(NasSubmissionMultipartBody.textPart(boundary, "submitter_label", submitterLabel))
                output.write(NasSubmissionMultipartBody.textPart(boundary, "original_file_name", attachment.displayName))
                output.write(NasSubmissionMultipartBody.textPart(boundary, "device_label", "android"))
                writeFilePart(
                    output = output,
                    boundary = boundary,
                    attachment = attachment,
                    fileIndex = fileIndex,
                    totalFiles = totalFiles,
                    onProgress = onProgress
                )
                output.write(NasSubmissionMultipartBody.closingBoundary(boundary))
                output.flush()
            }

            val responseText = if (connection.responseCode in 200..299) {
                connection.inputStream.bufferedReader().use { it.readText() }
            } else {
                connection.errorStream?.bufferedReader()?.use { it.readText() }
                    ?: """{"ok":false,"error":"NAS 업로드 실패: HTTP ${connection.responseCode}"}"""
            }
            return NasSubmissionUploadResponse.parse(responseText)
        } finally {
            connection.disconnect()
        }
    }

    private suspend fun writeFilePart(
        output: DataOutputStream,
        boundary: String,
        attachment: SubmissionAttachment,
        fileIndex: Int,
        totalFiles: Int,
        onProgress: suspend (NasSubmissionUploadProgress) -> Unit
    ) {
        output.write(
            NasSubmissionMultipartBody.fileHeader(
                boundary = boundary,
                fileName = attachment.displayName,
                mimeType = attachment.mimeType
            )
        )
        context.contentResolver.openInputStream(Uri.parse(attachment.uriString))?.use { input ->
            val buffer = ByteArray(64 * 1024)
            var sent = 0L
            var lastPercent: Int? = null
            onProgress(
                NasSubmissionUploadProgress(
                    fileIndex = fileIndex,
                    totalFiles = totalFiles,
                    fileName = attachment.displayName,
                    bytesSent = sent,
                    totalBytes = attachment.sizeBytes
                )
            )
            while (true) {
                val read = input.read(buffer)
                if (read <= 0) break
                output.write(buffer, 0, read)
                sent += read
                val progress = NasSubmissionUploadProgress(
                    fileIndex = fileIndex,
                    totalFiles = totalFiles,
                    fileName = attachment.displayName,
                    bytesSent = sent,
                    totalBytes = attachment.sizeBytes
                )
                val percent = progress.currentFilePercent
                if (percent == null || percent != lastPercent) {
                    lastPercent = percent
                    onProgress(progress)
                }
            }
        } ?: throw IllegalStateException("첨부 파일을 열 수 없습니다: ${attachment.displayName}")
        output.writeBytes("\r\n")
    }
}
