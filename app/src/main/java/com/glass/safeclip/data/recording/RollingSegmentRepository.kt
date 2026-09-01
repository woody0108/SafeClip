package com.glass.safeclip.data.recording

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.File
import java.nio.charset.StandardCharsets
import java.util.UUID

data class PendingSegment(
    val id: String,
    val file: File,
    val startElapsedMs: Long
)

class RollingSegmentRepository(
    private val directory: File,
    private val gson: Gson = Gson()
) {
    private val metadataFile = File(directory, METADATA_FILE_NAME)
    private var storedSegments: List<RollingSegment>

    init {
        require(directory.mkdirs() || directory.isDirectory) {
            "녹화 조각 폴더를 만들 수 없습니다: ${directory.absolutePath}"
        }
        directory.listFiles()
            ?.filter { it.isFile && it.name.endsWith(PENDING_SUFFIX) }
            ?.forEach(File::delete)
        storedSegments = readMetadata()
            .filter { File(java.net.URI(it.uriString)).isFile }
            .sortedBy(RollingSegment::startElapsedMs)
    }

    @Synchronized
    fun createPending(startElapsedMs: Long): PendingSegment {
        val id = UUID.randomUUID().toString()
        val file = File(directory, "$id$PENDING_SUFFIX")
        require(file.createNewFile()) { "새 녹화 조각 파일을 만들지 못했습니다." }
        return PendingSegment(id, file, startElapsedMs)
    }

    @Synchronized
    fun finalize(pending: PendingSegment, endElapsedMs: Long): RollingSegment {
        require(endElapsedMs > pending.startElapsedMs) { "녹화 조각 종료 시간이 올바르지 않습니다." }
        require(pending.file.isFile) { "완료할 녹화 조각 파일이 없습니다." }

        val completedFile = File(directory, "${pending.id}.mp4")
        if (!pending.file.renameTo(completedFile)) {
            pending.file.copyTo(completedFile, overwrite = false)
            check(pending.file.delete()) { "임시 녹화 조각을 정리하지 못했습니다." }
        }
        val segment = RollingSegment(
            id = pending.id,
            uriString = completedFile.toURI().toString(),
            startElapsedMs = pending.startElapsedMs,
            endElapsedMs = endElapsedMs,
            isProtected = false
        )
        storedSegments = (storedSegments + segment).sortedBy(RollingSegment::startElapsedMs)
        persistMetadata()
        return segment
    }

    @Synchronized
    fun segments(): List<RollingSegment> = storedSegments.toList()

    @Synchronized
    fun beginSession() {
        storedSegments.forEach { segment ->
            runCatching { File(java.net.URI(segment.uriString)).delete() }
        }
        storedSegments = emptyList()
        persistMetadata()
    }

    @Synchronized
    fun protect(ids: Set<String>) {
        updateProtection(ids, isProtected = true)
    }

    @Synchronized
    fun release(ids: Set<String>) {
        updateProtection(ids, isProtected = false)
    }

    @Synchronized
    fun cleanup(nowElapsedMs: Long) {
        val expiredIds = RollingSegmentRetention.expired(nowElapsedMs, storedSegments)
            .filter { segment -> File(java.net.URI(segment.uriString)).delete() }
            .map(RollingSegment::id)
            .toSet()
        if (expiredIds.isNotEmpty()) {
            storedSegments = storedSegments.filterNot { it.id in expiredIds }
            persistMetadata()
        }
    }

    fun availableBytes(): Long = directory.usableSpace

    private fun updateProtection(ids: Set<String>, isProtected: Boolean) {
        if (ids.isEmpty()) return
        storedSegments = storedSegments.map { segment ->
            if (segment.id in ids) segment.copy(isProtected = isProtected) else segment
        }
        persistMetadata()
    }

    private fun readMetadata(): List<RollingSegment> {
        if (!metadataFile.isFile) return emptyList()
        return runCatching {
            metadataFile.reader(StandardCharsets.UTF_8).use { reader ->
                val type = object : TypeToken<List<RollingSegment>>() {}.type
                gson.fromJson<List<RollingSegment>>(reader, type).orEmpty()
            }
        }.getOrDefault(emptyList())
    }

    private fun persistMetadata() {
        val temporary = File(directory, "$METADATA_FILE_NAME.tmp")
        temporary.writer(StandardCharsets.UTF_8).use { writer ->
            gson.toJson(storedSegments, writer)
        }
        if (metadataFile.exists()) {
            check(metadataFile.delete()) { "이전 녹화 메타데이터를 교체하지 못했습니다." }
        }
        check(temporary.renameTo(metadataFile)) { "녹화 메타데이터를 저장하지 못했습니다." }
    }

    private companion object {
        const val METADATA_FILE_NAME = "segments.json"
        const val PENDING_SUFFIX = ".pending.mp4"
    }
}
