package com.cristiano.flashlang.data.prompts

import com.cristiano.flashlang.data.model.UserPrompt
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PromptBuilderTest {
    private val vocabularyPrompt = PromptTemplates.all.first { it.id == "vocabulary-texts" }

    @Test
    fun replacesLanguageAndInputMarkers() {
        val prompt = buildPrompt(
            vocabularyPrompt,
            language = "es",
            values = PromptInputValues(text = "A short story"),
        )

        assertTrue(prompt.contains("professor de espanhol"))
        assertTrue(prompt.endsWith("TEXTO:\nA short story\n\n$FORMAT_BLOCK\nUse \"category\": \"Vocabulário\"."))
    }

    @Test
    fun replacesListAndFocusMarkers() {
        val vowelPrompt = PromptTemplates.all.first { it.id == "vowel-sounds" }
        val prompt = buildPrompt(
            vowelPrompt,
            language = "en",
            values = PromptInputValues(list = "hat, hate", focus = "vogal a"),
        )

        assertTrue(prompt.contains("Foco: vogal a;"))
        assertTrue(prompt.contains("classifique cada uma: hat, hate."))
    }

    @Test
    fun replacesEmptyMarkersWithNotProvidedText() {
        val vowelPrompt = PromptTemplates.all.first { it.id == "vowel-sounds" }
        val prompt = buildPrompt(vowelPrompt, language = "en", values = PromptInputValues())

        assertTrue(prompt.contains("Foco: (não informado)"))
        assertTrue(prompt.contains("classifique cada uma: (não informado)"))
    }

    @Test
    fun everyDeckPromptEndsWithTheSingleFormatBlock() {
        PromptTemplates.all.filter { it.generatesDeck }.forEach { template ->
            val prompt = buildPrompt(template, language = "en", values = PromptInputValues(text = "some text", list = "walk"))

            assertTrue(prompt.endsWith("$FORMAT_BLOCK\nUse \"category\": \"${template.category}\"."))
            assertEquals(1, prompt.windowed(FORMAT_BLOCK.length).count { it == FORMAT_BLOCK })
        }

        val conversation = PromptTemplates.all.single { !it.generatesDeck }
        assertFalse(buildPrompt(conversation, "en", PromptInputValues(text = "text")).endsWith(FORMAT_BLOCK))
    }

    @Test
    fun readyPromptHasItsCategoryInstructionButUserPromptDoesNot() {
        val readyPrompt = buildPrompt(vocabularyPrompt, "en", PromptInputValues(text = "story"))
        assertTrue(readyPrompt.contains("Use \"category\": \"Vocabulário\"."))

        val ownPrompt = UserPrompt(
            id = 44,
            title = "Meu vocabulário",
            description = "",
            body = "{{TEXTO}}",
        ).toPromptTemplate()
        val generatedOwnPrompt = buildPrompt(ownPrompt, "en", PromptInputValues(text = "story"))
        assertFalse(generatedOwnPrompt.contains("Use \"category\":"))
    }

    @Test
    fun requiredInputsControlWhetherPromptCanBeCopied() {
        val verbPrompt = PromptTemplates.all.first { it.id == "ed-pronunciation" }
        assertFalse(canGeneratePrompt(verbPrompt, PromptInputValues()))
        assertTrue(canGeneratePrompt(verbPrompt, PromptInputValues(list = "walk, play")))

        val vowelPrompt = PromptTemplates.all.first { it.id == "vowel-sounds" }
        assertTrue(canGeneratePrompt(vowelPrompt, PromptInputValues()))
    }

    @Test
    fun detectsOwnPromptInputsFromBodyMarkers() {
        val template = UserPrompt(
            id = 8,
            title = "Meu prompt",
            description = "",
            body = "{{IDIOMA}} {{TEXTO}} {{LISTA}} {{FOCO}}",
            generatesDeck = false,
        ).toPromptTemplate()

        assertTrue(template.needsLanguage)
        assertFalse(template.generatesDeck)
        assertEquals(
            listOf(PromptInputType.TEXT, PromptInputType.LIST, PromptInputType.FOCUS),
            template.inputs.map { it.type },
        )
        assertTrue(template.inputs.first { it.type == PromptInputType.TEXT }.required)
        assertTrue(template.inputs.first { it.type == PromptInputType.LIST }.required)
        assertFalse(template.inputs.first { it.type == PromptInputType.FOCUS }.required)
    }

    @Test
    fun clozePromptGeneratesDeckFormatAndIncludesClozeInstructions() {
        val template = PromptTemplates.all.single { it.id == "cloze-text-sentences" }
        val generated = buildPrompt(template, "en", PromptInputValues(text = "A short story"))

        assertTrue(template.generatesDeck)
        assertTrue(generated.contains("Use \"format\": \"cloze\" em todos os cards."))
        assertTrue(generated.endsWith("$FORMAT_BLOCK\nUse \"category\": \"Vocabulário\"."))
    }
}
