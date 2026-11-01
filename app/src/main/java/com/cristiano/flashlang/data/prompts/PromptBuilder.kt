package com.cristiano.flashlang.data.prompts

const val FORMAT_BLOCK = """FORMATO DE RESPOSTA (obrigatório): responda APENAS com JSON válido,
sem texto antes ou depois, sem bloco de código, sem aspas curvas,
sem comentários e sem vírgula depois do último item, neste formato:
{"deckName": "...", "language": "en ou es", "cards": [{"term": "...",
"type": "word ou expression", "termTranslation": "...", "example":
"...", "exampleTranslation": "...", "ipa": "... (opcional)",
"note": "... (opcional)"}]}.
Regras: traduções em português do Brasil; language "en" para
inglês e "es" para espanhol; ipa entre barras, em inglês americano
(en) ou espanhol da Espanha (es); note curta e em português."""

fun canGeneratePrompt(template: PromptTemplate, values: PromptInputValues): Boolean =
    template.inputs.filter { it.required }.all { input ->
        valueFor(input.type, values).isNotBlank()
    }

fun buildPrompt(
    template: PromptTemplate,
    language: String,
    values: PromptInputValues,
): String {
    val languageName = when (language) {
        "en" -> "inglês"
        "es" -> "espanhol"
        else -> ""
    }.ifBlank { "(não informado)" }

    val body = template.body
        .replace("{{IDIOMA}}", languageName)
        .replace("{{TEXTO}}", values.text.nonEmptyOrMissing())
        .replace("{{LISTA}}", values.list.nonEmptyOrMissing())
        .replace("{{FOCO}}", values.focus.nonEmptyOrMissing())
        .trimEnd()

    return "$body\n\n$FORMAT_BLOCK"
}

private fun valueFor(type: PromptInputType, values: PromptInputValues): String = when (type) {
    PromptInputType.TEXT -> values.text
    PromptInputType.LIST -> values.list
    PromptInputType.FOCUS -> values.focus
}

private fun String.nonEmptyOrMissing(): String = trim().ifEmpty { "(não informado)" }
