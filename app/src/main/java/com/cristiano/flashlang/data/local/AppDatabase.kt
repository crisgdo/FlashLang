package com.cristiano.flashlang.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.cristiano.flashlang.data.model.Card
import com.cristiano.flashlang.data.model.Deck

@Database(entities = [Deck::class, Card::class], version = 3, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun deckDao(): DeckDao

    companion object {
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE cards ADD COLUMN easeFactor REAL NOT NULL DEFAULT 2.5")
                db.execSQL("ALTER TABLE cards ADD COLUMN intervalDays INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE cards ADD COLUMN repetitions INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE cards ADD COLUMN dueAt INTEGER NOT NULL DEFAULT 0")
                db.execSQL("UPDATE cards SET dueAt = ${System.currentTimeMillis()}")
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE cards ADD COLUMN ipa TEXT")
                db.execSQL("ALTER TABLE cards ADD COLUMN note TEXT")
            }
        }
    }
}
