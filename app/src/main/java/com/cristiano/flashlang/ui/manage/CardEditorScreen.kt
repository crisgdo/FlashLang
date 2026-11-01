package com.cristiano.flashlang.ui.manage

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Save

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun CardEditorScreen(
    viewModel: CardEditorViewModel,
    isEditing: Boolean,
    onBack: () -> Unit,
    onSaved: () -> Unit,
) {
    val state by viewModel.state.collectAsState()
    var showDeleteConfirmation by remember { mutableStateOf(false) }
    LaunchedEffect(state.saved, state.deleted) {
        if (state.saved || state.deleted) onSaved()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (isEditing) "Editar card" else "Adicionar card") },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Voltar") }
                },
            )
        },
    ) { padding ->
        if (state.isLoading) {
            androidx.compose.foundation.layout.Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            Column(
                modifier = Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                OutlinedTextField(state.term, viewModel::updateTerm, modifier = Modifier.fillMaxWidth(), label = { Text("Termo") }, singleLine = true)
                Text("Tipo", style = MaterialTheme.typography.titleSmall)
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(selected = state.type == "word", onClick = { viewModel.updateType("word") })
                        Text("Palavra", maxLines = 1, softWrap = false)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(selected = state.type == "expression", onClick = { viewModel.updateType("expression") })
                        Text("Expressão", maxLines = 1, softWrap = false)
                    }
                }
                OutlinedTextField(state.termTranslation, viewModel::updateTermTranslation, modifier = Modifier.fillMaxWidth(), label = { Text("Tradução do termo") })
                OutlinedTextField(state.example, viewModel::updateExample, modifier = Modifier.fillMaxWidth(), label = { Text("Frase de exemplo") })
                OutlinedTextField(state.exampleTranslation, viewModel::updateExampleTranslation, modifier = Modifier.fillMaxWidth(), label = { Text("Tradução da frase") })
                OutlinedTextField(
                    state.ipa,
                    viewModel::updateIpa,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Pronúncia (IPA)") },
                    singleLine = true,
                )
                OutlinedTextField(
                    state.note,
                    viewModel::updateNote,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Regra ou nota") },
                    minLines = 3,
                )
                state.errorMessage?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                Button(onClick = viewModel::save, enabled = !state.isSaving, modifier = Modifier.fillMaxWidth()) {
                    if (state.isSaving) CircularProgressIndicator() else {
                        Icon(Icons.Outlined.Save, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Salvar")
                    }
                }
                if (isEditing) {
                    TextButton(onClick = { showDeleteConfirmation = true }, enabled = !state.isSaving, modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Outlined.Delete, contentDescription = null)
                        Text("Excluir card", color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }
    }

    if (showDeleteConfirmation) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmation = false },
            title = { Text("Excluir card?") },
            text = { Text("Esta ação não pode ser desfeita.") },
            confirmButton = {
                TextButton(onClick = { showDeleteConfirmation = false; viewModel.delete() }) { Text("Excluir") }
            },
            dismissButton = { TextButton(onClick = { showDeleteConfirmation = false }) { Text("Cancelar") } },
        )
    }
}
