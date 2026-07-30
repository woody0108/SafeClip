package com.glass.safeclip.data.submission

import org.junit.Assert.assertEquals
import org.junit.Test

class NasSubmissionMultipartBodyTest {
    @Test
    fun calculatesBodyLengthFromTextPartsFileHeaderAndFileSize() {
        val length = NasSubmissionMultipartBody.contentLength(
            boundary = "SafeClip-test",
            submissionId = "safeclip-1",
            submitterLabel = "베짱이들",
            originalFileName = "front.mp4",
            deviceLabel = "android",
            mimeType = "video/mp4",
            fileSizeBytes = 10L
        )

        val expected = (
            "--SafeClip-test\r\nContent-Disposition: form-data; name=\"submission_id\"\r\n\r\nsafeclip-1\r\n" +
                "--SafeClip-test\r\nContent-Disposition: form-data; name=\"submitter_label\"\r\n\r\n베짱이들\r\n" +
                "--SafeClip-test\r\nContent-Disposition: form-data; name=\"original_file_name\"\r\n\r\nfront.mp4\r\n" +
                "--SafeClip-test\r\nContent-Disposition: form-data; name=\"device_label\"\r\n\r\nandroid\r\n" +
                "--SafeClip-test\r\nContent-Disposition: form-data; name=\"file\"; filename=\"front.mp4\"\r\n" +
                "Content-Type: video/mp4\r\n\r\n" +
                "0123456789\r\n" +
                "--SafeClip-test--\r\n"
            ).toByteArray(Charsets.UTF_8).size.toLong()
        assertEquals(expected, length)
    }
}
