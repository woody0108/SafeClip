package com.glass.safeclip.data.recording

import java.io.File
import java.net.URI
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class RollingSegmentRepositoryTest {
    @get:Rule
    val temporaryFolder = TemporaryFolder()

    @Test
    fun finalizePersistsMetadataAndCleanupKeepsProtectedFiles() {
        val directory = temporaryFolder.newFolder("segments")
        val repository = RollingSegmentRepository(directory)
        val pending = repository.createPending(0)
        pending.file.writeBytes(byteArrayOf(1, 2, 3))
        val segment = repository.finalize(pending, 60_000)

        repository.protect(setOf(segment.id))
        repository.cleanup(10_860_001)

        val reloaded = RollingSegmentRepository(directory)
        assertTrue(File(URI(segment.uriString)).exists())
        assertTrue(reloaded.segments().single().isProtected)
    }

    @Test
    fun cleanupRemovesExpiredUnprotectedFilesAndMetadata() {
        val repository = RollingSegmentRepository(temporaryFolder.newFolder("cleanup"))
        val pending = repository.createPending(0)
        pending.file.writeText("video")
        val segment = repository.finalize(pending, 60_000)

        repository.cleanup(10_860_001)

        assertFalse(File(URI(segment.uriString)).exists())
        assertTrue(repository.segments().isEmpty())
    }

    @Test
    fun abandonedPendingFilesAreRemovedOnStartup() {
        val directory = temporaryFolder.newFolder("recovery")
        val abandoned = File(directory, "abandoned.pending.mp4").apply { writeText("partial") }

        RollingSegmentRepository(directory)

        assertFalse(abandoned.exists())
    }

    @Test
    fun releaseAllowsProtectedSegmentToExpire() {
        val repository = RollingSegmentRepository(temporaryFolder.newFolder("release"))
        val pending = repository.createPending(0)
        pending.file.writeText("video")
        val segment = repository.finalize(pending, 60_000)
        repository.protect(setOf(segment.id))

        repository.release(setOf(segment.id))
        repository.cleanup(10_860_001)

        assertEquals(emptyList<RollingSegment>(), repository.segments())
    }

    @Test
    fun beginSessionDiscardsSegmentsFromThePreviousRecording() {
        val directory = temporaryFolder.newFolder("new-session")
        val repository = RollingSegmentRepository(directory)
        val pending = repository.createPending(500_000)
        pending.file.writeText("old video")
        val oldSegment = repository.finalize(pending, 560_000)

        repository.beginSession()

        assertTrue(repository.segments().isEmpty())
        assertFalse(File(URI(oldSegment.uriString)).exists())
        assertTrue(RollingSegmentRepository(directory).segments().isEmpty())
    }
}
