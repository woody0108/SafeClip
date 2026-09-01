package com.glass.safeclip.data.recording

import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withTimeoutOrNull

internal object LocationRequestCoordinator {
    suspend fun <T> collectAvailable(
        timeoutMs: Long,
        requests: List<suspend () -> T?>
    ): List<T> = coroutineScope {
        requests.map { request ->
            async {
                withTimeoutOrNull(timeoutMs) { request() }
            }
        }.awaitAll().filterNotNull()
    }
}
