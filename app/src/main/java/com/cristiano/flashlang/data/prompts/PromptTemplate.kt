package com.cristiano.flashlang.data.prompts

enum class PromptInputType {
    TEXT,
    LIST,
    FOCUS,
}

data class PromptInput(
    val type: PromptInputType,
    val required: Boolean,
    val label: String,
    val hint: String,
)

data class PromptTemplate(
    val id: String,
    val title: String,
    val description: String,
    val category: String,
    val body: String,
    val inputs: Set<PromptInput>,
    val needsLanguage: Boolean,
)

data class PromptInputValues(
    val text: String = "",
    val list: String = "",
    val focus: String = "",
)
