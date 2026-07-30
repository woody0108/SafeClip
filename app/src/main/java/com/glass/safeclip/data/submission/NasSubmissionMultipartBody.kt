package com.glass.safeclip.data.submission

object NasSubmissionMultipartBody {
    fun textPart(boundary: String, name: String, value: String): ByteArray {
        return (
            "--$boundary\r\n" +
                "Content-Disposition: form-data; name=\"$name\"\r\n\r\n" +
                value +
                "\r\n"
            ).toByteArray(Charsets.UTF_8)
    }

    fun fileHeader(boundary: String, fileName: String, mimeType: String): ByteArray {
        return (
            "--$boundary\r\n" +
                "Content-Disposition: form-data; name=\"file\"; filename=\"$fileName\"\r\n" +
                "Content-Type: ${mimeType.ifBlank { "application/octet-stream" }}\r\n\r\n"
            ).toByteArray(Charsets.UTF_8)
    }

    fun closingBoundary(boundary: String): ByteArray {
        return "--$boundary--\r\n".toByteArray(Charsets.UTF_8)
    }

    fun contentLength(
        boundary: String,
        submissionId: String,
        submitterLabel: String,
        originalFileName: String,
        deviceLabel: String,
        mimeType: String,
        fileSizeBytes: Long
    ): Long {
        return textPart(boundary, "submission_id", submissionId).size.toLong() +
            textPart(boundary, "submitter_label", submitterLabel).size.toLong() +
            textPart(boundary, "original_file_name", originalFileName).size.toLong() +
            textPart(boundary, "device_label", deviceLabel).size.toLong() +
            fileHeader(boundary, originalFileName, mimeType).size.toLong() +
            fileSizeBytes +
            "\r\n".toByteArray(Charsets.UTF_8).size.toLong() +
            closingBoundary(boundary).size.toLong()
    }
}
