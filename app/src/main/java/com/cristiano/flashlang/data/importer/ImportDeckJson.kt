package com.cristiano.flashlang.data.importer

import kotlinx.serialization.Serializable
import com.cristiano.flashlang.data.model.Card

@Serializable
data class ImportDeckJson(
    val deckName: String = "",
    val language: String = "",
    val cards: List<ImportCardJson> = emptyList(),
)

@Serializable
data class ImportCardJson(
    val term: String = "",
    val type: String = "",
    val termTranslation: String = "",
    val example: String = "",
    val exampleTranslation: String = "",
) {
    fun toCard(deckId: Long, createdAt: Long = System.currentTimeMillis()): Card = Card(
        deckId = deckId,
        term = term.trim(),
        type = type,
        termTranslation = termTranslation.trim(),
        example = example.trim(),
        exampleTranslation = exampleTranslation.trim(),
        easeFactor = 2.5,
        intervalDays = 0,
        repetitions = 0,
        dueAt = createdAt,
    )
}
