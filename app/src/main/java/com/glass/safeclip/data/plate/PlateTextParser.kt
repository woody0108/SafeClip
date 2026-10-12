package com.glass.safeclip.data.plate

object PlateTextParser {
    private val modernPlate = Regex("(?<!\\d)(\\d{2,3})[\\s-]*([가-힣])[\\s-]*(\\d{4})(?!\\d)")

    fun extractCandidates(text: String): List<String> {
        return modernPlate.findAll(text)
            .map { match -> match.groupValues.drop(1).joinToString("") }
            .distinct()
            .toList()
    }

    fun normalizeConfirmed(text: String): String? {
        val compact = text.replace(Regex("[\\s-]"), "")
        return modernPlate.matchEntire(compact)
            ?.groupValues
            ?.drop(1)
            ?.joinToString("")
    }
}
