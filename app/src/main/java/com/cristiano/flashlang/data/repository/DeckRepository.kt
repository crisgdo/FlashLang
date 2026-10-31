package com.cristiano.flashlang.data.repository

import com.cristiano.flashlang.data.local.DeckDao
import com.cristiano.flashlang.data.model.Card
import com.cristiano.flashlang.data.model.Deck
import com.cristiano.flashlang.data.model.DeckListItem
import kotlinx.coroutines.flow.Flow

class DeckRepository(private val deckDao: DeckDao) {
    suspend fun insertDeck(deck: Deck, cards: List<Card> = emptyList()): Long =
        deckDao.insertDeckWithCards(deck, cards)

    fun observeDecks(): Flow<List<Deck>> = deckDao.observeDecks()

    fun observeDecksWithCardCount(): Flow<List<DeckListItem>> = deckDao.observeDecksWithCardCount()

    suspend fun getDecks(): List<Deck> = deckDao.getDecks()

    fun observeCards(deckId: Long): Flow<List<Card>> = deckDao.observeCards(deckId)

    suspend fun getCards(deckId: Long): List<Card> = deckDao.getCards(deckId)

    suspend fun deleteDeck(deckId: Long) = deckDao.deleteDeck(deckId)
}
