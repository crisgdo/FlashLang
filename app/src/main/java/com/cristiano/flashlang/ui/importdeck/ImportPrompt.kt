package com.cristiano.flashlang.ui.importdeck

import com.cristiano.flashlang.data.prompts.PromptInputValues
import com.cristiano.flashlang.data.prompts.PromptTemplates
import com.cristiano.flashlang.data.prompts.buildPrompt

fun buildImportPrompt(language: String): String {
    val languageCode = when (language.lowercase()) {
        "es", "espanhol", "español" -> "es"
        else -> "en"
    }
    val vocabularyPrompt = PromptTemplates.all.first { it.id == "vocabulary-texts" }
    return buildPrompt(vocabularyPrompt, languageCode, PromptInputValues())
}
