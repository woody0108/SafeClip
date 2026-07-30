package com.glass.safeclip.data.submission

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class NasSubmissionUploadProgressTest {
    @Test
    fun calculatesCurrentFilePercentWhenTotalBytesIsKnown() {
        val progress = NasSubmissionUploadProgress(
            fileIndex = 1,
            totalFiles = 3,
            fileName = "front.mp4",
            bytesSent = 50L,
            totalBytes = 100L
        )

        assertEquals(50, progress.currentFilePercent)
        assertEquals("1/3 front.mp4", progress.fileLabel)
    }

    @Test
    fun currentFilePercentIsNullWhenTotalBytesIsMissing() {
        val progress = NasSubmissionUploadProgress(
            fileIndex = 2,
            totalFiles = 3,
            fileName = "rear.mp4",
            bytesSent = 50L,
            totalBytes = null
        )

        assertNull(progress.currentFilePercent)
    }

    @Test
    fun clampsPercentAtOneHundred() {
        val progress = NasSubmissionUploadProgress(
            fileIndex = 1,
            totalFiles = 1,
            fileName = "front.mp4",
            bytesSent = 150L,
            totalBytes = 100L
        )

        assertEquals(100, progress.currentFilePercent)
    }

    @Test
    fun emitsProgressOnlyWhenPercentChanges() {
        val percents = NasSubmissionUploadProgress.changedPercentEvents(
            totalBytes = 100L,
            sentBytes = listOf(0L, 1L, 1L, 2L, 100L)
        )

        assertEquals(listOf(0, 1, 2, 100), percents)
    }

    @Test
    fun estimatesRemainingSecondsFromKnownUploadSpeed() {
        val progress = NasSubmissionUploadProgress(
            fileIndex = 1,
            totalFiles = 1,
            fileName = "front.mp4",
            bytesSent = 50L,
            totalBytes = 150L,
            startedAtMillis = 1_000L,
            nowMillis = 11_000L
        )

        assertEquals(20L, progress.remainingSeconds)
        assertEquals("예상 남은 시간 20초", progress.remainingTimeText)
    }

    @Test
    fun remainingTimeTextWaitsUntilSpeedCanBeCalculated() {
        val progress = NasSubmissionUploadProgress(
            fileIndex = 1,
            totalFiles = 1,
            fileName = "front.mp4",
            bytesSent = 0L,
            totalBytes = 150L,
            startedAtMillis = 1_000L,
            nowMillis = 11_000L
        )

        assertNull(progress.remainingSeconds)
        assertEquals("남은 시간 계산 중", progress.remainingTimeText)
    }
}
