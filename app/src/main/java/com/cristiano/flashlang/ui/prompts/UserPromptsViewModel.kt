package com.cristiano.flashlang.ui.prompts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.cristiano.flashlang.data.model.UserPrompt
import com.cristiano.flashlang.data.prompts.PromptTemplate
import com.cristiano.flashlang.data.repository.UserPromptRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class UserPromptsViewModel(private val repository: UserPromptRepository) : ViewModel() {
    val prompts: StateFlow<List<UserPrompt>> = repository.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun duplicate(prompt: UserPrompt) {
        viewModelScope.launch { repository.duplicate(prompt) }
    }

    fun duplicate(template: PromptTemplate) {
        viewModelScope.launch {
            repository.duplicateTemplate(
                title = template.title,
                description = template.description,
                body = template.body,
                generatesDeck = template.generatesDeck,
            )
        }
    }

    fun delete(id: Long) {
        viewModelScope.launch { repository.delete(id) }
    }

    class Factory(private val repository: UserPromptRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            require(modelClass.isAssignableFrom(UserPromptsViewModel::class.java))
            return UserPromptsViewModel(repository) as T
        }
    }
}
