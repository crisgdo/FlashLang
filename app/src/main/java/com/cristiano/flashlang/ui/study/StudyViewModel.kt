package com.cristiano.flashlang.ui.study

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.cristiano.flashlang.data.model.Card
import com.cristiano.flashlang.data.repository.DeckRepository
import com.cristiano.flashlang.data.scheduler.CardScheduleState
import com.cristiano.flashlang.data.scheduler.ReviewRating
import com.cristiano.flashlang.data.scheduler.Scheduler
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class StudyUiState(
    val deckId: Long = 0,
    val deckName: String = "",
    val cards: List<Card> = emptyList(),
    val allCards: List<Card> = emptyList(),
    val currentIndex: Int = 0,
    val isFlipped: Boolean = false,
    val language: String = "en",
    val isAllCardsMode: Boolean = false,
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val sessionTotal: Int = 0,
    val errorMessage: String? = null,
)

class StudyViewModel(private val repository: DeckRepository, private val deckId: Long) : ViewModel() {
    private val _state = MutableStateFlow(StudyUiState(deckId = deckId))
    val state: StateFlow<StudyUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            val allCards = repository.getCards(deckId).shuffled()
            val pendingCards = repository.getDueCards(deckId).shuffled()
            val deck = repository.getDecks().firstOrNull { it.id == deckId }
            val deckName = deck?.name.orEmpty()
            val language = deck?.language ?: "en"
            _state.update {
                it.copy(
                    deckName = deckName,
                    cards = pendingCards,
                    allCards = allCards,
                    language = language,
                    isLoading = false,
                    sessionTotal = pendingCards.size,
                )
            }
        }
    }

    fun flipCard() {
        if (_state.value.cards.isNotEmpty() && !_state.value.isSaving) {
            _state.update { it.copy(isFlipped = !it.isFlipped) }
        }
    }

    fun rateCurrentCard(rating: ReviewRating) {
        val currentState = _state.value
        val card = currentState.cards.firstOrNull() ?: return
        if (!currentState.isFlipped || currentState.isSaving || currentState.isAllCardsMode) return

        _state.update { it.copy(isSaving = true, errorMessage = null) }
        viewModelScope.launch {
            try {
                val schedule = Scheduler.review(
                    current = CardScheduleState(card.easeFactor, card.intervalDays, card.repetitions, card.dueAt),
                    rating = rating,
                    now = System.currentTimeMillis(),
                )
                val updatedCard = card.copy(
                    easeFactor = schedule.easeFactor,
                    intervalDays = schedule.intervalDays,
                    repetitions = schedule.repetitions,
                    dueAt = schedule.dueAt,
                )
                repository.updateCard(updatedCard)
                _state.update { state ->
                    val remaining = state.cards.drop(1).toMutableList()
                    if (rating == ReviewRating.AGAIN) remaining += updatedCard
                    state.copy(cards = remaining, currentIndex = 0, isFlipped = false, isSaving = false)
                }
            } catch (_: Exception) {
                _state.update { it.copy(isSaving = false, errorMessage = "Não foi possível salvar a avaliação. Tente novamente.") }
            }
        }
    }

    fun studyAllCards() {
        val cards = _state.value.allCards
        if (cards.isNotEmpty()) {
            _state.update {
                it.copy(cards = cards, currentIndex = 0, isFlipped = false, isAllCardsMode = true, errorMessage = null)
            }
        }
    }

    fun showPrevious() {
        val current = _state.value
        if (current.isAllCardsMode && current.currentIndex > 0) {
            _state.update { it.copy(currentIndex = it.currentIndex - 1, isFlipped = false) }
        }
    }

    fun showNext() {
        val current = _state.value
        if (current.isAllCardsMode && current.currentIndex < current.cards.lastIndex) {
            _state.update { it.copy(currentIndex = it.currentIndex + 1, isFlipped = false) }
        }
    }

    fun refreshCard(cardId: Long) {
        viewModelScope.launch {
            val refreshedCard = repository.getCard(cardId)
            _state.update { current ->
                val cardStillPresent = refreshedCard != null
                val updatedCards = if (cardStillPresent) {
                    current.cards.map { if (it.id == cardId) refreshedCard!! else it }
                } else {
                    current.cards.filterNot { it.id == cardId }
                }
                val updatedAllCards = if (cardStillPresent) {
                    current.allCards.map { if (it.id == cardId) refreshedCard!! else it }
                } else {
                    current.allCards.filterNot { it.id == cardId }
                }
                current.copy(
                    cards = updatedCards,
                    allCards = updatedAllCards,
                    currentIndex = current.currentIndex.coerceAtMost((updatedCards.size - 1).coerceAtLeast(0)),
                    sessionTotal = if (!cardStillPresent && !current.isAllCardsMode) (current.sessionTotal - 1).coerceAtLeast(updatedCards.size) else current.sessionTotal,
                    isFlipped = false,
                )
            }
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
