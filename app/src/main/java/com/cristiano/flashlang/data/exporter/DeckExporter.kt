package com.cristiano.flashlang.data.exporter

import com.cristiano.flashlang.data.model.Card
import com.cristiano.flashlang.data.model.Deck
import kotlinx.serialization.Serializable
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
data class ExportDeckJson(
    val deckName: String,
    val language: String,
    val cards: List<ExportCardJson>,
)

@Serializable
data class ExportCardJson(
    val term: String,
    val type: String,
    val termTranslation: String,
    val example: String,
    val exampleTranslation: String,
    val ipa: String? = null,
    val note: String? = null,
    val format: String? = null,
)

@OptIn(ExperimentalSerializationApi::class)
object DeckExporter {
    private val json = Json {
        prettyPrint = true
        explicitNulls = false
    }

    fun toJson(deck: Deck, cards: List<Card>): String = json.encodeToString(
        ExportDeckJson(
            deckName = deck.name,
            language = deck.language,
            cards = cards.map { card ->
                ExportCardJson(
                    term = card.term,
                    type = card.type,
                    termTranslation = card.termTranslation,
                    example = card.example,
                    exampleTranslation = card.exampleTranslation,
                    ipa = card.ipa,
                    note = card.note,
                    format = card.format.takeIf { it == "cloze" },
                )
            },
        ),
    )
}
