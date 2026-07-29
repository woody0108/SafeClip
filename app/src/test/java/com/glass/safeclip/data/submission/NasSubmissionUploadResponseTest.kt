package com.glass.safeclip.data.submission

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class NasSubmissionUploadResponseTest {
    @Test
    fun parsesSuccessfulUploadResponse() {
        val json = """{"ok":true,"stored_name":"stored.mp4","relative_path":"2026/07/29/stored.mp4","size_bytes":1234}"""

        val response = NasSubmissionUploadResponse.parse(json)

        assertEquals("stored.mp4", response.storedName)
        assertEquals("2026/07/29/stored.mp4", response.relativePath)
        assertEquals(1234L, response.sizeBytes)
    }

    @Test
    fun rejectsFailedUploadResponse() {
        val json = """{"ok":false,"error":"Invalid upload key."}"""

        val result = runCatching { NasSubmissionUploadResponse.parse(json) }

        assertTrue(result.isFailure)
        assertEquals("Invalid upload key.", result.exceptionOrNull()?.message)
    }
}
