package com.cristiano.flashlang

import android.app.Application
import androidx.room.Room
import com.cristiano.flashlang.data.local.AppDatabase
import com.cristiano.flashlang.data.repository.DeckRepository
import com.cristiano.flashlang.data.repository.UserPromptRepository

class FlashLangApplication : Application() {
    val database: AppDatabase by lazy {
        Room.databaseBuilder(this, AppDatabase::class.java, "flashlang.db")
            .addMigrations(AppDatabase.MIGRATION_1_2, AppDatabase.MIGRATION_2_3, AppDatabase.MIGRATION_3_4, AppDatabase.MIGRATION_4_5)
            .build()
    }

    val deckRepository: DeckRepository by lazy { DeckRepository(database.deckDao()) }
    val userPromptRepository: UserPromptRepository by lazy { UserPromptRepository(database.userPromptDao()) }
}
