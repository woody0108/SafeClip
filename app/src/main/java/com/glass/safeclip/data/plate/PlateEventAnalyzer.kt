package com.glass.safeclip.data.plate

data class PlateAnalysisResult(
    val candidates: List<String>,
    val contextImageUri: String? = null,
    val plateImageUri: String? = null
)

interface PlateEventAnalyzer {
    fun startSession()
    fun stopSession()
    suspend fun analyzeEvent(eventId: String, triggerElapsedMs: Long, triggerEpochMs: Long): PlateAnalysisResult
    fun close()

    object None : PlateEventAnalyzer {
        override fun startSession() = Unit
        override fun stopSession() = Unit
        override suspend fun analyzeEvent(eventId: String, triggerElapsedMs: Long, triggerEpochMs: Long) =
            PlateAnalysisResult(emptyList())
        override fun close() = Unit
    }
}
