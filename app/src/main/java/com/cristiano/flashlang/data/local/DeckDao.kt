package com.cristiano.flashlang.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.cristiano.flashlang.data.model.Card
import com.cristiano.flashlang.data.model.Deck
import com.cristiano.flashlang.data.model.DeckWithCards
import kotlinx.coroutines.flow.Flow

@Dao
interface DeckDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertDeck(deck: Deck): Long

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertCards(cards: List<Card>)

    @Transaction
    suspend fun insertDeckWithCards(deck: Deck, cards: List<Card>): Long {
        val deckId = insertDeck(deck)
        if (cards.isNotEmpty()) insertCards(cards.map { it.copy(id = 0, deckId = deckId) })
        return deckId
    }

    @Query("SELECT * FROM decks ORDER BY createdAt DESC")
    fun observeDecks(): Flow<List<Deck>>

    @Query("SELECT * FROM decks ORDER BY createdAt DESC")
    suspend fun getDecks(): List<Deck>

    @Query("SELECT * FROM cards WHERE deckId = :deckId ORDER BY id")
    fun observeCards(deckId: Long): Flow<List<Card>>

    @Query("SELECT * FROM cards WHERE deckId = :deckId ORDER BY id")
    suspend fun getCards(deckId: Long): List<Card>

    @Query("DELETE FROM decks WHERE id = :deckId")
    suspend fun deleteDeck(deckId: Long)
}
