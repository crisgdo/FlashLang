package com.cristiano.flashlang.data.exporter

import com.cristiano.flashlang.data.importer.DeckImportResult
import com.cristiano.flashlang.data.importer.DeckImporter
import com.cristiano.flashlang.data.model.Card
import com.cristiano.flashlang.data.model.Deck
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DeckExporterTest {
    @Test
    fun exportJsonContainsImportFormatAndExcludesReviewProgress() {
        val json = DeckExporter.toJson(deck, listOf(card))

        assertTrue(json.contains("\"deckName\""))
        assertTrue(json.contains("\"termTranslation\""))
        assertFalse(json.contains("easeFactor"))
        assertFalse(json.contains("intervalDays"))
        assertFalse(json.contains("repetitions"))
        assertFalse(json.contains("dueAt"))
        assertTrue(json.contains("\"ipa\": \"/ˈtɪkɪt/\""))
        assertTrue(json.contains("\"note\": \"A travel document.\""))
    }

    @Test
    fun exportOmitsOptionalFieldsWhenAbsent() {
        val json = DeckExporter.toJson(deck, listOf(card.copy(ipa = null, note = null)))

        assertFalse(json.contains("\"ipa\""))
        assertFalse(json.contains("\"note\""))
    }

    @Test
    fun exportIncludesFormatOnlyForClozeAndImportPreservesIt() {
        val json = DeckExporter.toJson(deck, listOf(card.copy(format = "cloze", example = "I need a ___.")))
        assertTrue(json.contains("\"format\": \"cloze\""))
        val imported = DeckImporter().parse(json) as DeckImportResult.Success
        assertEquals("cloze", imported.deck.cards.single().format)
        assertEquals("I need a ___.", imported.deck.cards.single().example)

        val basicJson = DeckExporter.toJson(deck, listOf(card.copy(format = "basic")))
        assertFalse(basicJson.contains("\"format\""))
    }

    @Test
    fun exportThenImportPreservesDeckAndCardContent() {
        val exported = DeckExporter.toJson(deck, listOf(card))
        val imported = DeckImporter().parse(exported)

        assertTrue(imported is DeckImportResult.Success)
        imported as DeckImportResult.Success
        assertEquals(deck.name, imported.deck.deckName)
        assertEquals(deck.language, imported.deck.language)
        assertEquals(1, imported.deck.cards.size)
        val importedCard = imported.deck.cards.single()
        assertEquals(card.term, importedCard.term)
        assertEquals(card.type, importedCard.type)
        assertEquals(card.termTranslation, importedCard.termTranslation)
        assertEquals(card.example, importedCard.example)
        assertEquals(card.exampleTranslation, importedCard.exampleTranslation)
        assertEquals(card.ipa, importedCard.ipa)
        assertEquals(card.note, importedCard.note)
    }

    private companion object {
        val deck = Deck(id = 7, name = "Viagem", language = "en", createdAt = 123)
        val card = Card(
            id = 11,
            deckId = deck.id,
            term = "ticket",
            type = "word",
            termTranslation = "bilhete",
            example = "I need a ticket.",
            exampleTranslation = "Eu preciso de um bilhete.",
            ipa = "/ˈtɪkɪt/",
            note = "A travel document.",
            easeFactor = 1.7,
            intervalDays = 8,
            repetitions = 3,
            dueAt = 99_999,
        )
    }
}
