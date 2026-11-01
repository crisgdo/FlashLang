package com.cristiano.flashlang.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user_prompts")
data class UserPrompt(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val description: String = "",
    val body: String,
    val generatesDeck: Boolean = true,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = createdAt,
)
