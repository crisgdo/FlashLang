package com.cristiano.flashlang.data.repository

import com.cristiano.flashlang.data.local.UserPromptDao
import com.cristiano.flashlang.data.model.UserPrompt
import kotlinx.coroutines.flow.Flow

class UserPromptRepository(private val dao: UserPromptDao) {
    fun observeAll(): Flow<List<UserPrompt>> = dao.observeAll()

    suspend fun getById(id: Long): UserPrompt? = dao.getById(id)

    suspend fun create(
        title: String,
        description: String,
        body: String,
        generatesDeck: Boolean = true,
    ): Long {
        val now = System.currentTimeMillis()
        return dao.insert(
            UserPrompt(
                title = title.trim(),
                description = description.trim(),
                body = body.trim(),
                generatesDeck = generatesDeck,
                createdAt = now,
                updatedAt = now,
            ),
        )
    }

    suspend fun update(
        id: Long,
        title: String,
        description: String,
        body: String,
        generatesDeck: Boolean,
    ) {
        val current = dao.getById(id) ?: return
        dao.update(
            current.copy(
                title = title.trim(),
                description = description.trim(),
                body = body.trim(),
                generatesDeck = generatesDeck,
                updatedAt = System.currentTimeMillis(),
            ),
        )
    }

    suspend fun duplicate(prompt: UserPrompt, title: String = "Cópia de ${prompt.title}"): Long =
        create(title, prompt.description, prompt.body, prompt.generatesDeck)

    suspend fun duplicateTemplate(
        title: String,
        description: String,
        body: String,
        generatesDeck: Boolean,
    ): Long = create("Cópia de $title", description, body, generatesDeck)

    suspend fun delete(id: Long) = dao.deleteById(id)
}
