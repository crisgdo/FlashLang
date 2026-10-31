package com.cristiano.flashlang.data.importer

import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json

sealed interface DeckImportResult {
    data class Success(val deck: ImportDeckJson) : DeckImportResult
    data class Error(val message: String) : DeckImportResult
}

class DeckImporter(
    private val json: Json = Json { ignoreUnknownKeys = true },
) {
    fun parse(input: String): DeckImportResult {
        val cleanedInput = removeMarkdownFence(input)
        val deck = try {
            json.decodeFromString<ImportDeckJson>(cleanedInput)
        } catch (_: SerializationException) {
            return DeckImportResult.Error("JSON inválido. Verifique a estrutura e os campos do baralho.")
        } catch (_: IllegalArgumentException) {
            return DeckImportResult.Error("JSON inválido. Verifique a estrutura e os campos do baralho.")
        }

        if (deck.deckName.isBlank()) return DeckImportResult.Error("O nome do baralho (deckName) não pode ficar vazio.")
        if (deck.language !in setOf("en", "es")) return DeckImportResult.Error("O idioma deve ser \"en\" (inglês) ou \"es\" (espanhol).")
        if (deck.cards.isEmpty()) return DeckImportResult.Error("O baralho precisa conter pelo menos um card.")
        if (deck.cards.any { card ->
                card.term.isBlank() || card.type.isBlank() || card.termTranslation.isBlank() ||
                    card.example.isBlank() || card.exampleTranslation.isBlank()
            }
        ) return DeckImportResult.Error("Cada card deve preencher term, type, termTranslation, example e exampleTranslation.")
        if (deck.cards.any { it.type !in setOf("word", "expression") }) {
            return DeckImportResult.Error("O tipo de cada card deve ser \"word\" ou \"expression\".")
        }

        return DeckImportResult.Success(deck)
    }

    private fun removeMarkdownFence(input: String): String {
        val trimmed = input.trim()
        if (!trimmed.startsWith("```")) return trimmed
        val firstLineEnd = trimmed.indexOf('\n')
        if (firstLineEnd < 0) return trimmed.removePrefix("```").removeSuffix("```").trim()
        val bodyStart = firstLineEnd + 1
        val closingFence = trimmed.lastIndexOf("```")
        if (closingFence < bodyStart) return trimmed
        return trimmed.substring(bodyStart, closingFence).trim()
    }
}
