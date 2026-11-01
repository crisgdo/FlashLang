package com.cristiano.flashlang

import android.app.Application
import androidx.room.Room
import com.cristiano.flashlang.data.local.AppDatabase
import com.cristiano.flashlang.data.repository.DeckRepository

class FlashLangApplication : Application() {
    val database: AppDatabase by lazy {
        Room.databaseBuilder(this, AppDatabase::class.java, "flashlang.db")
            .addMigrations(AppDatabase.MIGRATION_1_2, AppDatabase.MIGRATION_2_3)
            .build()
    }

    val deckRepository: DeckRepository by lazy { DeckRepository(database.deckDao()) }
}
