package com.cristiano.flashlang.data.repository

import com.cristiano.flashlang.data.local.DeckDao
import com.cristiano.flashlang.data.model.Card
import com.cristiano.flashlang.data.model.Deck
import com.cristiano.flashlang.data.model.DeckWithCards
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DeckRepositoryTest {
    @Test
    fun insertDeckWithCards_returnsIdAndPersistsAssociatedCards() = runBlocking {
        val dao = FakeDeckDao()
        val repository = DeckRepository(dao)
        val deck = Deck(name = "Viagem", language = "en", createdAt = 10)
        val card = Card(
            deckId = 0,
            term = "ticket",
            type = "word",
            termTranslation = "bilhete",
            example = "I need a ticket.",
            exampleTranslation = "Eu preciso de um bilhete.",
        )

        val deckId = repository.insertDeck(deck, listOf(card))

        assertEquals(1L, deckId)
        assertEquals(listOf(card.copy(id = 1, deckId = deckId)), repository.getCards(deckId))
    }

    @Test
    fun deleteDeck_removesDeckAndItsCards() = runBlocking {
        val dao = FakeDeckDao()
        val repository = DeckRepository(dao)
        val deckId = repository.insertDeck(Deck(name = "Espanhol", language = "es"))
        repository.insertDeck(
            Deck(name = "Outro", language = "es"),
            listOf(Card(deckId = 0, term = "hola", type = "word", termTranslation = "olá", example = "Hola.", exampleTranslation = "Olá.")),
        )

        repository.deleteDeck(deckId)

        assertTrue(repository.getDecks().none { it.id == deckId })
        assertTrue(repository.getCards(deckId).isEmpty())
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

        override suspend fun insertDeckWithCards(deck: Deck, cards: List<Card>): Long {
            val id = insertDeck(deck)
            insertCards(cards.map { it.copy(id = 0, deckId = id) })
            return id
        }

        override fun observeDecks(): Flow<List<Deck>> = MutableStateFlow(decks)
        override suspend fun getDecks(): List<Deck> = decks.toList()
        override fun observeCards(deckId: Long): Flow<List<Card>> = MutableStateFlow(cards.filter { it.deckId == deckId })
        override suspend fun getCards(deckId: Long): List<Card> = cards.filter { it.deckId == deckId }

        override suspend fun deleteDeck(deckId: Long) {
            decks.removeAll { it.id == deckId }
            cards.removeAll { it.deckId == deckId }
        }
    }
}
