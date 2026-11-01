package com.cristiano.flashlang.data.backup

import kotlinx.serialization.Serializable

@Serializable
data class BackupFileJson(
    val app: String = "FlashLang",
    val backupVersion: Int = 1,
    val exportedAt: String = "",
    val decks: List<BackupDeckJson> = emptyList(),
    val prompts: List<BackupPromptJson> = emptyList(),
)

@Serializable
data class BackupDeckJson(
    val deckName: String,
    val language: String,
    val category: String? = null,
    val cards: List<BackupCardJson>,
)

@Serializable
data class BackupCardJson(
    val term: String,
    val type: String,
    val termTranslation: String,
    val example: String,
    val exampleTranslation: String,
    val ipa: String? = null,
    val note: String? = null,
    val format: String? = null,
    val progress: BackupProgressJson? = null,
)

@Serializable
data class BackupProgressJson(
    val easeFactor: Double,
    val intervalDays: Int,
    val repetitions: Int,
    val dueAt: Long,
)

@Serializable
data class BackupPromptJson(
    val title: String,
    val description: String = "",
    val body: String,
    val generatesDeck: Boolean = true,
)
