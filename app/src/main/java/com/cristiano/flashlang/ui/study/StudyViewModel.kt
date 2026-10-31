package com.cristiano.flashlang.ui.study

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.cristiano.flashlang.data.model.Card
import com.cristiano.flashlang.data.repository.DeckRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class StudyUiState(
    val cards: List<Card> = emptyList(),
    val currentIndex: Int = 0,
    val isFlipped: Boolean = false,
    val language: String = "en",
)

class StudyViewModel(repository: DeckRepository, deckId: Long) : ViewModel() {
    private val currentIndex = MutableStateFlow(0)
    private val isFlipped = MutableStateFlow(false)
    private val language = MutableStateFlow("en")

    init {
        viewModelScope.launch {
            language.value = repository.getDecks().firstOrNull { it.id == deckId }?.language ?: "en"
        }
    }

    val state: StateFlow<StudyUiState> = combine(
        repository.observeCards(deckId), currentIndex, isFlipped, language,
    ) { cards, index, flipped, deckLanguage ->
        StudyUiState(cards = cards, currentIndex = index.coerceIn(0, (cards.size - 1).coerceAtLeast(0)), isFlipped = flipped, language = deckLanguage)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), StudyUiState())

    fun flipCard() {
        isFlipped.value = !isFlipped.value
    }

    fun showPrevious() {
        val cards = state.value.cards
        if (currentIndex.value > 0 && cards.isNotEmpty()) {
            currentIndex.value--
            isFlipped.value = false
        }
    }

    fun showNext() {
        val cards = state.value.cards
        if (currentIndex.value < cards.lastIndex) {
            currentIndex.value++
            isFlipped.value = false
        }
    }

    class Factory(private val repository: DeckRepository, private val deckId: Long) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            require(modelClass.isAssignableFrom(StudyViewModel::class.java))
            return StudyViewModel(repository, deckId) as T
        }
    }
}
