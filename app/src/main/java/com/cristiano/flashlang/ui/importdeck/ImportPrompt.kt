package com.cristiano.flashlang.ui.importdeck

fun buildImportPrompt(language: String): String = IMPORT_PROMPT_TEMPLATE.replace("[IDIOMA]", language)

private const val IMPORT_PROMPT_TEMPLATE = """Leia o texto abaixo e gere de 15 a 25 flashcards com palavras e expressões úteis para aprendizado de [IDIOMA]. Responda APENAS com JSON válido, sem texto extra e sem bloco de código, neste formato:
{"deckName": "...", "language": "en ou es", "cards": [{"term": "...", "type": "word ou expression", "termTranslation": "...", "example": "...", "exampleTranslation": "..."}]}.
Use "en" para inglês e "es" para espanhol no campo language.
Traduções em português do Brasil. A frase de exemplo deve usar o termo em contexto, de forma natural. Priorize palavras e expressões relevantes do texto, evite palavras muito básicas.

TEXTO:
"""
