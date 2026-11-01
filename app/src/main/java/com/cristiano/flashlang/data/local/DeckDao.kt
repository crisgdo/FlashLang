package com.cristiano.flashlang.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.cristiano.flashlang.data.model.Card
import com.cristiano.flashlang.data.model.Deck
import com.cristiano.flashlang.data.model.DeckWithCards
import com.cristiano.flashlang.data.model.DeckListItem
import com.cristiano.flashlang.data.model.DeckCategoryCount
import kotlinx.coroutines.flow.Flow

@Dao
interface DeckDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertDeck(deck: Deck): Long

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertCards(cards: List<Card>)

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertCard(card: Card): Long

    @Transaction
    suspend fun insertDeckWithCards(deck: Deck, cards: List<Card>): Long {
        val deckId = insertDeck(deck)
        if (cards.isNotEmpty()) insertCards(cards.map { it.copy(id = 0, deckId = deckId) })
        return deckId
    }

    @Query("SELECT * FROM decks ORDER BY createdAt DESC")
    fun observeDecks(): Flow<List<Deck>>

    @Query("SELECT decks.*, (SELECT COUNT(*) FROM cards WHERE cards.deckId = decks.id) AS cardCount, (SELECT COUNT(*) FROM cards WHERE cards.deckId = decks.id AND cards.dueAt <= :now) AS pendingCount FROM decks ORDER BY createdAt DESC")
    fun observeDecksWithCardCount(now: Long): Flow<List<DeckListItem>>

    @Query("SELECT * FROM decks ORDER BY createdAt DESC")
    suspend fun getDecks(): List<Deck>

    @Query("SELECT category, COUNT(*) AS deckCount FROM decks GROUP BY COALESCE(NULLIF(LOWER(TRIM(category)), ''), '')")
    fun observeCategoryCounts(): Flow<List<DeckCategoryCount>>

    @Query("SELECT category FROM decks WHERE category IS NOT NULL AND LOWER(TRIM(category)) = LOWER(TRIM(:category)) LIMIT 1")
    suspend fun findCategory(category: String): String?

    @Query("SELECT * FROM cards WHERE deckId = :deckId ORDER BY id")
    fun observeCards(deckId: Long): Flow<List<Card>>

    @Query("SELECT * FROM cards WHERE deckId = :deckId ORDER BY id")
    suspend fun getCards(deckId: Long): List<Card>

    @Query("SELECT * FROM cards WHERE id = :cardId LIMIT 1")
    suspend fun getCard(cardId: Long): Card?

    @Query("SELECT * FROM cards WHERE deckId = :deckId AND dueAt <= :now")
    suspend fun getDueCards(deckId: Long, now: Long): List<Card>

    @Update
    suspend fun updateCard(card: Card)

    @Query("DELETE FROM cards WHERE id = :cardId")
    suspend fun deleteCard(cardId: Long)

    @Query("DELETE FROM cards WHERE deckId = :deckId")
    suspend fun deleteCardsForDeck(deckId: Long)

    @Query("UPDATE decks SET name = :name, language = :language, category = :category WHERE id = :deckId")
    suspend fun updateDeckContent(deckId: Long, name: String, language: String, category: String?)

    @Query("UPDATE decks SET name = :name WHERE id = :deckId")
    suspend fun renameDeck(deckId: Long, name: String)

    @Query("UPDATE decks SET category = :category WHERE id = :deckId")
    suspend fun updateDeckCategory(deckId: Long, category: String?)

    @Query("UPDATE decks SET category = :newCategory WHERE category IS NOT NULL AND LOWER(TRIM(category)) = LOWER(TRIM(:oldCategory))")
    suspend fun renameCategory(oldCategory: String, newCategory: String)

    @Query("UPDATE decks SET category = NULL WHERE category IS NOT NULL AND LOWER(TRIM(category)) = LOWER(TRIM(:category))")
    suspend fun removeCategory(category: String)

    @Transaction
    suspend fun replaceDeck(deckId: Long, deck: Deck, cards: List<Card>) {
        updateDeckContent(deckId, deck.name, deck.language, deck.category)
        deleteCardsForDeck(deckId)
        if (cards.isNotEmpty()) insertCards(cards.map { it.copy(id = 0, deckId = deckId) })
    }

    @Query("DELETE FROM decks WHERE id = :deckId")
    suspend fun deleteDeck(deckId: Long)
}
