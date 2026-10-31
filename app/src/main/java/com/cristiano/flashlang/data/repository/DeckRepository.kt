package com.cristiano.flashlang.data.repository

import com.cristiano.flashlang.data.local.DeckDao
import com.cristiano.flashlang.data.model.Card
import com.cristiano.flashlang.data.model.Deck
import com.cristiano.flashlang.data.model.DeckListItem
import kotlinx.coroutines.flow.Flow

class DeckRepository(private val deckDao: DeckDao) {
    suspend fun insertDeck(deck: Deck, cards: List<Card> = emptyList()): Long {
        val createdAt = System.currentTimeMillis()
        val newCards = cards.map { card ->
            card.copy(
                id = 0,
                easeFactor = 2.5,
                intervalDays = 0,
                repetitions = 0,
                dueAt = createdAt,
            )
        }
        return deckDao.insertDeckWithCards(deck, newCards)
    }

    fun observeDecks(): Flow<List<Deck>> = deckDao.observeDecks()

    fun observeDecksWithCardCount(now: Long = System.currentTimeMillis()): Flow<List<DeckListItem>> =
        deckDao.observeDecksWithCardCount(now)

    suspend fun getDecks(): List<Deck> = deckDao.getDecks()

    suspend fun findDeckByName(name: String): Deck? =
        deckDao.getDecks().firstOrNull { it.name.equals(name.trim(), ignoreCase = true) }

    suspend fun replaceDeck(deckId: Long, deck: Deck, cards: List<Card>) {
        val createdAt = System.currentTimeMillis()
        deckDao.replaceDeck(
            deckId,
            deck,
            cards.map { card ->
                card.copy(id = 0, deckId = deckId, easeFactor = 2.5, intervalDays = 0, repetitions = 0, dueAt = createdAt)
            },
        )
    }

    fun observeCards(deckId: Long): Flow<List<Card>> = deckDao.observeCards(deckId)

    suspend fun getCards(deckId: Long): List<Card> = deckDao.getCards(deckId)

    suspend fun getCard(cardId: Long): Card? = deckDao.getCard(cardId)

    suspend fun addCard(
        deckId: Long,
        term: String,
        type: String,
        termTranslation: String,
        example: String,
        exampleTranslation: String,
    ): Long = deckDao.insertCard(
        Card(
            deckId = deckId,
            term = term,
            type = type,
            termTranslation = termTranslation,
            example = example,
            exampleTranslation = exampleTranslation,
            easeFactor = 2.5,
            intervalDays = 0,
            repetitions = 0,
            dueAt = System.currentTimeMillis(),
        ),
    )

    suspend fun updateCardContent(
        cardId: Long,
        term: String,
        type: String,
        termTranslation: String,
        example: String,
        exampleTranslation: String,
    ) {
        val existing = deckDao.getCard(cardId) ?: return
        deckDao.updateCard(
            existing.copy(
                term = term,
                type = type,
                termTranslation = termTranslation,
                example = example,
                exampleTranslation = exampleTranslation,
            ),
        )
    }

    suspend fun deleteCard(cardId: Long) = deckDao.deleteCard(cardId)

    suspend fun renameDeck(deckId: Long, name: String) = deckDao.renameDeck(deckId, name.trim())

    suspend fun getDueCards(deckId: Long, now: Long = System.currentTimeMillis()): List<Card> =
        deckDao.getDueCards(deckId, now)

    suspend fun updateCard(card: Card) = deckDao.updateCard(card)

    suspend fun deleteDeck(deckId: Long) = deckDao.deleteDeck(deckId)
}
