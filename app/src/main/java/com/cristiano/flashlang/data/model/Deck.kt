package com.cristiano.flashlang.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "decks")
data class Deck(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val language: String,
    val createdAt: Long = System.currentTimeMillis(),
    val category: String? = null,
)
