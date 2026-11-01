package com.cristiano.flashlang.data.model

import androidx.room.Entity
import androidx.room.ColumnInfo
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "cards",
    foreignKeys = [
        ForeignKey(
            entity = Deck::class,
            parentColumns = ["id"],
            childColumns = ["deckId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("deckId")],
)
data class Card(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val deckId: Long,
    val term: String,
    val type: String,
    val termTranslation: String,
    val example: String,
    val exampleTranslation: String,
    val ipa: String? = null,
    val note: String? = null,
    @ColumnInfo(defaultValue = "'basic'") val format: String = "basic",
    @ColumnInfo(defaultValue = "2.5") val easeFactor: Double = 2.5,
    @ColumnInfo(defaultValue = "0") val intervalDays: Int = 0,
    @ColumnInfo(defaultValue = "0") val repetitions: Int = 0,
    @ColumnInfo(defaultValue = "0") val dueAt: Long = System.currentTimeMillis(),
)
