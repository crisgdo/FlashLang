package com.cristiano.flashlang.ui.manage

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card as MaterialCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManageCardsScreen(
    viewModel: ManageCardsViewModel,
    onBack: () -> Unit,
    onStudy: () -> Unit,
    onAddCard: () -> Unit,
    onEditCard: (Long) -> Unit,
) {
    val deckName by viewModel.deckName.collectAsState()
    val cards by viewModel.cards.collectAsState()
    var search by remember { mutableStateOf("") }
    var showRenameDialog by remember { mutableStateOf(false) }
    var newName by remember(deckName) { mutableStateOf(deckName) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(deckName.ifBlank { "Gerenciar baralho" }) },
                navigationIcon = { TextButton(onClick = onBack) { Text("Voltar") } },
                actions = {
                    TextButton(onClick = { showRenameDialog = true }) { Text("Renomear") }
                    TextButton(onClick = onStudy) { Text("Estudar") }
                },
            )
        },
        floatingActionButton = { FloatingActionButton(onClick = onAddCard) { Text("+") } },
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            OutlinedTextField(
                value = search,
                onValueChange = { search = it; viewModel.updateSearch(it) },
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                label = { Text("Buscar termo ou tradução") },
                singleLine = true,
            )
            if (cards.isEmpty()) {
                Text("Nenhum card encontrado.", modifier = Modifier.align(Alignment.CenterHorizontally).padding(top = 24.dp))
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(cards, key = { it.id }) { card ->
                        MaterialCard(
                            modifier = Modifier.fillMaxWidth().clickable { onEditCard(card.id) },
                        ) {
                            Row(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text(card.term, style = MaterialTheme.typography.titleMedium)
                                    Text(card.termTranslation, style = MaterialTheme.typography.bodyMedium)
                                }
                                Text("Editar", style = MaterialTheme.typography.labelLarge)
                            }
                        }
                    }
                }
            }
        }
    }

    if (showRenameDialog) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showRenameDialog = false },
            title = { Text("Renomear baralho") },
            text = {
                OutlinedTextField(value = newName, onValueChange = { newName = it }, label = { Text("Nome") }, singleLine = true)
            },
            confirmButton = {
                TextButton(onClick = { viewModel.renameDeck(newName); showRenameDialog = false }) { Text("Salvar") }
            },
            dismissButton = { TextButton(onClick = { showRenameDialog = false }) { Text("Cancelar") } },
        )
    }
}
