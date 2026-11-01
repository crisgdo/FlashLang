package com.cristiano.flashlang.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Share
import com.cristiano.flashlang.data.model.DeckListItem

@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onImportClick: () -> Unit,
    onDeckClick: (Long) -> Unit,
    onManageClick: (Long) -> Unit,
    onExportClick: (Long) -> Unit,
) {
    val decks by viewModel.decks.collectAsState()
    var deckToDelete by remember { mutableStateOf<DeckListItem?>(null) }

    Scaffold(
        topBar = {
            Text(
                "Meus baralhos",
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.statusBarsPadding().padding(horizontal = 20.dp, vertical = 16.dp),
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onImportClick) {
                Icon(Icons.Outlined.Add, contentDescription = "Importar baralho")
            }
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
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(decks, key = { it.deck.id }) { item ->
                    DeckRow(
                        item = item,
                        onDelete = { deckToDelete = item },
                        onClick = { onDeckClick(item.deck.id) },
                        onExport = { onExportClick(item.deck.id) },
                        onManage = { onManageClick(item.deck.id) },
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
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().semantics {
            contentDescription = "Estudar ${item.deck.name}"
            role = Role.Button
        },
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = item.deck.name,
                modifier = Modifier.fillMaxWidth(),
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(8.dp))
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    "${languageName(item.deck.language)} · ${item.cardCount} cards",
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                )
                Text(
                    "${item.pendingCount} para revisar hoje",
                    style = MaterialTheme.typography.bodySmall,
                    textAlign = TextAlign.Center,
                )
            }
            Spacer(Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                FilledTonalButton(
                    onClick = onManage,
                    modifier = Modifier.weight(1f),
                    shape = CircleShape,
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
                ) {
                    Icon(Icons.Outlined.Edit, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Gerenciar", maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                FilledTonalButton(
                    onClick = onExport,
                    modifier = Modifier.weight(1f),
                    shape = CircleShape,
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
                ) {
                    Icon(Icons.Outlined.Share, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Exportar", maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
            Spacer(Modifier.height(4.dp))
            TextButton(onClick = onDelete) {
                Icon(
                    Icons.Outlined.Delete,
                    contentDescription = "Excluir baralho",
                    tint = MaterialTheme.colorScheme.error,
                )
                Spacer(Modifier.width(8.dp))
                Text("Excluir", color = MaterialTheme.colorScheme.error)
            }
        }
    }
}

private fun languageName(language: String): String = when (language) {
    "en" -> "Inglês"
    "es" -> "Espanhol"
    else -> language
}
