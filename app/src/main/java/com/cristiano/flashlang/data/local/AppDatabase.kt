package com.cristiano.flashlang.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.cristiano.flashlang.data.model.Card
import com.cristiano.flashlang.data.model.Deck

@Database(entities = [Deck::class, Card::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun deckDao(): DeckDao
}
