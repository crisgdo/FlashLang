package com.cristiano.flashlang.ui.backup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.cristiano.flashlang.data.backup.BackupCounts
import com.cristiano.flashlang.data.backup.BackupExport
import com.cristiano.flashlang.data.backup.BackupRestorePreview
import com.cristiano.flashlang.data.backup.BackupScope
import com.cristiano.flashlang.data.backup.BackupService
import com.cristiano.flashlang.data.backup.RestoreConflictChoice
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class BackupUiState(
    val counts: BackupCounts = BackupCounts(0, 0, 0),
    val isLoading: Boolean = true,
    val isRestoring: Boolean = false,
    val export: BackupExport? = null,
    val restorePreview: BackupRestorePreview? = null,
    val message: String? = null,
    val errorMessage: String? = null,
)

class BackupViewModel(private val backupService: BackupService) : ViewModel() {
    private val _state = MutableStateFlow(BackupUiState())
    val state: StateFlow<BackupUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            runCatching { backupService.counts() }
                .onSuccess { counts -> _state.update { it.copy(counts = counts, isLoading = false) } }
                .onFailure { exception ->
                    _state.update { it.copy(isLoading = false, errorMessage = exception.message ?: "Não foi possível ler os dados.") }
                }
        }
    }

    fun export(scope: BackupScope, includeProgress: Boolean) {
        _state.update { it.copy(errorMessage = null, message = null) }
        viewModelScope.launch {
            runCatching { backupService.createExport(scope, includeProgress) }
                .onSuccess { file -> _state.update { it.copy(export = file) } }
                .onFailure { exception -> _state.update { it.copy(errorMessage = exception.message ?: "Não foi possível criar o backup.") } }
        }
    }

    fun prepareRestore(content: String) {
        _state.update { it.copy(isRestoring = true, errorMessage = null, message = null, restorePreview = null) }
        viewModelScope.launch {
            runCatching { backupService.prepareRestore(content) }
                .onSuccess { preview -> _state.update { it.copy(isRestoring = false, restorePreview = preview) } }
                .onFailure { exception ->
                    _state.update { it.copy(isRestoring = false, errorMessage = exception.message ?: "Não foi possível ler o arquivo de backup.") }
                }
        }
    }

    fun restore(choice: RestoreConflictChoice) {
        val preview = _state.value.restorePreview ?: return
        _state.update { it.copy(isRestoring = true, errorMessage = null) }
        viewModelScope.launch {
            try {
                backupService.restore(preview, choice)
                val counts = backupService.counts()
                _state.update {
                    it.copy(counts = counts, isRestoring = false, restorePreview = null, message = "Restauração concluída.")
                }
            } catch (exception: Exception) {
                _state.update { it.copy(isRestoring = false, errorMessage = exception.message ?: "Não foi possível restaurar o backup.") }
            }
        }
    }

    fun dismissExport() { _state.update { it.copy(export = null) } }
    fun dismissRestorePreview() { _state.update { it.copy(restorePreview = null) } }
    fun dismissMessages() { _state.update { it.copy(message = null, errorMessage = null) } }

    class Factory(private val service: BackupService) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            require(modelClass.isAssignableFrom(BackupViewModel::class.java))
            return BackupViewModel(service) as T
        }
    }
}
