package com.cristiano.flashlang.data.backup

import androidx.room.withTransaction
import com.cristiano.flashlang.data.importer.DeckImportResult
import com.cristiano.flashlang.data.importer.DeckImporter
import com.cristiano.flashlang.data.importer.ImportCardJson
import com.cristiano.flashlang.data.importer.ImportDeckJson
import com.cristiano.flashlang.data.local.AppDatabase
import com.cristiano.flashlang.data.model.Card
import com.cristiano.flashlang.data.model.Deck
import com.cristiano.flashlang.data.model.UserPrompt
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.decodeFromJsonElement
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.format.DateTimeFormatter

enum class BackupScope { ALL, DECKS, PROMPTS }
enum class RestoreConflictChoice { KEEP_BOTH, REPLACE, SKIP }

data class BackupCounts(val deckCount: Int, val cardCount: Int, val promptCount: Int)
data class BackupExport(val json: String, val suggestedFileName: String)
data class RestoreDeckPlan(val deck: ImportDeckJson, val progress: List<BackupProgressJson?>)
data class BackupRestorePreview(
    val decks: List<RestoreDeckPlan>,
    val prompts: List<BackupPromptJson>,
    val deckConflicts: Int,
    val promptConflicts: Int,
) {
    val cardCount: Int get() = decks.sumOf { it.deck.cards.size }
    val conflictCount: Int get() = deckConflicts + promptConflicts
}

interface BackupDataStore {
    suspend fun <T> transaction(block: suspend BackupDataStore.() -> T): T
    suspend fun getDecks(): List<Deck>
    suspend fun getCards(deckId: Long): List<Card>
    suspend fun getPrompts(): List<UserPrompt>
    suspend fun insertDeckWithCards(deck: Deck, cards: List<Card>)
    suspend fun deleteDeck(deckId: Long)
    suspend fun insertPrompt(prompt: UserPrompt)
    suspend fun deletePrompt(promptId: Long)
}

class RoomBackupDataStore(private val database: AppDatabase) : BackupDataStore {
    override suspend fun <T> transaction(block: suspend BackupDataStore.() -> T): T =
        database.withTransaction { block(this@RoomBackupDataStore) }

    override suspend fun getDecks(): List<Deck> = database.deckDao().getDecks()
    override suspend fun getCards(deckId: Long): List<Card> = database.deckDao().getCards(deckId)
    override suspend fun getPrompts(): List<UserPrompt> = database.userPromptDao().getAll()

    override suspend fun insertDeckWithCards(deck: Deck, cards: List<Card>) {
        val deckId = database.deckDao().insertDeck(deck)
        if (cards.isNotEmpty()) database.deckDao().insertCards(cards.map { it.copy(id = 0, deckId = deckId) })
    }

    override suspend fun deleteDeck(deckId: Long) = database.deckDao().deleteDeck(deckId)
    override suspend fun insertPrompt(prompt: UserPrompt) { database.userPromptDao().insert(prompt) }
    override suspend fun deletePrompt(promptId: Long) = database.userPromptDao().deleteById(promptId)
}

