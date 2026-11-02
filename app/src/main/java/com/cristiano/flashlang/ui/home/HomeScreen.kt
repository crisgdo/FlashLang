package com.cristiano.flashlang.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.platform.LocalContext
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
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.Category
import androidx.compose.material.icons.outlined.DriveFileRenameOutline
import androidx.compose.material.icons.outlined.Backup
import com.cristiano.flashlang.data.model.DeckListItem
import java.util.Locale

@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onImportClick: () -> Unit,
    onPromptsClick: () -> Unit,
    onBackupClick: () -> Unit,
    onDeckClick: (Long) -> Unit,
    onManageClick: (Long) -> Unit,
    onExportClick: (Long) -> Unit,
) {
    val decks by viewModel.decks.collectAsStateWithLifecycle()
    val categories by viewModel.categories.collectAsStateWithLifecycle()
    var deckIdToDelete by rememberSaveable { mutableStateOf<Long?>(null) }
    var deckIdToCategorize by rememberSaveable { mutableStateOf<Long?>(null) }
    var showCategoryManager by rememberSaveable { mutableStateOf(false) }
    var categoryToRename by rememberSaveable { mutableStateOf<String?>(null) }
    var categoryToDelete by rememberSaveable { mutableStateOf<String?>(null) }
    var topMenuExpanded by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val filterPreferences = remember(context) {
        context.getSharedPreferences("flashlang_home_preferences", android.content.Context.MODE_PRIVATE)
    }
    var selectedFilter by rememberSaveable {
        mutableStateOf(filterPreferences.getString("deck_category_filter", "__all__") ?: "__all__")
    }
    val categoryNames = categories.mapNotNull { it.category?.trim()?.takeIf(String::isNotEmpty) }
        .distinctBy { it.lowercase(Locale.ROOT) }
        .sortedBy { it.lowercase(Locale.ROOT) }
    val hasUncategorized = categories.any { it.category.isNullOrBlank() }
    val selectedCategory = selectedFilter.removePrefix("category:")
    val filterExists = selectedFilter == "__all__" ||
        (selectedFilter == "__none__" && hasUncategorized) ||
        categoryNames.any { it.equals(selectedCategory, ignoreCase = true) }
    androidx.compose.runtime.LaunchedEffect(filterExists, selectedFilter) {
        if (decks.isNotEmpty() && !filterExists) {
            selectedFilter = "__all__"
            filterPreferences.edit().putString("deck_category_filter", "__all__").apply()
        }
    }
    val filteredDecks = when (selectedFilter) {
        "__all__" -> decks
        "__none__" -> decks.filter { it.deck.category.isNullOrBlank() }
        else -> decks.filter { it.deck.category?.trim()?.equals(selectedCategory, ignoreCase = true) == true }
    }
    val sections = decks.groupBy {
        it.deck.category?.trim()?.takeIf(String::isNotEmpty)?.lowercase(Locale.ROOT) ?: "__none__"
    }.map { (key, items) ->
        val name = if (key == "__none__") "Sem categoria" else items.first().deck.category!!.trim()
        DeckCategorySection(name, items.sortedBy { it.deck.name.lowercase(Locale.ROOT) })
    }.sortedWith(compareBy<DeckCategorySection> { it.category == "Sem categoria" }.thenBy { it.category.lowercase(Locale.ROOT) })

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
                Box {
                    IconButton(onClick = { topMenuExpanded = true }) {
                        Icon(Icons.Outlined.MoreVert, contentDescription = "Mais opções")
                    }
                    DropdownMenu(expanded = topMenuExpanded, onDismissRequest = { topMenuExpanded = false }) {
                        DropdownMenuItem(
                            text = { Text("Backup") },
                            leadingIcon = { Icon(Icons.Outlined.Backup, contentDescription = null) },
                            onClick = { topMenuExpanded = false; onBackupClick() },
                        )
                        DropdownMenuItem(
                            text = { Text("Gerenciar categorias") },
                            leadingIcon = { Icon(Icons.Outlined.Category, contentDescription = null) },
                            onClick = { topMenuExpanded = false; showCategoryManager = true },
                        )
                    }
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
            Column(modifier = Modifier.fillMaxSize().padding(padding)) {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    item {
                        FilterChip(
                            selected = selectedFilter == "__all__",
                            onClick = { selectedFilter = "__all__"; filterPreferences.edit().putString("deck_category_filter", "__all__").apply() },
                            label = { Text("Todas") },
                        )
                    }
                    items(categoryNames, key = { "category-${it.lowercase(Locale.ROOT)}" }) { category ->
                        val key = "category:${category.lowercase(Locale.ROOT)}"
                        FilterChip(
                            selected = selectedFilter == key,
                            onClick = { selectedFilter = key; filterPreferences.edit().putString("deck_category_filter", key).apply() },
                            label = { Text(category) },
                        )
                    }
                    if (hasUncategorized) {
                        item {
                            FilterChip(
                                selected = selectedFilter == "__none__",
                                onClick = { selectedFilter = "__none__"; filterPreferences.edit().putString("deck_category_filter", "__none__").apply() },
                                label = { Text("Sem categoria") },
                            )
                        }
                    }
                }
                if (filteredDecks.isEmpty()) {
                    Text("Nenhum baralho nesta categoria.", modifier = Modifier.fillMaxWidth().padding(24.dp), textAlign = TextAlign.Center)
                } else {
                    LazyColumn(
                        modifier = Modifier.weight(1f).fillMaxWidth(),
                        contentPadding = PaddingValues(start = 16.dp, top = 8.dp, end = 16.dp, bottom = 96.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        if (selectedFilter == "__all__") {
                            sections.forEach { section ->
                                item(key = "section-${section.category.lowercase(Locale.ROOT)}") {
                                    val pendingCount = section.decks.sumOf { it.pendingCount }
                                    Column(modifier = Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 2.dp)) {
                                        Text(section.category, style = MaterialTheme.typography.titleMedium)
                                        Text(
                                            "${section.decks.size} baralhos · $pendingCount para revisar hoje",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    }
                                }
                                items(section.decks, key = { "deck-${it.deck.id}" }) { item ->
                                    HomeDeckRow(
                                        item,
                                        onDelete = { deckIdToDelete = item.deck.id },
                                        onClick = { onDeckClick(item.deck.id) },
                                        onExport = { onExportClick(item.deck.id) },
                                        onManage = { onManageClick(item.deck.id) },
                                        onCategory = { deckIdToCategorize = item.deck.id },
                                    )
                                }
                            }
                        } else {
                            items(filteredDecks.sortedBy { it.deck.name.lowercase(Locale.ROOT) }, key = { "deck-${it.deck.id}" }) { item ->
                                HomeDeckRow(
                                    item,
                                    onDelete = { deckIdToDelete = item.deck.id },
                                    onClick = { onDeckClick(item.deck.id) },
                                    onExport = { onExportClick(item.deck.id) },
                                    onManage = { onManageClick(item.deck.id) },
                                    onCategory = { deckIdToCategorize = item.deck.id },
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    deckIdToDelete?.let { deckId ->
        val item = decks.firstOrNull { it.deck.id == deckId }
        if (item != null) {
            AlertDialog(
                onDismissRequest = { deckIdToDelete = null },
                title = { Text("Excluir baralho?") },
                text = { Text("O baralho ${item.deck.name} e seus cards serão excluídos.") },
                confirmButton = {
                    TextButton(onClick = { viewModel.deleteDeck(item.deck.id); deckIdToDelete = null }) { Text("Excluir") }
                },
                dismissButton = { TextButton(onClick = { deckIdToDelete = null }) { Text("Cancelar") } },
            )
        }
    }

    deckIdToCategorize?.let { deckId ->
        val item = decks.firstOrNull { it.deck.id == deckId }
        if (item != null) {
            DeckCategoryDialog(
                deckId = item.deck.id,
                currentCategory = item.deck.category,
                categories = categoryNames,
                onDismiss = { deckIdToCategorize = null },
                onSave = { category -> viewModel.setDeckCategory(item.deck.id, category); deckIdToCategorize = null },
            )
        }
    }

    if (showCategoryManager) {
        CategoryManagerDialog(
            categories = categories.filter { !it.category.isNullOrBlank() }.sortedBy { it.category!!.lowercase(Locale.ROOT) },
            onDismiss = { showCategoryManager = false },
            onRename = { category -> categoryToRename = category },
            onDelete = { category -> categoryToDelete = category },
        )
    }

    categoryToRename?.let { category ->
        RenameCategoryDialog(
            currentName = category,
            onDismiss = { categoryToRename = null },
            onSave = { newName -> viewModel.renameCategory(category, newName); categoryToRename = null },
        )
    }

    categoryToDelete?.let { category ->
        AlertDialog(
            onDismissRequest = { categoryToDelete = null },
            title = { Text("Excluir categoria?") },
            text = { Text("Os baralhos da categoria \"$category\" não serão apagados; ficarão sem categoria.") },
            confirmButton = { TextButton(onClick = { viewModel.removeCategory(category); categoryToDelete = null }) { Text("Excluir") } },
            dismissButton = { TextButton(onClick = { categoryToDelete = null }) { Text("Cancelar") } },
        )
    }
}

@Composable
private fun HomeDeckRow(
    item: DeckListItem,
    onDelete: () -> Unit,
    onClick: () -> Unit,
    onExport: () -> Unit,
    onManage: () -> Unit,
    onCategory: () -> Unit,
) {
    DeckRow(
        item = item,
        onDelete = onDelete,
        onClick = onClick,
        onExport = onExport,
        onManage = onManage,
        onCategory = onCategory,
    )
}

private data class DeckCategorySection(val category: String, val decks: List<DeckListItem>)

@Composable
private fun DeckCategoryDialog(
    deckId: Long,
    currentCategory: String?,
    categories: List<String>,
    onDismiss: () -> Unit,
    onSave: (String?) -> Unit,
) {
    var selectedCategory by rememberSaveable(deckId) { mutableStateOf(currentCategory) }
    var newCategory by rememberSaveable(deckId) { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Categoria do baralho") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                CategoryChoice("Sem categoria", selectedCategory == null) { selectedCategory = null }
                categories.forEach { category ->
                    CategoryChoice(category, selectedCategory.equals(category, ignoreCase = true)) { selectedCategory = category }
                }
                OutlinedTextField(
                    value = newCategory,
                    onValueChange = { newCategory = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Nova categoria") },
                    singleLine = true,
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onSave(newCategory.trim().takeIf(String::isNotEmpty) ?: selectedCategory) }) { Text("Salvar") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } },
    )
}

@Composable
private fun CategoryChoice(label: String, selected: Boolean, onSelect: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onSelect),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(selected = selected, onClick = onSelect)
        Text(label)
    }
}

@Composable
private fun CategoryManagerDialog(
    categories: List<com.cristiano.flashlang.data.model.DeckCategoryCount>,
    onDismiss: () -> Unit,
    onRename: (String) -> Unit,
    onDelete: (String) -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Gerenciar categorias") },
        text = {
            if (categories.isEmpty()) {
                Text("Ainda não há categorias.")
            } else {
                Column(Modifier.verticalScroll(rememberScrollState())) {
                    categories.forEach { categoryCount ->
                        val category = categoryCount.category.orEmpty()
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(category, style = MaterialTheme.typography.titleSmall)
                                Text("${categoryCount.deckCount} baralhos", style = MaterialTheme.typography.bodySmall)
                            }
                            IconButton(onClick = { onRename(category) }) {
                                Icon(Icons.Outlined.DriveFileRenameOutline, contentDescription = "Renomear $category")
                            }
                            IconButton(onClick = { onDelete(category) }) {
                                Icon(Icons.Outlined.Delete, contentDescription = "Excluir $category")
                            }
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Fechar") } },
    )
}

@Composable
private fun RenameCategoryDialog(currentName: String, onDismiss: () -> Unit, onSave: (String) -> Unit) {
    var newName by rememberSaveable(currentName) { mutableStateOf(currentName) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Renomear categoria") },
        text = {
            OutlinedTextField(
                value = newName,
                onValueChange = { newName = it },
                label = { Text("Nome") },
                singleLine = true,
            )
        },
        confirmButton = { TextButton(onClick = { if (newName.isNotBlank()) onSave(newName) }, enabled = newName.isNotBlank()) { Text("Salvar") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } },
    )
}

@Composable
private fun DeckRow(
    item: DeckListItem,
    onDelete: () -> Unit,
    onClick: () -> Unit,
    onExport: () -> Unit,
    onManage: () -> Unit,
    onCategory: () -> Unit,
) {
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
                    DropdownMenuItem(
                        text = { Text("Categoria") },
                        leadingIcon = { Icon(Icons.Outlined.Folder, contentDescription = null) },
                        onClick = { menuExpanded = false; onCategory() },
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
