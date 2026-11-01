package com.cristiano.flashlang.data.backup

import com.cristiano.flashlang.data.exporter.DeckExporter
import com.cristiano.flashlang.data.importer.DeckImporter
import com.cristiano.flashlang.data.model.Card
import com.cristiano.flashlang.data.model.Deck
import com.cristiano.flashlang.data.model.UserPrompt
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

class BackupServiceTest {
    private val fixedClock = Clock.fixed(Instant.parse("2026-04-05T12:30:00Z"), ZoneOffset.UTC)

    @Test
    fun exportThenRestorePreservesDeckCardsProgressCategoriesAndUserPrompts() = runBlocking {
        val source = populatedStore()
        val export = BackupService(source, clock = fixedClock).createExport(BackupScope.ALL, includeProgress = true)
        val target = FakeBackupDataStore()
        val service = BackupService(target, clock = fixedClock)
        val preview = service.prepareRestore(export.json)

        service.restore(preview, RestoreConflictChoice.KEEP_BOTH)

        val deck = target.getDecks().single()
        val card = target.getCards(deck.id).single()
        val prompt = target.getPrompts().single()
        assertEquals("Pronúncia", deck.category)
        assertEquals("walked", card.term)
        assertEquals("cloze", card.format)
        assertEquals("/wɔkt/", card.ipa)
        assertEquals("base: walk", card.note)
        assertEquals(1.8, card.easeFactor, 0.0)
        assertEquals(9, card.intervalDays)
        assertEquals(4, card.repetitions)
        assertEquals(1_800_000_000_000L, card.dueAt)
        assertEquals("Meus verbos", prompt.title)
        assertFalse(prompt.generatesDeck)
    }

    @Test
    fun exportWithoutProgressRestoresCardsAsPending() = runBlocking {
        val export = BackupService(populatedStore(), clock = fixedClock)
            .createExport(BackupScope.DECKS, includeProgress = false)
        assertFalse(export.json.contains("\"progress\""))
        val target = FakeBackupDataStore()
        BackupService(target, clock = fixedClock).let { service ->
            service.restore(service.prepareRestore(export.json), RestoreConflictChoice.KEEP_BOTH)
        }
        val restoredCard = target.getCards(target.getDecks().single().id).single()
        assertEquals(2.5, restoredCard.easeFactor, 0.0)
        assertEquals(0, restoredCard.intervalDays)
        assertEquals(0, restoredCard.repetitions)
    }

    @Test
    fun exportsAndRestoresOnlyOwnPrompts() = runBlocking {
        val export = BackupService(populatedStore(), clock = fixedClock)
            .createExport(BackupScope.PROMPTS, includeProgress = true)
        assertFalse(export.json.contains("\"deckName\""))
        assertTrue(export.json.contains("\"prompts\""))

        val target = FakeBackupDataStore()
        val service = BackupService(target, clock = fixedClock)
        service.restore(service.prepareRestore(export.json), RestoreConflictChoice.KEEP_BOTH)

        assertTrue(target.getDecks().isEmpty())
        assertEquals("Meus verbos", target.getPrompts().single().title)
    }

    @Test
    fun acceptsLegacySingleDeckAndRejectsInvalidFile() = runBlocking {
        val legacy = DeckExporter.toJson(
            Deck(name = "Legacy", language = "en", category = "Leitura"),
            listOf(card(deckId = 0)),
        )
        val service = BackupService(FakeBackupDataStore(), clock = fixedClock)
        val preview = service.prepareRestore(legacy)
        assertEquals(1, preview.decks.size)
        assertEquals("Leitura", preview.decks.single().deck.category)

        val error = runCatching { service.prepareRestore("{isso não é JSON") }.exceptionOrNull()
        assertTrue(error is BackupException)
        assertTrue(error?.message.orEmpty().contains("inválido"))
    }

    @Test
    fun appliesKeepBothReplaceAndSkipToDeckAndPromptConflicts() = runBlocking {
        val export = BackupService(populatedStore(), clock = fixedClock)
            .createExport(BackupScope.ALL, includeProgress = true)

        val keepStore = conflictStore()
        val keepService = BackupService(keepStore, clock = fixedClock)
        val keepPreview = keepService.prepareRestore(export.json)
        assertEquals(1, keepPreview.deckConflicts)
        assertEquals(1, keepPreview.promptConflicts)
        keepService.restore(keepPreview, RestoreConflictChoice.KEEP_BOTH)
        assertTrue(keepStore.getDecks().any { it.name == "Travel (2)" })
        assertTrue(keepStore.getPrompts().any { it.title == "Meus verbos (2)" })

        val replaceStore = conflictStore()
        val replaceService = BackupService(replaceStore, clock = fixedClock)
        replaceService.restore(replaceService.prepareRestore(export.json), RestoreConflictChoice.REPLACE)
        assertEquals(1, replaceStore.getDecks().size)
        assertEquals("walked", replaceStore.getCards(replaceStore.getDecks().single().id).single().term)
        assertEquals("Gere cards cloze.", replaceStore.getPrompts().single().body)

        val skipStore = conflictStore()
        val skipService = BackupService(skipStore, clock = fixedClock)
        skipService.restore(skipService.prepareRestore(export.json), RestoreConflictChoice.SKIP)
        assertEquals("old", skipStore.getCards(skipStore.getDecks().single().id).single().term)
        assertEquals("prompt antigo", skipStore.getPrompts().single().body)
    }

