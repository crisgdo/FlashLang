package com.cristiano.flashlang.data.importer

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DeckImporterTest {
    private val importer = DeckImporter()

    @Test
    fun parsesValidJson() {
        val result = importer.parse(validJson)

        assertTrue(result is DeckImportResult.Success)
        assertEquals("Travel", (result as DeckImportResult.Success).deck.deckName)
        assertEquals(1, result.deck.cards.size)
    }

    @Test
    fun parsesJsonInsideMarkdownFence() {
        val result = importer.parse("```json\n$validJson\n```")

        assertTrue(result is DeckImportResult.Success)
    }

    @Test
    fun returnsClearErrorForBrokenJson() {
        val result = importer.parse("{ this is not json }")

        assertEquals("JSON inválido. Verifique a estrutura e os campos do baralho.", (result as DeckImportResult.Error).message)
    }

    @Test
    fun rejectsUnsupportedLanguage() {
        val result = importer.parse(validJson.replace("\"en\"", "\"fr\""))

        assertTrue((result as DeckImportResult.Error).message.contains("idioma"))
    }

    @Test
    fun rejectsBlankDeckName() {
        val result = importer.parse(validJson.replace("Travel", "   "))

        assertTrue((result as DeckImportResult.Error).message.contains("nome do baralho"))
    }

    @Test
    fun rejectsEmptyCardsList() {
        val result = importer.parse(validJson.replaceAfter("\"cards\":", " []}"))

        assertTrue((result as DeckImportResult.Error).message.contains("pelo menos um card"))
    }

    @Test
    fun rejectsMissingRequiredCardField() {
        val result = importer.parse(validJson.replace("\"term\": \"ticket\",", ""))

        assertTrue((result as DeckImportResult.Error).message.contains("Cada card"))
    }

    private companion object {
        const val validJson = """{
          "deckName": "Travel",
          "language": "en",
          "cards": [{
            "term": "ticket",
            "type": "word",
            "termTranslation": "bilhete",
            "example": "I need a ticket.",
            "exampleTranslation": "Eu preciso de um bilhete."
          }]
        }"""
    }
}
