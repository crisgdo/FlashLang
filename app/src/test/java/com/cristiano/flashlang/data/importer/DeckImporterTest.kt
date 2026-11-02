package com.cristiano.flashlang.data.importer

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DeckImporterTest {
    private val importer = DeckImporter()

    @Test
    fun parsesValidJsonAndIgnoresUnknownKeys() {
        val result = importer.parse(validJson.replace("\"deckName\":", "\"extra\": 42, \"deckName\":"))

        assertTrue(result is DeckImportResult.Success)
        assertEquals("Travel", (result as DeckImportResult.Success).deck.deckName)
        assertEquals(1, result.deck.cards.size)
    }

    @Test
    fun parsesJsonInsideMarkdownFence() {
        assertTrue(importer.parse("```json\n$validJson\n```") is DeckImportResult.Success)
    }

    @Test
    fun optionalIpaAndNoteCanBeMissingOrProvided() {
        val withoutFields = importer.parse(validJson) as DeckImportResult.Success
        assertEquals(null, withoutFields.deck.cards.single().ipa)
        assertEquals(null, withoutFields.deck.cards.single().note)

        val withFields = importer.parse(
            validJson.replace(
                "\"type\": \"word\",",
                "\"type\": \"word\", \"ipa\": \"[tɪkɪt]\", \"note\": \"Use this in travel contexts.\",",
            ),
        ) as DeckImportResult.Success
        assertEquals("[tɪkɪt]", withFields.deck.cards.single().ipa)
        assertEquals("Use this in travel contexts.", withFields.deck.cards.single().note)
    }

    @Test
    fun acceptsAlternativeIpaAndNoteKeys() {
        val pronunciation = importer.parse(
            validJson.replace("\"type\": \"word\",", "\"type\": \"word\", \"pronunciation\": \"/tɪkɪt/\", \"rule\": \"Rule one\",")
        ) as DeckImportResult.Success
        assertEquals("/tɪkɪt/", pronunciation.deck.cards.single().ipa)
        assertEquals("Rule one", pronunciation.deck.cards.single().note)

        val phonetic = importer.parse(
            validJson.replace("\"type\": \"word\",", "\"type\": \"word\", \"phonetic\": \"ticket-ipa\", \"notes\": \"Plural: tickets\",")
        ) as DeckImportResult.Success
        assertEquals("ticket-ipa", phonetic.deck.cards.single().ipa)
        assertEquals("Plural: tickets", phonetic.deck.cards.single().note)
    }

    @Test
    fun blankOptionalFieldsBecomeNull() {
        val result = importer.parse(
            validJson.replace("\"type\": \"word\",", "\"type\": \"word\", \"ipa\": \"  \", \"note\": \"\\n  \",")
        ) as DeckImportResult.Success

        assertEquals(null, result.deck.cards.single().ipa)
        assertEquals(null, result.deck.cards.single().note)
    }

    @Test
    fun removesLineContinuationSlashesAndDecodesLeadingHtmlSpaces() {
        val copiedJson = """{\
&#x20;"deckName": "Travel",\
&#x20;"language": "en",\
&#x20;"cards": [{\
&#x20;"term": "ticket", "type": "word", "termTranslation": "bilhete", "example": "Need a ticket", "exampleTranslation": "Preciso de bilhete"\
&#x20;}]}"""

        val result = importer.parse(copiedJson)

        assertTrue(result.toString(), result is DeckImportResult.Success)
    }

    @Test
    fun removesBomNonBreakingAndZeroWidthSpacesAndNormalizesCurlyQuotes() {
        val curlyQuotedJson = validJson.replaceFirst("\"", "“").replaceFirst("\"", "”")
        val input = "\uFEFF\u200B${curlyQuotedJson.replaceFirst("{", "{\u00A0") }"

        assertTrue(importer.parse(input) is DeckImportResult.Success)
    }

    @Test
    fun decodesHtmlNamedAndNumericEntities() {
        val input = validJson.replace("\"", "&quot;")
            .replace("Travel", "Rock &amp; Roll")
            .replace("bilhete", "ticket&#x20;pass&#32;&lt;A&gt;")
        val result = importer.parse(input) as DeckImportResult.Success

        assertEquals("Rock & Roll", result.deck.deckName)
        assertEquals("ticket pass <A>", result.deck.cards.first().termTranslation)
    }

    @Test
    fun extractsJsonWhenSurroundedByOtherText() {
        assertTrue(importer.parse("Resposta da IA:\n$validJson\nEspero que ajude!") is DeckImportResult.Success)
    }

    @Test
    fun acceptsTrailingComma() {
        val input = validJson.replace("\"exampleTranslation\": \"Eu preciso de um bilhete.\"", "\"exampleTranslation\": \"Eu preciso de um bilhete.\",")

        assertTrue(importer.parse(input) is DeckImportResult.Success)
    }

    @Test
    fun normalizesCommonLanguageAndCardTypeValues() {
        val input = validJson.replace("\"en\"", "\"INGLÊS\"").replace("\"word\"", "\"palavra\"")
        val result = importer.parse(input) as DeckImportResult.Success

        assertEquals("en", result.deck.language)
        assertEquals("word", result.deck.cards.first().type)
        assertEquals("es", (importer.parse(validJson.replace("\"en\"", "\"español\"")) as DeckImportResult.Success).deck.language)
        assertEquals("expression", (importer.parse(validJson.replace("\"word\"", "\"idiom\"")) as DeckImportResult.Success).deck.cards.first().type)
    }

    @Test
    fun reportsApproximatePositionAndHintForBrokenJson() {
        val error = importer.parse("{ this is not json }") as DeckImportResult.Error

        assertTrue(error.message.contains("linha"))
        assertTrue(error.message.contains("coluna"))
        assertTrue(error.message.contains("Confira vírgulas"))
    }

    @Test
    fun rejectsUnsupportedLanguage() {
        val error = importer.parse(validJson.replace("\"en\"", "\"fr\"")) as DeckImportResult.Error

        assertTrue(error.message.contains("idioma"))
    }

    @Test
    fun importsOptionalCategoryAndTrimsIt() {
        val withCategory = importer.parse(validJson.replace("\"language\": \"en\",", "\"language\": \"en\", \"category\": \"  Pronúncia  \","))
            as DeckImportResult.Success
        assertEquals("Pronúncia", withCategory.deck.category)

        val blankCategory = importer.parse(validJson.replace("\"language\": \"en\",", "\"language\": \"en\", \"category\": \"  \","))
            as DeckImportResult.Success
        assertEquals(null, blankCategory.deck.category)

        val withoutCategory = importer.parse(validJson) as DeckImportResult.Success
        assertEquals(null, withoutCategory.deck.category)
    }

    @Test
    fun rejectsBlankDeckName() {
        val error = importer.parse(validJson.replace("Travel", "   ")) as DeckImportResult.Error

        assertTrue(error.message.contains("deckName"))
    }

    @Test
    fun rejectsEmptyCardsList() {
        val error = importer.parse(validJson.replaceAfter("\"cards\":", " []}")) as DeckImportResult.Error

        assertTrue(error.message.contains("pelo menos um card"))
    }

    @Test
    fun reportsMissingRequiredFieldAndCardNumber() {
        val error = importer.parse(validJson.replace("\"term\": \"ticket\",", "")) as DeckImportResult.Error

        assertTrue(error.message.contains("term"))
        assertTrue(error.message.contains("card 1"))
    }

    @Test
    fun importsClozeWithExistingGapAndNormalizesAllGapVariantsToOne() {
        val input = validJson
            .replace("\"example\": \"I need a ticket.\"", "\"example\": \"I need [___] and ____ today.\"")
            .replace("\"exampleTranslation\":", "\"format\": \"cloze\", \"exampleTranslation\":")
        val result = importer.parse(input) as DeckImportResult.Success
        val card = result.deck.cards.single()

        assertEquals("cloze", card.format)
        assertEquals("I need ___ and ticket today.", card.example)
    }

    @Test
    fun createsClozeGapFromTermWhenMissing() {
        val input = validJson.replace("\"example\": \"I need a ticket.\"", "\"example\": \"I need a ticket today.\"")
            .replace("\"exampleTranslation\":", "\"format\": \"lacuna\", \"exampleTranslation\":")
        val result = importer.parse(input) as DeckImportResult.Success

        assertEquals("I need a ___ today.", result.deck.cards.single().example)
        assertEquals("cloze", result.deck.cards.single().format)
    }

    @Test
    fun normalizesClozeEllipsisAndUnicodeEllipsisGaps() {
        val jsonWithGaps = validJson
            .replace("\"example\": \"I need a ticket.\"", "\"example\": \"I need [...] and … today.\"")
            .replace("\"exampleTranslation\":", "\"format\": \"cloze\", \"exampleTranslation\":")
        val result = importer.parse(jsonWithGaps) as DeckImportResult.Success

        assertEquals("I need ___ and ticket today.", result.deck.cards.single().example)
    }

    @Test
    fun rejectsClozeWithoutGapOrTermAndDefaultsInvalidFormatToBasic() {
        val invalidCloze = validJson.replace("I need a ticket.", "The train leaves now.")
            .replace("\"exampleTranslation\":", "\"format\": \"cloze\", \"exampleTranslation\":")
        val error = importer.parse(invalidCloze) as DeckImportResult.Error
        assertTrue(error.message.contains("card 1"))
        assertTrue(error.message.contains("lacuna"))

        val invalidFormat = validJson.replace("\"exampleTranslation\":", "\"format\": \"unknown\", \"exampleTranslation\":")
        val success = importer.parse(invalidFormat) as DeckImportResult.Success
        assertEquals("basic", success.deck.cards.single().format)

        val omittedFormat = importer.parse(validJson) as DeckImportResult.Success
        assertEquals("basic", omittedFormat.deck.cards.single().format)
    }

    @Test
    fun strictParse_preservesCurvedQuotesInsideValidJsonStrings() {
        val jsonWithCurvedQuotes = """{
          "deckName": "Citações “famosas”",
          "language": "en",
          "cards": [{
            "term": "“ticket”",
            "type": "word",
            "termTranslation": "‘bilhete’",
            "example": "He said: “I need a ticket”.",
            "exampleTranslation": "Ele disse: ‘Preciso de um bilhete’."
          }]
        }"""
        val result = importer.parse(jsonWithCurvedQuotes)
        assertTrue(result.toString(), result is DeckImportResult.Success)
        val deck = (result as DeckImportResult.Success).deck
        assertEquals("Citações “famosas”", deck.deckName)
        val card = deck.cards.single()
        assertEquals("“ticket”", card.term)
        assertEquals("‘bilhete’", card.termTranslation)
        assertEquals("He said: “I need a ticket”.", card.example)
        assertEquals("Ele disse: ‘Preciso de um bilhete’.", card.exampleTranslation)
    }

    @Test
    fun strictParse_preservesLiteralHtmlEntitiesAndSpecialChars() {
        val jsonWithEntities = """{
          "deckName": "Music",
          "language": "en",
          "cards": [{
            "term": "Rock &amp; Roll",
            "type": "expression",
            "termTranslation": "Rock &amp; Roll",
            "example": "We love Rock &amp; Roll.",
            "exampleTranslation": "Nós amamos Rock &amp; Roll.",
            "note": "Use `code` &lt;tags&gt;"
          }]
        }"""
        val result = importer.parse(jsonWithEntities)
        assertTrue(result.toString(), result is DeckImportResult.Success)
        val card = (result as DeckImportResult.Success).deck.cards.single()
        assertEquals("Rock &amp; Roll", card.term)
        assertEquals("Rock &amp; Roll", card.termTranslation)
        assertEquals("We love Rock &amp; Roll.", card.example)
        assertEquals("Use `code` &lt;tags&gt;", card.note)
    }

    @Test
    fun validateAndNormalize_normalizesDirectModelWithoutModifyingText() {
        val model = ImportDeckJson(
            deckName = "  Quotes  ",
            language = "ENGLISH",
            category = "  Test  ",
            cards = listOf(
                ImportCardJson(
                    term = "“custom”",
                    type = "palavra",
                    termTranslation = "‘personalizado’",
                    example = "This is “custom”.",
                    exampleTranslation = "Isto é ‘personalizado’.",
                    format = "basic",
                ),
            ),
        )
        val result = importer.validateAndNormalize(model)
        assertTrue(result is DeckImportResult.Success)
        val deck = (result as DeckImportResult.Success).deck
        assertEquals("Quotes", deck.deckName)
        assertEquals("en", deck.language)
        assertEquals("Test", deck.category)
        val card = deck.cards.single()
        assertEquals("“custom”", card.term)
        assertEquals("‘personalizado’", card.termTranslation)
        assertEquals("word", card.type)
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
