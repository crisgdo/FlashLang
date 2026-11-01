package com.cristiano.flashlang.data.repository

import com.cristiano.flashlang.data.local.UserPromptDao
import com.cristiano.flashlang.data.model.UserPrompt
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class UserPromptRepositoryTest {
    @Test
    fun createReadUpdateDuplicateAndDeletePrompts() = runBlocking {
        val repository = UserPromptRepository(FakeUserPromptDao())
        val id = repository.create("  Meu prompt  ", "  descrição ", "  {{TEXTO}}  ", generatesDeck = false)
        val created = repository.getById(id)!!

        assertEquals("Meu prompt", created.title)
        assertEquals("descrição", created.description)
        assertEquals("{{TEXTO}}", created.body)
        assertFalse(created.generatesDeck)
        assertEquals(created.createdAt, created.updatedAt)

        repository.update(id, "Prompt atualizado", "", "{{IDIOMA}} {{LISTA}}", true)
        val updated = repository.getById(id)!!
        assertEquals("Prompt atualizado", updated.title)
        assertTrue(updated.generatesDeck)
        assertTrue(updated.updatedAt >= created.updatedAt)

        val copyId = repository.duplicate(updated)
        val copy = repository.getById(copyId)!!
        assertNotEquals(updated.id, copy.id)
        assertEquals("Cópia de Prompt atualizado", copy.title)
        assertEquals(updated.body, copy.body)
        assertEquals(updated.generatesDeck, copy.generatesDeck)

        repository.delete(id)
        assertEquals(null, repository.getById(id))
        assertNotNull(repository.getById(copyId))
    }

    private class FakeUserPromptDao : UserPromptDao {
        private val prompts = MutableStateFlow<List<UserPrompt>>(emptyList())
        private var nextId = 1L

        override suspend fun insert(prompt: UserPrompt): Long {
            val id = nextId++
            prompts.value = prompts.value + prompt.copy(id = id)
            return id
        }

        override suspend fun update(prompt: UserPrompt) {
            prompts.value = prompts.value.map { if (it.id == prompt.id) prompt else it }
        }

        override suspend fun delete(prompt: UserPrompt) {
            deleteById(prompt.id)
        }

        override fun observeAll(): Flow<List<UserPrompt>> = prompts

        override suspend fun getAll(): List<UserPrompt> = prompts.value

        override suspend fun getById(id: Long): UserPrompt? = prompts.value.firstOrNull { it.id == id }

        override suspend fun deleteById(id: Long) {
            prompts.value = prompts.value.filterNot { it.id == id }
        }
    }
}
