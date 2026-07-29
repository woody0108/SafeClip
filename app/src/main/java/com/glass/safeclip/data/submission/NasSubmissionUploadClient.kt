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
        attachments: List<SubmissionAttachment>
    ): List<SubmissionAttachment> {
        if (!isConfigured()) {
            throw IllegalStateException("NAS 업로드 주소와 키가 설정되지 않았습니다.")
        }
        return withContext(Dispatchers.IO) {
            attachments.map { attachment ->
                attachment.withUploadResponse(uploadOne(submissionId, attachment))
            }
        }
    }

    private fun uploadOne(
        submissionId: String,
        attachment: SubmissionAttachment
    ): NasSubmissionUploadResponse {
        val boundary = "SafeClip-${UUID.randomUUID()}"
        val connection = (URL(uploadUrl).openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            doOutput = true
            doInput = true
            connectTimeout = 20_000
            readTimeout = 120_000
            setRequestProperty("X-SafeClip-Upload-Key", uploadKey)
            setRequestProperty("Content-Type", "multipart/form-data; boundary=$boundary")
        }

        try {
            DataOutputStream(connection.outputStream).use { output ->
                writeTextPart(output, boundary, "submission_id", submissionId)
                writeTextPart(output, boundary, "original_file_name", attachment.displayName)
                writeTextPart(output, boundary, "device_label", "android")
                writeFilePart(output, boundary, attachment)
                output.writeBytes("--$boundary--\r\n")
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

    private fun writeTextPart(
        output: DataOutputStream,
        boundary: String,
        name: String,
        value: String
    ) {
        output.writeBytes("--$boundary\r\n")
        output.writeBytes("Content-Disposition: form-data; name=\"$name\"\r\n\r\n")
        output.write(value.toByteArray(Charsets.UTF_8))
        output.writeBytes("\r\n")
    }

    private fun writeFilePart(
        output: DataOutputStream,
        boundary: String,
        attachment: SubmissionAttachment
    ) {
        output.writeBytes("--$boundary\r\n")
        output.writeBytes(
            "Content-Disposition: form-data; name=\"file\"; filename=\"${attachment.displayName}\"\r\n"
        )
        output.writeBytes("Content-Type: ${attachment.mimeType.ifBlank { "application/octet-stream" }}\r\n\r\n")
        context.contentResolver.openInputStream(Uri.parse(attachment.uriString))?.use { input ->
            input.copyTo(output, bufferSize = 64 * 1024)
        } ?: throw IllegalStateException("첨부 파일을 열 수 없습니다: ${attachment.displayName}")
        output.writeBytes("\r\n")
    }
}
