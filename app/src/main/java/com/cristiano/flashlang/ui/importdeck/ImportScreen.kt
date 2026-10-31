package com.cristiano.flashlang.ui.importdeck

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun ImportScreen(viewModel: ImportViewModel, onImportSuccess: () -> Unit, onBack: () -> Unit) {
    val state by viewModel.state.collectAsState()
    LaunchedEffect(state.imported) {
        if (state.imported) onImportSuccess()
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Importar baralho") }, navigationIcon = {
            androidx.compose.material3.TextButton(onClick = onBack) { Text("Voltar") }
        }) },
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text("Cole abaixo o JSON do baralho.", style = MaterialTheme.typography.bodyLarge)
            OutlinedTextField(
                value = state.jsonText,
                onValueChange = viewModel::onJsonChanged,
                modifier = Modifier.fillMaxWidth(),
                minLines = 14,
                label = { Text("JSON do baralho") },
                placeholder = { Text("{\n  \"deckName\": ...\n}") },
                isError = state.errorMessage != null,
            )
            state.errorMessage?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            Button(
                onClick = viewModel::importDeck,
                enabled = !state.isSaving,
                modifier = Modifier.fillMaxWidth(),
            ) {
                if (state.isSaving) CircularProgressIndicator()
                else Text("Importar")
            }
        }
    }
}
