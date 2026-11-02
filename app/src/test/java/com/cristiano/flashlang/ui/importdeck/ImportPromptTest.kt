package com.cristiano.flashlang.ui.importdeck

import com.cristiano.flashlang.data.prompts.FORMAT_BLOCK
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ImportPromptTest {

    @Test
    fun buildImportPrompt_withEmptyText_removesTextSectionAndDoesNotEmitMissingMarker() {
        val prompt = buildImportPrompt(language = "inglês", text = "")

        assertTrue(prompt.contains("professor de inglês"))
        assertFalse("Prompt should not contain 'TEXTO:' when text is empty", prompt.contains("TEXTO:"))
        assertFalse("Prompt should not contain '(não informado)'", prompt.contains("(não informado)"))
        assertTrue(prompt.contains(FORMAT_BLOCK))
        assertTrue(prompt.contains("Use \"category\": \"Vocabulário\"."))
    }

    @Test
    fun buildImportPrompt_withWhitespaceText_removesTextSection() {
        val prompt = buildImportPrompt(language = "en", text = "   \n\t  ")

        assertFalse("Prompt should not contain 'TEXTO:' when text is whitespace", prompt.contains("TEXTO:"))
        assertFalse(prompt.contains("(não informado)"))
        assertTrue(prompt.contains(FORMAT_BLOCK))
    }

    @Test
    fun buildImportPrompt_withProvidedText_includesTextSectionAndFormatBlock() {
        val sampleText = "Artificial intelligence is transforming software development."
        val prompt = buildImportPrompt(language = "inglês", text = sampleText)

        assertTrue(prompt.contains("professor de inglês"))
        assertTrue("Prompt should contain 'TEXTO:' when text is provided", prompt.contains("TEXTO:\n$sampleText"))
        assertFalse(prompt.contains("(não informado)"))
        assertTrue(prompt.contains(FORMAT_BLOCK))
        assertTrue(prompt.contains("Use \"category\": \"Vocabulário\"."))
    }

    @Test
    fun buildImportPrompt_withSpanishLanguage_setsSpanishInPrompt() {
        val prompt = buildImportPrompt(language = "espanhol", text = "")

        assertTrue(prompt.contains("professor de espanhol"))
        assertFalse(prompt.contains("TEXTO:"))
        assertTrue(prompt.contains(FORMAT_BLOCK))
        assertTrue(prompt.contains("Use \"category\": \"Vocabulário\"."))
    }
}

