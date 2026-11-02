package com.cristiano.flashlang.ui.home

import com.cristiano.flashlang.data.local.DeckDao
import com.cristiano.flashlang.data.model.Card
import com.cristiano.flashlang.data.model.Deck
import com.cristiano.flashlang.data.model.DeckCategoryCount
import com.cristiano.flashlang.data.model.DeckListItem
import com.cristiano.flashlang.data.repository.DeckRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class HomeViewModelTest {

    @Test
    fun decks_updatesPendingCountAsTimeFlowAdvances() = runBlocking {
        val dao = FakeDeckDao()
        val repository = DeckRepository(dao)
        val deckId = repository.insertDeck(Deck(name = "Espanhol", language = "es"))
        dao.insertCards(
            listOf(
                Card(
                    id = 1,
                    deckId = deckId,
                    term = "hola",
                    type = "word",
                    termTranslation = "olá",
                    example = "Hola mundo",
                    exampleTranslation = "Olá mundo",
                    dueAt = 1000L,
                ),
            ),
        )

        val timeFlow = MutableStateFlow(500L)
        val testScope = CoroutineScope(Job() + Dispatchers.Unconfined)
        try {
            val viewModel = HomeViewModel(
                repository = repository,
                timeFlow = timeFlow,
                coroutineScope = testScope,
            )

            val initialDecks = viewModel.decks.first { it.isNotEmpty() }
            assertEquals(1, initialDecks.size)
            assertEquals(1, initialDecks.first().cardCount)
            assertEquals(0, initialDecks.first().pendingCount)

            timeFlow.value = 1500L

            val updatedDecks = viewModel.decks.first { it.first().pendingCount == 1 }
            assertEquals(1, updatedDecks.size)
            assertEquals(1, updatedDecks.first().pendingCount)
        } finally {
            testScope.cancel()
        }
    }

    @Test
    fun viewModel_actionsDelegateToRepository() = runBlocking {
        val dao = FakeDeckDao()
        val repository = DeckRepository(dao)
        val deckId = repository.insertDeck(Deck(name = "Inglês", language = "en", category = "Básico"))

        val timeFlow = MutableStateFlow(1000L)
        val testScope = CoroutineScope(Job() + Dispatchers.Unconfined)
        try {
            val viewModel = HomeViewModel(
                repository = repository,
                timeFlow = timeFlow,
                coroutineScope = testScope,
            )

            viewModel.setDeckCategory(deckId, "Avançado")
            assertEquals("Avançado", repository.getDecks().first { it.id == deckId }.category)

            viewModel.renameCategory("Avançado", "Fluência")
            assertEquals("Fluência", repository.getDecks().first { it.id == deckId }.category)

            viewModel.removeCategory("Fluência")
            assertEquals(null, repository.getDecks().first { it.id == deckId }.category)

            viewModel.deleteDeck(deckId)
            assertTrue(repository.getDecks().isEmpty())
        } finally {
            testScope.cancel()
        }
    }

    @Test
    fun factory_createsHomeViewModelSuccessfully() {
        val dao = FakeDeckDao()
        val repository = DeckRepository(dao)
        val factory = HomeViewModel.Factory(repository)
        val viewModel = factory.create(HomeViewModel::class.java)
        assertNotNull(viewModel)
    }

    private class FakeDeckDao : DeckDao {
        private val decks = mutableListOf<Deck>()
        private val cards = mutableListOf<Card>()
        private var nextDeckId = 1L
        private var nextCardId = 1L

        override suspend fun insertDeck(deck: Deck): Long {
            val id = nextDeckId++
            decks += deck.copy(id = id)
            return id
        }

        override suspend fun insertCards(cards: List<Card>) {
            cards.forEach { this.cards += it.copy(id = nextCardId++) }
        }

        override suspend fun insertCard(card: Card): Long {
            val id = nextCardId++
            cards += card.copy(id = id)
            return id
        }

        override suspend fun insertDeckWithCards(deck: Deck, cards: List<Card>): Long {
            val id = insertDeck(deck)
            insertCards(cards.map { it.copy(id = 0, deckId = id) })
            return id
        }

        override fun observeDecks(): Flow<List<Deck>> = MutableStateFlow(decks.toList())

        override fun observeDecksWithCardCount(now: Long): Flow<List<DeckListItem>> =
            MutableStateFlow(decks.map { deck ->
                DeckListItem(
                    deck,
                    cards.count { it.deckId == deck.id },
                    cards.count { it.deckId == deck.id && it.dueAt <= now },
                )
            })

        override suspend fun getDecks(): List<Deck> = decks.toList()

        override fun observeCategoryCounts(): Flow<List<DeckCategoryCount>> = MutableStateFlow(
            decks.groupBy { it.category?.trim()?.lowercase() ?: "" }.map { (_, groupedDecks) ->
                DeckCategoryCount(groupedDecks.first().category, groupedDecks.size)
            },
        )

        override suspend fun findCategory(category: String): String? = decks.firstOrNull {
            it.category?.trim()?.equals(category.trim(), ignoreCase = true) == true
        }?.category

        override fun observeCards(deckId: Long): Flow<List<Card>> = MutableStateFlow(cards.filter { it.deckId == deckId })
        override suspend fun getCards(deckId: Long): List<Card> = cards.filter { it.deckId == deckId }
        override suspend fun getCard(cardId: Long): Card? = cards.firstOrNull { it.id == cardId }
        override suspend fun getDueCards(deckId: Long, now: Long): List<Card> = cards.filter { it.deckId == deckId && it.dueAt <= now }

        override suspend fun updateCard(card: Card) {
            val index = cards.indexOfFirst { it.id == card.id }
            if (index >= 0) cards[index] = card
        }

        override suspend fun deleteCard(cardId: Long) {
            cards.removeAll { it.id == cardId }
        }

        override suspend fun deleteCardsForDeck(deckId: Long) {
            cards.removeAll { it.deckId == deckId }
        }

        override suspend fun updateDeckContent(deckId: Long, name: String, language: String, category: String?) {
            val index = decks.indexOfFirst { it.id == deckId }
            if (index >= 0) decks[index] = decks[index].copy(name = name, language = language, category = category)
        }

        override suspend fun renameDeck(deckId: Long, name: String) {
            val index = decks.indexOfFirst { it.id == deckId }
            if (index >= 0) decks[index] = decks[index].copy(name = name)
        }

        override suspend fun updateDeckCategory(deckId: Long, category: String?) {
            val index = decks.indexOfFirst { it.id == deckId }
            if (index >= 0) decks[index] = decks[index].copy(category = category)
        }

        override suspend fun renameCategory(oldCategory: String, newCategory: String) {
            decks.indices.forEach { index ->
                if (decks[index].category?.equals(oldCategory, ignoreCase = true) == true) {
                    decks[index] = decks[index].copy(category = newCategory)
                }
            }
        }

        override suspend fun removeCategory(category: String) {
            decks.indices.forEach { index ->
                if (decks[index].category?.equals(category, ignoreCase = true) == true) {
                    decks[index] = decks[index].copy(category = null)
                }
            }
        }

        override suspend fun replaceDeck(deckId: Long, deck: Deck, cards: List<Card>) {
            updateDeckContent(deckId, deck.name, deck.language, deck.category)
            deleteCardsForDeck(deckId)
            insertCards(cards.map { it.copy(id = 0, deckId = deckId) })
        }

        override suspend fun deleteDeck(deckId: Long) {
            decks.removeAll { it.id == deckId }
            cards.removeAll { it.deckId == deckId }
        }
    }
}
