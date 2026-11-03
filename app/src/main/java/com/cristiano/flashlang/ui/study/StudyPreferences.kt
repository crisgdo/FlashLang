package com.cristiano.flashlang.ui.study

enum class StudyMode(val storedValue: String) {
    STANDARD("standard"),
    INVERTED("inverted"),
    PRONUNCIATION("pronunciation");

    companion object {
        fun fromStoredValue(value: String?): StudyMode =
            entries.firstOrNull { it.storedValue == value } ?: STANDARD
    }
}

interface StudyPreferenceStore {
    fun getString(key: String): String?
    fun putString(key: String, value: String)
}

/** Per-deck study preferences, kept independent of the Room schema. */
class DeckStudyPreferences(private val store: StudyPreferenceStore) {
    fun getMode(deckId: Long): StudyMode = StudyMode.fromStoredValue(store.getString(modeKey(deckId)))

    fun setMode(deckId: Long, mode: StudyMode) {
        store.putString(modeKey(deckId), mode.storedValue)
    }

    fun isAutoplayEnabled(deckId: Long): Boolean = store.getString(autoplayKey(deckId)) != "false"

    fun setAutoplayEnabled(deckId: Long, enabled: Boolean) {
        store.putString(autoplayKey(deckId), enabled.toString())
    }

    private fun modeKey(deckId: Long) = "study_mode_$deckId"
    private fun autoplayKey(deckId: Long) = "study_autoplay_$deckId"
}

fun pronunciationSlowRate(selectedRate: Float): Float = selectedRate * 0.6f
