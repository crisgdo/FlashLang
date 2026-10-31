package com.cristiano.flashlang.ui.importdeck

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.cristiano.flashlang.data.importer.DeckImportResult
import com.cristiano.flashlang.data.importer.DeckImporter
import com.cristiano.flashlang.data.model.Deck
import com.cristiano.flashlang.data.repository.DeckRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ImportUiState(
    val jsonText: String = "",
    val errorMessage: String? = null,
    val isSaving: Boolean = false,
    val imported: Boolean = false,
    val duplicateDeckId: Long? = null,
    val duplicateDeckName: String? = null,
    val pendingDeck: com.cristiano.flashlang.data.importer.ImportDeckJson? = null,
)

enum class DuplicateAction { REPLACE, KEEP_BOTH, CANCEL }

class ImportViewModel(
    private val repository: DeckRepository,
    private val importer: DeckImporter = DeckImporter(),
) : ViewModel() {
    private val _state = MutableStateFlow(ImportUiState())
    val state: StateFlow<ImportUiState> = _state.asStateFlow()

    fun onJsonChanged(value: String) {
        _state.update { it.copy(jsonText = value, errorMessage = null, duplicateDeckId = null, duplicateDeckName = null, pendingDeck = null) }
    }

    fun onFileReadError() {
        _state.update { it.copy(errorMessage = "Não foi possível ler o arquivo selecionado.") }
    }

    fun importDeck() {
        when (val result = importer.parse(_state.value.jsonText)) {
            is DeckImportResult.Error -> _state.update { it.copy(errorMessage = result.message) }
            is DeckImportResult.Success -> {
                _state.update { it.copy(errorMessage = null, isSaving = true) }
                viewModelScope.launch {
                    val existing = repository.findDeckByName(result.deck.deckName)
                    if (existing != null) {
                        _state.update {
                            it.copy(
                                isSaving = false,
                                duplicateDeckId = existing.id,
                                duplicateDeckName = existing.name,
                                pendingDeck = result.deck,
                            )
                        }
                    } else {
                        saveDeck(result.deck, name = result.deck.deckName)
                    }
                }
            }
        }
    }

    fun resolveDuplicate(action: DuplicateAction) {
        val state = _state.value
        val importedDeck = state.pendingDeck ?: return
        when (action) {
            DuplicateAction.CANCEL -> _state.update { it.copy(duplicateDeckId = null, duplicateDeckName = null, pendingDeck = null) }
            DuplicateAction.REPLACE -> {
                val deckId = state.duplicateDeckId ?: return
                _state.update { it.copy(isSaving = true, duplicateDeckId = null, duplicateDeckName = null, pendingDeck = null) }
                viewModelScope.launch { saveDeck(importedDeck, name = importedDeck.deckName, replaceId = deckId) }
            }
            DuplicateAction.KEEP_BOTH -> {
                _state.update { it.copy(isSaving = true, duplicateDeckId = null, duplicateDeckName = null, pendingDeck = null) }
                viewModelScope.launch {
                    var suffix = 2
                    var candidate = "${importedDeck.deckName} ($suffix)"
                    while (repository.findDeckByName(candidate) != null) {
                        suffix++
                        candidate = "${importedDeck.deckName} ($suffix)"
                    }
                    saveDeck(importedDeck, name = candidate)
                }
            }
        }
    }

    private suspend fun saveDeck(
        importedDeck: com.cristiano.flashlang.data.importer.ImportDeckJson,
        name: String,
        replaceId: Long? = null,
    ) {
        try {
            val deck = Deck(name = name.trim(), language = importedDeck.language)
            val cards = importedDeck.cards.map { it.toCard(deckId = replaceId ?: 0) }
            if (replaceId == null) repository.insertDeck(deck, cards)
            else repository.replaceDeck(replaceId, deck, cards)
            _state.update { it.copy(isSaving = false, imported = true) }
        } catch (_: Exception) {
            _state.update { it.copy(isSaving = false, errorMessage = "Não foi possível salvar o baralho. Tente novamente.") }
        }
    }

    class Factory(private val repository: DeckRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            require(modelClass.isAssignableFrom(ImportViewModel::class.java))
            return ImportViewModel(repository) as T
        }
    }
}
