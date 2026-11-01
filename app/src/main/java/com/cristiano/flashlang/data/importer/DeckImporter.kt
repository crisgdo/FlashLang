package com.cristiano.flashlang.data.importer

import kotlinx.serialization.SerializationException
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import java.util.Locale

sealed interface DeckImportResult {
    data class Success(val deck: ImportDeckJson) : DeckImportResult
    data class Error(val message: String) : DeckImportResult
}

@OptIn(ExperimentalSerializationApi::class)
class DeckImporter(
    private val json: Json = Json {
        ignoreUnknownKeys = true
        allowTrailingComma = true
    },
) {
    fun parse(input: String): DeckImportResult {
        val cleanedInput = cleanInput(input)
        val parsed = try {
            json.decodeFromString<ImportDeckJson>(cleanedInput)
        } catch (exception: SerializationException) {
            return DeckImportResult.Error(invalidJsonMessage(cleanedInput, exception))
        } catch (exception: IllegalArgumentException) {
            return DeckImportResult.Error(invalidJsonMessage(cleanedInput, exception))
        }

        val deck = parsed.copy(
            deckName = parsed.deckName.trim(),
            language = normalizeLanguage(parsed.language),
            cards = parsed.cards.map { card ->
                card.copy(
                    type = normalizeType(card.type),
                    ipa = card.ipa?.trim()?.takeIf(String::isNotEmpty),
                    note = card.note?.trim()?.takeIf(String::isNotEmpty),
                )
            },
        )

        if (deck.deckName.isBlank()) return DeckImportResult.Error("O campo deckName não pode ficar vazio.")
        if (deck.language !in setOf("en", "es")) return DeckImportResult.Error("O idioma deve ser inglês (en) ou espanhol (es).")
        if (deck.cards.isEmpty()) return DeckImportResult.Error("O baralho precisa conter pelo menos um card.")

        val requiredFields = listOf(
            "term" to ImportCardJson::term,
            "type" to ImportCardJson::type,
            "termTranslation" to ImportCardJson::termTranslation,
            "example" to ImportCardJson::example,
            "exampleTranslation" to ImportCardJson::exampleTranslation,
        )
        deck.cards.forEachIndexed { index, card ->
            requiredFields.firstOrNull { (_, value) -> value(card).isBlank() }?.let { (field, _) ->
                return DeckImportResult.Error("Campo obrigatório \"$field\" vazio ou ausente no card ${index + 1}.")
            }
            if (card.type !in setOf("word", "expression")) {
                return DeckImportResult.Error("O campo type do card ${index + 1} deve ser \"word\" ou \"expression\".")
            }
        }

        return DeckImportResult.Success(deck)
    }

    private fun cleanInput(input: String): String {
        var cleaned = input.replace(Regex("\\\\\\r?\\n")) { match ->
            if (match.value.endsWith("\r\n")) "\r\n" else "\n"
        }
        cleaned = decodeHtmlEntities(cleaned)
        cleaned = cleaned.replace(Regex("[\\uFEFF\\u00A0\\u200B]"), "")
        cleaned = cleaned
            .replace('“', '"').replace('”', '"')
            .replace('‘', '\'').replace('’', '\'')
        cleaned = cleaned.replace(Regex("```(?:json)?", RegexOption.IGNORE_CASE), "")
        val firstBrace = cleaned.indexOf('{')
        val lastBrace = cleaned.lastIndexOf('}')
        return if (firstBrace >= 0 && lastBrace >= firstBrace) {
            cleaned.substring(firstBrace, lastBrace + 1).trim()
        } else {
            cleaned.trim()
        }
    }

    private fun decodeHtmlEntities(input: String): String {
        val entityPattern = Regex("&#(?:x([0-9a-fA-F]+)|(\\d+));|&(nbsp|quot|amp|lt|gt);", RegexOption.IGNORE_CASE)
        return entityPattern.replace(input) { match ->
            val hex = match.groups[1]?.value
            val decimal = match.groups[2]?.value
            val named = match.groups[3]?.value?.lowercase(Locale.ROOT)
            when {
                hex != null -> hex.toIntOrNull(16)?.let(Character::toChars)?.concatToString() ?: match.value
                decimal != null -> decimal.toIntOrNull()?.let(Character::toChars)?.concatToString() ?: match.value
                named == "nbsp" -> "\u00A0"
                named == "quot" -> "\""
                named == "amp" -> "&"
                named == "lt" -> "<"
                named == "gt" -> ">"
                else -> match.value
            }
        }
    }

    private fun normalizeLanguage(value: String): String = when (value.trim().lowercase(Locale.ROOT)) {
        "en", "english", "inglês", "ingles" -> "en"
        "es", "spanish", "espanhol", "español" -> "es"
        else -> value.trim()
    }

    private fun normalizeType(value: String): String = when (value.trim().lowercase(Locale.ROOT)) {
        "word", "palavra" -> "word"
        "expression", "phrase", "expressão", "idiom" -> "expression"
        else -> value.trim()
    }

    private fun invalidJsonMessage(source: String, exception: Exception): String {
        val offset = Regex("offset (\\d+)", RegexOption.IGNORE_CASE)
            .find(exception.message.orEmpty())?.groupValues?.get(1)?.toIntOrNull()
            ?.coerceIn(0, source.length) ?: 0
        val line = source.take(offset).count { it == '\n' } + 1
        val lastLineBreak = source.lastIndexOf('\n', (offset - 1).coerceAtLeast(0))
        val column = offset - lastLineBreak
        return "JSON inválido perto da linha $line, coluna $column. Confira vírgulas, aspas e chaves."
    }
}
