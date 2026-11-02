package com.cristiano.flashlang.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.cristiano.flashlang.data.model.DeckListItem
import com.cristiano.flashlang.data.model.DeckCategoryCount
import com.cristiano.flashlang.data.repository.DeckRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.time.Clock

class HomeViewModel(
    private val repository: DeckRepository,
    private val clock: Clock = Clock.systemDefaultZone(),
    private val tickIntervalMillis: Long = 30_000L,
    timeFlow: Flow<Long>? = null,
    coroutineScope: CoroutineScope? = null,
) : ViewModel() {
    private val scope: CoroutineScope = coroutineScope ?: viewModelScope

    private val ticker: Flow<Long> = timeFlow ?: flow {
        while (currentCoroutineContext().isActive) {
            emit(clock.millis())
            delay(tickIntervalMillis)
        }
    }

    val decks: StateFlow<List<DeckListItem>> = repository.observeDecksWithCardCount(ticker)
        .stateIn(scope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val categories: StateFlow<List<DeckCategoryCount>> = repository.observeCategoryCounts()
        .stateIn(scope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun deleteDeck(deckId: Long) {
        scope.launch { repository.deleteDeck(deckId) }
    }

    fun setDeckCategory(deckId: Long, category: String?) {
        scope.launch { repository.setDeckCategory(deckId, category) }
    }

    fun renameCategory(oldCategory: String, newCategory: String) {
        scope.launch { repository.renameCategory(oldCategory, newCategory) }
    }

    fun removeCategory(category: String) {
        scope.launch { repository.removeCategory(category) }
    }

    class Factory(
        private val repository: DeckRepository,
        private val clock: Clock = Clock.systemDefaultZone(),
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            require(modelClass.isAssignableFrom(HomeViewModel::class.java))
            return HomeViewModel(repository, clock) as T
        }
    }
}
