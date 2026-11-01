package com.cristiano.flashlang.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.Share
import com.cristiano.flashlang.data.model.DeckListItem

@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onImportClick: () -> Unit,
    onPromptsClick: () -> Unit,
    onDeckClick: (Long) -> Unit,
    onManageClick: (Long) -> Unit,
    onExportClick: (Long) -> Unit,
) {
    val decks by viewModel.decks.collectAsState()
    var deckToDelete by remember { mutableStateOf<DeckListItem?>(null) }

    Scaffold(
        topBar = {
            Row(
                modifier = Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 20.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "Meus baralhos",
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.weight(1f),
                )
                IconButton(onClick = onPromptsClick) {
                    Icon(Icons.Outlined.AutoAwesome, contentDescription = "Prompts")
                }
            }
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
    var menuExpanded by remember(item.deck.id) { mutableStateOf(false) }
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().semantics {
            contentDescription = "Estudar ${item.deck.name}"
            role = Role.Button
        },
    ) {
        Box(Modifier.fillMaxWidth().padding(16.dp)) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.Start,
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = item.deck.name,
                    modifier = Modifier.fillMaxWidth().padding(end = 48.dp),
                    style = MaterialTheme.typography.titleMedium,
                    textAlign = TextAlign.Start,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    "${languageName(item.deck.language)} · ${item.cardCount} cards\n${item.pendingCount} para revisar hoje",
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Start,
                )
            }

            Box(Modifier.align(Alignment.TopEnd)) {
                IconButton(onClick = { menuExpanded = true }) {
                    Icon(Icons.Outlined.MoreVert, contentDescription = "Mais opções")
                }
                DropdownMenu(
                    expanded = menuExpanded,
                    onDismissRequest = { menuExpanded = false },
                ) {
                    DropdownMenuItem(
                        text = { Text("Gerenciar") },
                        leadingIcon = { Icon(Icons.Outlined.Edit, contentDescription = null) },
                        onClick = { menuExpanded = false; onManage() },
                    )
                    DropdownMenuItem(
                        text = { Text("Exportar") },
                        leadingIcon = { Icon(Icons.Outlined.Share, contentDescription = null) },
                        onClick = { menuExpanded = false; onExport() },
                    )
                    HorizontalDivider()
                    DropdownMenuItem(
                        text = { Text("Excluir", color = MaterialTheme.colorScheme.error) },
                        leadingIcon = {
                            Icon(
                                Icons.Outlined.Delete,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error,
                            )
                        },
                        onClick = { menuExpanded = false; onDelete() },
                    )
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