    @Test
    fun restoreIsAtomicWhenAnInsertFails() = runBlocking {
        val export = BackupService(populatedStore(), clock = fixedClock)
            .createExport(BackupScope.ALL, includeProgress = true)
        val target = conflictStore().apply { failOnPromptInsert = true }
        val service = BackupService(target, clock = fixedClock)
        val preview = service.prepareRestore(export.json)
        val beforeDecks = target.getDecks()
        val beforeCards = target.getCards(beforeDecks.single().id)
        val beforePrompts = target.getPrompts()

        val result = runCatching { service.restore(preview, RestoreConflictChoice.KEEP_BOTH) }

        assertTrue(result.isFailure)
        assertEquals(beforeDecks, target.getDecks())
        assertEquals(beforeCards, target.getCards(beforeDecks.single().id))
        assertEquals(beforePrompts, target.getPrompts())
    }

    private suspend fun populatedStore(): FakeBackupDataStore = FakeBackupDataStore().apply {
        insertDeckWithCards(
            Deck(name = "Travel", language = "en", category = "Pronúncia"),
            listOf(card(deckId = 0)),
        )
        insertPrompt(UserPrompt(title = "Meus verbos", description = "Notas", body = "Gere cards cloze.", generatesDeck = false))
    }

    private suspend fun conflictStore(): FakeBackupDataStore = FakeBackupDataStore().apply {
        insertDeckWithCards(
            Deck(name = "Travel", language = "en", category = "Old"),
            listOf(card(deckId = 0).copy(term = "old", easeFactor = 2.3)),
        )
        insertPrompt(UserPrompt(title = "Meus verbos", body = "prompt antigo"))
    }

    private fun card(deckId: Long): Card = Card(
        deckId = deckId,
        term = "walked",
        type = "word",
        termTranslation = "andou",
        example = "I ___ home.",
        exampleTranslation = "Eu fui para casa.",
        ipa = "/wɔkt/",
        note = "base: walk",
        format = "cloze",
        easeFactor = 1.8,
        intervalDays = 9,
        repetitions = 4,
        dueAt = 1_800_000_000_000L,
    )

    private class FakeBackupDataStore : BackupDataStore {
        private var nextDeckId = 1L
        private var nextCardId = 1L
        private var nextPromptId = 1L
        private val decks = mutableListOf<Deck>()
        private val cards = mutableListOf<Card>()
        private val prompts = mutableListOf<UserPrompt>()
        var failOnPromptInsert: Boolean = false

        override suspend fun <T> transaction(block: suspend BackupDataStore.() -> T): T {
            val savedDecks = decks.toList()
            val savedCards = cards.toList()
            val savedPrompts = prompts.toList()
            val savedDeckId = nextDeckId
            val savedCardId = nextCardId
            val savedPromptId = nextPromptId
            return try {
                block(this)
            } catch (exception: Exception) {
                decks.clear(); decks.addAll(savedDecks)
                cards.clear(); cards.addAll(savedCards)
                prompts.clear(); prompts.addAll(savedPrompts)
                nextDeckId = savedDeckId
                nextCardId = savedCardId
                nextPromptId = savedPromptId
                throw exception
            }
        }

        override suspend fun getDecks(): List<Deck> = decks.toList()
        override suspend fun getCards(deckId: Long): List<Card> = cards.filter { it.deckId == deckId }
        override suspend fun getPrompts(): List<UserPrompt> = prompts.toList()

        override suspend fun insertDeckWithCards(deck: Deck, cards: List<Card>) {
            val deckId = nextDeckId++
            decks += deck.copy(id = deckId)
            cards.forEach { this.cards += it.copy(id = nextCardId++, deckId = deckId) }
        }

        override suspend fun deleteDeck(deckId: Long) {
            decks.removeAll { it.id == deckId }
            cards.removeAll { it.deckId == deckId }
        }

        override suspend fun insertPrompt(prompt: UserPrompt) {
            if (failOnPromptInsert) error("falha simulada ao inserir prompt")
            prompts += prompt.copy(id = nextPromptId++)
        }

        override suspend fun deletePrompt(promptId: Long) { prompts.removeAll { it.id == promptId } }
    }
}
