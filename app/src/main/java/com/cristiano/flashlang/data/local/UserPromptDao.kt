package com.cristiano.flashlang.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.cristiano.flashlang.data.model.UserPrompt
import kotlinx.coroutines.flow.Flow

@Dao
interface UserPromptDao {
    @Insert
    suspend fun insert(prompt: UserPrompt): Long

    @Update
    suspend fun update(prompt: UserPrompt)

    @Delete
    suspend fun delete(prompt: UserPrompt)

    @Query("SELECT * FROM user_prompts ORDER BY updatedAt DESC, id DESC")
    fun observeAll(): Flow<List<UserPrompt>>

    @Query("SELECT * FROM user_prompts ORDER BY updatedAt DESC, id DESC")
    suspend fun getAll(): List<UserPrompt>

    @Query("SELECT * FROM user_prompts WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): UserPrompt?

    @Query("DELETE FROM user_prompts WHERE id = :id")
    suspend fun deleteById(id: Long)
}
