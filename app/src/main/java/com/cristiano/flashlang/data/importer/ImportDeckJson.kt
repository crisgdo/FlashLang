package com.cristiano.flashlang.data.importer

import kotlinx.serialization.Serializable
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.JsonNames
import com.cristiano.flashlang.data.model.Card

@Serializable
data class ImportDeckJson(
    val deckName: String = "",
    val language: String = "",
    val cards: List<ImportCardJson> = emptyList(),
)

@OptIn(ExperimentalSerializationApi::class)
@Serializable
data class ImportCardJson(
    val term: String = "",
    val type: String = "",
    val termTranslation: String = "",
    val example: String = "",
    val exampleTranslation: String = "",
    @JsonNames("pronunciation", "phonetic")
    val ipa: String? = null,
    @JsonNames("rule", "notes")
    val note: String? = null,
) {
    fun toCard(deckId: Long, createdAt: Long = System.currentTimeMillis()): Card = Card(
        deckId = deckId,
        term = term.trim(),
        type = type,
        termTranslation = termTranslation.trim(),
        example = example.trim(),
        exampleTranslation = exampleTranslation.trim(),
        ipa = ipa?.trim()?.takeIf(String::isNotEmpty),
        note = note?.trim()?.takeIf(String::isNotEmpty),
        easeFactor = 2.5,
        intervalDays = 0,
        repetitions = 0,
        dueAt = createdAt,
    )
}
