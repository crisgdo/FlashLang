package com.cristiano.flashlang.ui.study

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class StudyPreferencesTest {
    @Test
    fun storesModeAndAutoplayIndependentlyForEachDeck() {
        val store = FakeStudyPreferenceStore()
        val preferences = DeckStudyPreferences(store)

        assertEquals(StudyMode.STANDARD, preferences.getMode(10))
        assertTrue(preferences.isAutoplayEnabled(10))
        preferences.setMode(10, StudyMode.PRONUNCIATION)
        preferences.setAutoplayEnabled(10, false)

        assertEquals(StudyMode.PRONUNCIATION, preferences.getMode(10))
        assertFalse(preferences.isAutoplayEnabled(10))
        assertEquals(StudyMode.STANDARD, preferences.getMode(11))
        assertTrue(preferences.isAutoplayEnabled(11))

        preferences.setMode(11, StudyMode.INVERTED)
        assertEquals(StudyMode.INVERTED, preferences.getMode(11))
    }

    @Test
    fun reducedRateIsSixtyPercentOfChosenRateWithoutChangingIt() {
        val chosenRates = listOf(0.75f, 1f, 1.25f)
        val reducedRates = chosenRates.map(::pronunciationSlowRate)

        assertEquals(0.45f, reducedRates[0], 0.0001f)
        assertEquals(0.6f, reducedRates[1], 0.0001f)
        assertEquals(0.75f, reducedRates[2], 0.0001f)
        assertEquals(listOf(0.75f, 1f, 1.25f), chosenRates)
    }

    private class FakeStudyPreferenceStore : StudyPreferenceStore {
        private val values = mutableMapOf<String, String>()
        override fun getString(key: String): String? = values[key]
        override fun putString(key: String, value: String) { values[key] = value }
    }
}
