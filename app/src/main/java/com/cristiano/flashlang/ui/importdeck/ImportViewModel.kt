package com.cristiano.flashlang.ui.importdeck

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.cristiano.flashlang.data.importer.DeckImportResult
import com.cristiano.flashlang.data.importer.DeckImporter
import com.cristiano.flashlang.data.model.Card
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
)

class ImportViewModel(
    private val repository: DeckRepository,
    private val importer: DeckImporter = DeckImporter(),
) : ViewModel() {
    private val _state = MutableStateFlow(ImportUiState())
    val state: StateFlow<ImportUiState> = _state.asStateFlow()

    fun onJsonChanged(value: String) {
        _state.update { it.copy(jsonText = value, errorMessage = null) }
    }

    fun importDeck() {
        when (val result = importer.parse(_state.value.jsonText)) {
            is DeckImportResult.Error -> _state.update { it.copy(errorMessage = result.message) }
            is DeckImportResult.Success -> {
                val importedDeck = result.deck
                _state.update { it.copy(errorMessage = null, isSaving = true) }
                viewModelScope.launch {
                    try {
                        repository.insertDeck(
                            deck = Deck(name = importedDeck.deckName.trim(), language = importedDeck.language),
                            cards = importedDeck.cards.map { card ->
                                Card(
                                    deckId = 0,
                                    term = card.term.trim(),
                                    type = card.type,
                                    termTranslation = card.termTranslation.trim(),
                                    example = card.example.trim(),
                                    exampleTranslation = card.exampleTranslation.trim(),
                                )
                            },
                        )
                        _state.update { it.copy(isSaving = false, imported = true) }
                    } catch (_: Exception) {
                        _state.update { it.copy(isSaving = false, errorMessage = "Não foi possível salvar o baralho. Tente novamente.") }
                    }
                }
            }
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
