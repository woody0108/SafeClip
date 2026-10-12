package com.glass.safeclip.data.plate

import kotlin.math.abs

data class PlateObservation(
    val text: String,
    val frameOffsetMs: Long,
    val pixelArea: Int,
    val sharpness: Double
)

data class RankedPlateCandidate(
    val text: String,
    val frameCount: Int,
    val bestObservation: PlateObservation
)

object PlateCandidateRanker {
    fun rank(observations: List<PlateObservation>): List<RankedPlateCandidate> {
        return observations
            .groupBy(PlateObservation::text)
            .map { (text, matches) ->
                RankedPlateCandidate(
                    text = text,
                    frameCount = matches.size,
                    bestObservation = matches.maxBy(::frameScore)
                )
            }
            .sortedWith(
                compareByDescending<RankedPlateCandidate> { it.frameCount }
                    .thenByDescending { frameScore(it.bestObservation) }
            )
    }

    private fun frameScore(observation: PlateObservation): Double {
        return observation.sharpness * 1_000.0 + observation.pixelArea - abs(observation.frameOffsetMs)
    }
}
