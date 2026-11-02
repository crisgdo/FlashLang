package com.cristiano.flashlang.ui.importdeck

import com.cristiano.flashlang.data.prompts.PromptInputValues
import com.cristiano.flashlang.data.prompts.PromptTemplates
import com.cristiano.flashlang.data.prompts.buildPrompt

fun buildImportPrompt(language: String, text: String = ""): String {
    val languageCode = when (language.lowercase()) {
        "es", "espanhol", "español" -> "es"
        else -> "en"
    }
    val vocabularyPrompt = PromptTemplates.all.first { it.id == "vocabulary-texts" }
    val trimmedText = text.trim()
    return if (trimmedText.isNotEmpty()) {
        buildPrompt(vocabularyPrompt, languageCode, PromptInputValues(text = trimmedText))
    } else {
        val templateWithoutText = vocabularyPrompt.copy(
            body = vocabularyPrompt.body
                .replace("\n\nTEXTO:\n{{TEXTO}}", "")
                .replace("TEXTO:\n{{TEXTO}}", "")
                .trimEnd(),
        )
        buildPrompt(templateWithoutText, languageCode, PromptInputValues())
    }
}
