package com.cristiano.flashlang.data.importer

import kotlinx.serialization.Serializable

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
)
