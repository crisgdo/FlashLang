package com.cristiano.flashlang.ui.manage

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.cristiano.flashlang.data.repository.DeckRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class CardEditorState(
    val term: String = "",
    val type: String = "word",
    val termTranslation: String = "",
    val example: String = "",
    val exampleTranslation: String = "",
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val errorMessage: String? = null,
    val saved: Boolean = false,
    val deleted: Boolean = false,
)

class CardEditorViewModel(
    private val repository: DeckRepository,
    private val deckId: Long,
    private val cardId: Long?,
) : ViewModel() {
    private val _state = MutableStateFlow(CardEditorState(isLoading = cardId != null))
    val state: StateFlow<CardEditorState> = _state.asStateFlow()

    init {
        if (cardId != null) {
            viewModelScope.launch {
                val card = repository.getCard(cardId)
                if (card == null) _state.update { it.copy(isLoading = false, errorMessage = "Card não encontrado.") }
                else _state.value = CardEditorState(
                    term = card.term,
                    type = card.type,
                    termTranslation = card.termTranslation,
                    example = card.example,
                    exampleTranslation = card.exampleTranslation,
                )
            }
        }
    }

    fun updateTerm(value: String) = _state.update { it.copy(term = value, errorMessage = null) }
    fun updateType(value: String) { if (value in setOf("word", "expression")) _state.update { it.copy(type = value) } }
    fun updateTermTranslation(value: String) = _state.update { it.copy(termTranslation = value, errorMessage = null) }
    fun updateExample(value: String) = _state.update { it.copy(example = value, errorMessage = null) }
    fun updateExampleTranslation(value: String) = _state.update { it.copy(exampleTranslation = value, errorMessage = null) }

    fun save() {
        val current = _state.value
        val fields = listOf(current.term, current.type, current.termTranslation, current.example, current.exampleTranslation)
        if (fields.any(String::isBlank)) {
            _state.update { it.copy(errorMessage = "Preencha todos os campos antes de salvar.") }
            return
        }
        _state.update { it.copy(isSaving = true, errorMessage = null) }
        viewModelScope.launch {
            try {
                val existingId = cardId
                if (existingId == null) {
                    repository.addCard(deckId, current.term.trim(), current.type, current.termTranslation.trim(), current.example.trim(), current.exampleTranslation.trim())
                } else {
                    repository.updateCardContent(existingId, current.term.trim(), current.type, current.termTranslation.trim(), current.example.trim(), current.exampleTranslation.trim())
                }
                _state.update { it.copy(isSaving = false, saved = true) }
            } catch (_: Exception) {
                _state.update { it.copy(isSaving = false, errorMessage = "Não foi possível salvar o card. Tente novamente.") }
            }
        }
    }

    fun delete() {
        val existingId = cardId ?: return
        _state.update { it.copy(isSaving = true, errorMessage = null) }
        viewModelScope.launch {
            try {
                repository.deleteCard(existingId)
                _state.update { it.copy(isSaving = false, deleted = true) }
            } catch (_: Exception) {
                _state.update { it.copy(isSaving = false, errorMessage = "Não foi possível excluir o card.") }
            }
        }
    }

    class Factory(private val repository: DeckRepository, private val deckId: Long, private val cardId: Long?) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            require(modelClass.isAssignableFrom(CardEditorViewModel::class.java))
            return CardEditorViewModel(repository, deckId, cardId) as T
        }
    }
}
