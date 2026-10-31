package com.cristiano.flashlang.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.cristiano.flashlang.data.model.DeckListItem

@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onImportClick: () -> Unit,
    onDeckClick: (Long) -> Unit,
    onExportClick: (Long) -> Unit,
) {
    val decks by viewModel.decks.collectAsState()
    var deckToDelete by remember { mutableStateOf<DeckListItem?>(null) }

    Scaffold(
        topBar = { Text("Meus baralhos", style = MaterialTheme.typography.headlineSmall, modifier = Modifier.padding(20.dp)) },
        floatingActionButton = {
            FloatingActionButton(onClick = onImportClick) { Text("+") }
        },
    ) { padding ->
        if (decks.isEmpty()) {
            Column(
                modifier = Modifier.fillMaxSize().padding(padding).padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Text("Nenhum baralho ainda. Importe o primeiro!", style = MaterialTheme.typography.bodyLarge)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(decks, key = { it.deck.id }) { item ->
                    DeckRow(
                        item = item,
                        onDelete = { deckToDelete = item },
                        onClick = { onDeckClick(item.deck.id) },
                        onExport = { onExportClick(item.deck.id) },
                        onManage = { onDeckClick(item.deck.id) },
                    )
                }
            }
        }
    }

    deckToDelete?.let { item ->
        AlertDialog(
            onDismissRequest = { deckToDelete = null },
            title = { Text("Excluir baralho?") },
            text = { Text("O baralho ${item.deck.name} e seus cards serão excluídos.") },
            confirmButton = {
                TextButton(onClick = { viewModel.deleteDeck(item.deck.id); deckToDelete = null }) { Text("Excluir") }
            },
            dismissButton = { TextButton(onClick = { deckToDelete = null }) { Text("Cancelar") } },
        )
    }
}

@Composable
private fun DeckRow(item: DeckListItem, onDelete: () -> Unit, onClick: () -> Unit, onExport: () -> Unit, onManage: () -> Unit) {
    Card(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(start = 16.dp, top = 12.dp, bottom = 12.dp, end = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(item.deck.name, style = MaterialTheme.typography.titleMedium)
                Text("${languageName(item.deck.language)} · ${item.cardCount} cards", style = MaterialTheme.typography.bodyMedium)
                Text("${item.pendingCount} para revisar hoje", style = MaterialTheme.typography.bodySmall)
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = onManage) { Text("Gerenciar") }
                TextButton(onClick = onExport) { Text("Exportar") }
                IconButton(onClick = onDelete) {
                    Text("🗑", modifier = Modifier.semantics { contentDescription = "Excluir ${item.deck.name}" })
                }
            }
        }
    }
}

private fun languageName(language: String): String = when (language) {
    "en" -> "Inglês"
    "es" -> "Espanhol"
    else -> language
}