@OptIn(ExperimentalSerializationApi::class)
class BackupService(
    private val store: BackupDataStore,
    private val importer: DeckImporter = DeckImporter(),
    private val clock: Clock = Clock.systemUTC(),
    private val json: Json = Json {
        prettyPrint = true
        explicitNulls = false
        ignoreUnknownKeys = true
        allowTrailingComma = true
    },
) {
    suspend fun counts(): BackupCounts {
        val decks = store.getDecks()
        return BackupCounts(decks.size, decks.sumOf { store.getCards(it.id).size }, store.getPrompts().size)
    }

    suspend fun createExport(scope: BackupScope, includeProgress: Boolean): BackupExport {
        val decks = if (scope == BackupScope.PROMPTS) emptyList() else store.getDecks()
        val prompts = if (scope == BackupScope.DECKS) emptyList() else store.getPrompts()
        val backupDecks = decks.map { deck ->
            BackupDeckJson(
                deckName = deck.name,
                language = deck.language,
                category = deck.category,
                cards = store.getCards(deck.id).map { card ->
                    BackupCardJson(
                        term = card.term,
                        type = card.type,
                        termTranslation = card.termTranslation,
                        example = card.example,
                        exampleTranslation = card.exampleTranslation,
                        ipa = card.ipa,
                        note = card.note,
                        format = card.format.takeIf { it == "cloze" },
                        progress = if (includeProgress) {
                            BackupProgressJson(card.easeFactor, card.intervalDays, card.repetitions, card.dueAt)
                        } else null,
                    )
                },
            )
        }
        val backup = BackupFileJson(
            exportedAt = Instant.now(clock).toString(),
            decks = backupDecks,
            prompts = prompts.map { BackupPromptJson(it.title, it.description, it.body, it.generatesDeck) },
        )
        val prefix = when (scope) {
            BackupScope.ALL -> "flashlang-backup"
            BackupScope.DECKS -> "flashlang-baralhos"
            BackupScope.PROMPTS -> "flashlang-prompts"
        }
        val date = LocalDate.now(clock).format(DateTimeFormatter.ISO_LOCAL_DATE)
        return BackupExport(json.encodeToString(backup), "$prefix-$date.json")
    }

    suspend fun prepareRestore(input: String): BackupRestorePreview {
        val parsed = parseInput(input)
        val existingDecks = store.getDecks()
        val existingPrompts = store.getPrompts()
        val deckConflicts = parsed.first.count { incoming ->
            existingDecks.any { it.name.equals(incoming.deck.deckName, ignoreCase = true) }
        }
        val promptConflicts = parsed.second.count { incoming ->
            existingPrompts.any { it.title.equals(incoming.title, ignoreCase = true) }
        }
        return BackupRestorePreview(parsed.first, parsed.second, deckConflicts, promptConflicts)
    }

    suspend fun restore(preview: BackupRestorePreview, choice: RestoreConflictChoice) {
        try {
            store.transaction {
            for (plan in preview.decks) {
                val imported = plan.deck
                val currentDecks = getDecks()
                val conflict = currentDecks.firstOrNull { it.name.equals(imported.deckName, ignoreCase = true) }
                if (conflict != null && choice == RestoreConflictChoice.SKIP) continue
                var finalName = imported.deckName
                if (conflict != null && choice == RestoreConflictChoice.KEEP_BOTH) {
                    finalName = uniqueName(imported.deckName) { candidate ->
                        currentDecks.any { it.name.equals(candidate, ignoreCase = true) }
                    }
                } else if (conflict != null && choice == RestoreConflictChoice.REPLACE) {
                    deleteDeck(conflict.id)
                }
                val now = System.currentTimeMillis()
                val cards = imported.cards.mapIndexed { index, importedCard ->
                    val card = importedCard.toCard(deckId = 0, createdAt = now)
                    val progress = plan.progress.getOrNull(index)
                    if (progress == null) card else card.copy(
                        easeFactor = progress.easeFactor,
                        intervalDays = progress.intervalDays,
                        repetitions = progress.repetitions,
                        dueAt = progress.dueAt,
                    )
                }
                insertDeckWithCards(
                    Deck(name = finalName, language = imported.language, category = imported.normalizedCategory(), createdAt = now),
                    cards,
                )
            }

            for (prompt in preview.prompts) {
                val currentPrompts = getPrompts()
                val conflict = currentPrompts.firstOrNull { it.title.equals(prompt.title, ignoreCase = true) }
                if (conflict != null && choice == RestoreConflictChoice.SKIP) continue
                var finalTitle = prompt.title
                if (conflict != null && choice == RestoreConflictChoice.KEEP_BOTH) {
                    finalTitle = uniqueName(prompt.title) { candidate ->
                        currentPrompts.any { it.title.equals(candidate, ignoreCase = true) }
                    }
                } else if (conflict != null && choice == RestoreConflictChoice.REPLACE) {
                    deletePrompt(conflict.id)
                }
                val now = System.currentTimeMillis()
                insertPrompt(
                    UserPrompt(
                        title = finalTitle,
                        description = prompt.description,
                        body = prompt.body,
                        generatesDeck = prompt.generatesDeck,
                        createdAt = now,
                        updatedAt = now,
                    ),
                )
            }
            }
        } catch (exception: BackupException) {
            throw exception
        } catch (exception: Exception) {
            throw BackupException(
                "Não foi possível aplicar o backup; a restauração foi desfeita. Motivo: ${exception.message ?: "erro no armazenamento"}.",
            )
        }
    }

    private fun parseInput(input: String): Pair<List<RestoreDeckPlan>, List<BackupPromptJson>> {
        val root = try {
            json.parseToJsonElement(input.trim()) as? JsonObject
                ?: throw BackupException("O arquivo precisa conter um objeto JSON.")
        } catch (exception: BackupException) {
            throw exception
        } catch (_: Exception) {
            throw BackupException("Arquivo de backup inválido. Confira se o JSON está completo.")
        }

        val hasMetadata = root.containsKey("app") || root.containsKey("backupVersion") || root.containsKey("exportedAt")
        if (hasMetadata || root.containsKey("decks") || root.containsKey("prompts")) {
            val backup = try {
                json.decodeFromJsonElement<BackupFileJson>(root)
            } catch (_: Exception) {
                throw BackupException("Formato de backup inválido: verifique baralhos e prompts.")
            }
            if (hasMetadata && backup.app != "FlashLang") throw BackupException("Este arquivo não é um backup do FlashLang.")
            if (hasMetadata && backup.backupVersion != 1) throw BackupException("Versão de backup não compatível: ${backup.backupVersion}.")
            val decks = backup.decks.map { backupDeck ->
                val importDeck = ImportDeckJson(
                    deckName = backupDeck.deckName,
                    language = backupDeck.language,
                    category = backupDeck.category,
                    cards = backupDeck.cards.map { card ->
                        ImportCardJson(
                            term = card.term,
                            type = card.type,
                            termTranslation = card.termTranslation,
                            example = card.example,
                            exampleTranslation = card.exampleTranslation,
                            ipa = card.ipa,
                            note = card.note,
                            format = card.format,
                        )
                    },
                )
                val deckResult = importer.validateAndNormalize(importDeck)
                val imported = (deckResult as? DeckImportResult.Success)?.deck
                    ?: throw BackupException((deckResult as? DeckImportResult.Error)?.message ?: "Baralho inválido no backup.")
                RestoreDeckPlan(imported, backupDeck.cards.map { it.progress })
            }
            backup.prompts.forEachIndexed { index, prompt ->
                if (prompt.title.isBlank()) throw BackupException("O título está vazio no prompt ${index + 1}.")
                if (prompt.body.isBlank()) throw BackupException("O corpo está vazio no prompt ${index + 1}.")
            }
            return decks to backup.prompts
        }

        if (root.containsKey("deckName") && root.containsKey("language") && root.containsKey("cards")) {
            return when (val result = importer.parse(input)) {
                is DeckImportResult.Success -> listOf(RestoreDeckPlan(result.deck, List(result.deck.cards.size) { null })) to emptyList()
                is DeckImportResult.Error -> throw BackupException(result.message)
            }
        }
        throw BackupException("O arquivo não contém um backup, baralho único ou backup de prompts reconhecido.")
    }

    private fun uniqueName(base: String, exists: (String) -> Boolean): String {
        var suffix = 2
        var candidate = "$base ($suffix)"
        while (exists(candidate)) {
            suffix++
            candidate = "$base ($suffix)"
        }
        return candidate
    }
}

class BackupException(message: String) : Exception(message)
