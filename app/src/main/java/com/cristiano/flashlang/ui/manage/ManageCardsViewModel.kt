package com.cristiano.flashlang.ui.manage

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

class ManageCardsViewModel(private val repository: DeckRepository, private val deckId: Long) : ViewModel() {
    private val searchQuery = MutableStateFlow("")
    private val _deckName = MutableStateFlow("")
    val deckName: StateFlow<String> = _deckName

    val cards: StateFlow<List<Card>> = combine(repository.observeCards(deckId), searchQuery) { cards, query ->
        val normalizedQuery = query.trim()
        if (normalizedQuery.isEmpty()) cards
        else cards.filter {
            it.term.contains(normalizedQuery, ignoreCase = true) ||
                it.termTranslation.contains(normalizedQuery, ignoreCase = true)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    init {
        viewModelScope.launch {
            _deckName.value = repository.getDecks().firstOrNull { it.id == deckId }?.name.orEmpty()
        }
    }

    fun updateSearch(value: String) {
        searchQuery.value = value
    }

    fun renameDeck(name: String) {
        val normalizedName = name.trim()
        if (normalizedName.isBlank()) return
        viewModelScope.launch {
            repository.renameDeck(deckId, normalizedName)
            _deckName.value = normalizedName
        }
    }

    class Factory(private val repository: DeckRepository, private val deckId: Long) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            require(modelClass.isAssignableFrom(ManageCardsViewModel::class.java))
            return ManageCardsViewModel(repository, deckId) as T
        }
    }
}
