package com.cristiano.flashlang.ui.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.List
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Backup
import androidx.compose.material.icons.outlined.Category
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.DriveFileRenameOutline
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.LocalOffer
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.Upload
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cristiano.flashlang.data.model.DeckListItem
import com.cristiano.flashlang.ui.theme.BadgeAmberBackgroundDark
import com.cristiano.flashlang.ui.theme.BadgeAmberBackgroundLight
import com.cristiano.flashlang.ui.theme.BadgeAmberTextDark
import com.cristiano.flashlang.ui.theme.BadgeAmberTextLight
import com.cristiano.flashlang.ui.theme.BadgeGreenBackgroundDark
import com.cristiano.flashlang.ui.theme.BadgeGreenBackgroundLight
import com.cristiano.flashlang.ui.theme.BadgeGreenTextDark
import com.cristiano.flashlang.ui.theme.BadgeGreenTextLight
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

    val collapsedSections = rememberSaveable(saver = listSaver(
        save = { it.toList() },
        restore = { mutableStateListOf<String>().apply { addAll(it) } }
    )) { mutableStateListOf<String>() }

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

    LaunchedEffect(filterExists, selectedFilter) {
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
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Meus baralhos",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontSize = 21.sp,
                        fontWeight = FontWeight.SemiBold,
                    ),
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.weight(1f),
                )
                IconButton(onClick = onPromptsClick) {
                    Icon(
                        Icons.Outlined.AutoAwesome,
                        contentDescription = "Prompts",
                        tint = MaterialTheme.colorScheme.onBackground,
                    )
                }
                Box {
                    IconButton(onClick = { topMenuExpanded = true }) {
                        Icon(
                            Icons.Outlined.MoreVert,
                            contentDescription = "Mais opções",
                            tint = if (topMenuExpanded) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onBackground,
                        )
                    }
                    DropdownMenu(
                        expanded = topMenuExpanded,
                        onDismissRequest = { topMenuExpanded = false },
                        modifier = Modifier.width(220.dp),
                        shape = RoundedCornerShape(14.dp),
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                        shadowElevation = 8.dp,
                        tonalElevation = 0.dp,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)),
                    ) {
                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = "Backup",
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Normal,
                                    ),
                                    color = MaterialTheme.colorScheme.onSurface,
                                )
                            },
                            leadingIcon = {
                                Icon(
                                    Icons.Outlined.Backup,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(18.dp),
                                )
                            },
                            onClick = {
                                topMenuExpanded = false
                                onBackupClick()
                            },
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
                        )
                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = "Gerenciar categorias",
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Normal,
                                    ),
                                    color = MaterialTheme.colorScheme.onSurface,
                                )
                            },
                            leadingIcon = {
                                Icon(
                                    Icons.Outlined.Category,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(18.dp),
                                )
                            },
                            onClick = {
                                topMenuExpanded = false
                                showCategoryManager = true
                            },
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
                        )
                    }
                }
            }
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onImportClick,
                shape = RoundedCornerShape(18.dp),
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                icon = { Icon(Icons.Outlined.Add, contentDescription = null) },
                text = { Text("Importar", fontWeight = FontWeight.SemiBold, fontSize = 15.sp) },
            )
        },
    ) { padding ->
        if (decks.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Text(
                    text = "Nenhum baralho ainda. Importe o primeiro!",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            val totalPending = filteredDecks.sumOf { it.pendingCount }
            val activeDecksCount = filteredDecks.count { it.cardCount > 0 }

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(bottom = 96.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                // Chips de Filtro
                item {
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        item {
                            CategoryFilterChip(
                                label = "Todas",
                                isSelected = selectedFilter == "__all__",
                                onClick = {
                                    selectedFilter = "__all__"
                                    filterPreferences.edit().putString("deck_category_filter", "__all__").apply()
                                },
                            )
                        }
                        items(categoryNames, key = { "category-${it.lowercase(Locale.ROOT)}" }) { category ->
                            val key = "category:${category.lowercase(Locale.ROOT)}"
                            CategoryFilterChip(
                                label = category,
                                isSelected = selectedFilter == key,
                                onClick = {
                                    selectedFilter = key
                                    filterPreferences.edit().putString("deck_category_filter", key).apply()
                                },
                            )
                        }
                        if (hasUncategorized) {
                            item {
                                CategoryFilterChip(
                                    label = "Sem categoria",
                                    isSelected = selectedFilter == "__none__",
                                    onClick = {
                                        selectedFilter = "__none__"
                                        filterPreferences.edit().putString("deck_category_filter", "__none__").apply()
                                    },
                                )
                            }
                        }
                    }
                }

                // Hero Banner
                item {
                    HeroBanner(
                        totalPending = totalPending,
                        totalDecks = activeDecksCount,
                        modifier = Modifier.padding(horizontal = 16.dp),
                    )
                }

                if (filteredDecks.isEmpty()) {
                    item {
                        Text(
                            text = "Nenhum baralho nesta categoria.",
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                } else if (selectedFilter == "__all__") {
                    sections.forEach { section ->
                        val isCollapsed = collapsedSections.contains(section.category)
                        val sectionPending = section.decks.sumOf { it.pendingCount }

                        item(key = "section-${section.category.lowercase(Locale.ROOT)}") {
                            SectionHeader(
                                category = section.category,
                                deckCount = section.decks.size,
                                pendingCount = sectionPending,
                                isCollapsed = isCollapsed,
                                onToggleCollapse = {
                                    if (isCollapsed) collapsedSections.remove(section.category)
                                    else collapsedSections.add(section.category)
                                },
                                modifier = Modifier.padding(horizontal = 16.dp),
                            )
                        }

                        if (!isCollapsed) {
                            items(section.decks, key = { "deck-${it.deck.id}" }) { item ->
                                Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                                    HomeDeckRow(
                                        item = item,
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
                } else {
                    items(filteredDecks.sortedBy { it.deck.name.lowercase(Locale.ROOT) }, key = { "deck-${it.deck.id}" }) { item ->
                        Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                            HomeDeckRow(
                                item = item,
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

    deckIdToDelete?.let { deckId ->
        val item = decks.firstOrNull { it.deck.id == deckId }
        if (item != null) {
            AlertDialog(
                onDismissRequest = { deckIdToDelete = null },
                title = { Text("Excluir baralho?") },
                text = { Text("O baralho ${item.deck.name} e seus cards serão excluídos.") },
                confirmButton = {
                    TextButton(onClick = {
                        viewModel.deleteDeck(deckId)
                        deckIdToDelete = null
                    }) { Text("Excluir", color = MaterialTheme.colorScheme.error) }
                },
                dismissButton = { TextButton(onClick = { deckIdToDelete = null }) { Text("Cancelar") } },
            )
        }
    }

    deckIdToCategorize?.let { deckId ->
        val item = decks.firstOrNull { it.deck.id == deckId }
        if (item != null) {
            DeckCategoryDialog(
                deckId = deckId,
                currentCategory = item.deck.category,
                categories = categoryNames,
                onDismiss = { deckIdToCategorize = null },
                onSave = { category ->
                    viewModel.setDeckCategory(deckId, category)
                    deckIdToCategorize = null
                },
            )
        }
    }

    if (showCategoryManager) {
        CategoryManagerDialog(
            categories = categories,
            onDismiss = { showCategoryManager = false },
            onRename = { category ->
                categoryToRename = category
            },
            onDelete = { category ->
                categoryToDelete = category
            },
        )
    }

    categoryToRename?.let { currentName ->
        RenameCategoryDialog(
            currentName = currentName,
            onDismiss = { categoryToRename = null },
            onSave = { newName ->
                viewModel.renameCategory(currentName, newName)
                categoryToRename = null
            },
        )
    }

    categoryToDelete?.let { categoryName ->
        AlertDialog(
            onDismissRequest = { categoryToDelete = null },
            title = { Text("Excluir categoria?") },
            text = { Text("Os baralhos da categoria \"$categoryName\" ficarão sem categoria.") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.removeCategory(categoryName)
                    categoryToDelete = null
                }) { Text("Excluir", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { categoryToDelete = null }) { Text("Cancelar") } },
        )
    }
}

@Composable
private fun CategoryFilterChip(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
        border = if (isSelected) null else BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp, fontWeight = FontWeight.Medium),
            color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(horizontal = 15.dp, vertical = 10.dp),
        )
    }
}

@Composable
private fun HeroBanner(
    totalPending: Int,
    totalDecks: Int,
    modifier: Modifier = Modifier,
) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.primaryContainer,
        modifier = modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 18.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom,
        ) {
            Column {
                Text(
                    text = "Para revisar hoje",
                    style = MaterialTheme.typography.labelMedium.copy(fontSize = 12.sp),
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "$totalPending cards",
                    style = MaterialTheme.typography.headlineLarge.copy(
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold,
                        lineHeight = 36.sp,
                    ),
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
            Text(
                text = if (totalPending == 0 && totalDecks > 0) "Tudo em dia!" else "em $totalDecks ${if (totalDecks == 1) "baralho" else "baralhos"}",
                style = MaterialTheme.typography.labelMedium.copy(fontSize = 12.sp),
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
        }
    }
}

@Composable
private fun SectionHeader(
    category: String,
    deckCount: Int,
    pendingCount: Int,
    isCollapsed: Boolean,
    onToggleCollapse: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onToggleCollapse)
            .padding(horizontal = 4.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = category,
            style = MaterialTheme.typography.titleMedium.copy(fontSize = 16.sp, fontWeight = FontWeight.SemiBold),
            color = MaterialTheme.colorScheme.onBackground,
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                text = "$deckCount ${if (deckCount == 1) "baralho" else "baralhos"} · $pendingCount hoje",
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Icon(
                imageVector = Icons.Outlined.KeyboardArrowDown,
                contentDescription = if (isCollapsed) "Expandir $category" else "Recolher $category",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .size(18.dp)
                    .rotate(if (isCollapsed) -90f else 0f),
            )
        }
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
    var menuExpanded by remember(item.deck.id) { mutableStateOf(false) }
    val isDark = isSystemInDarkTheme()

    val amberBg = if (isDark) BadgeAmberBackgroundDark else BadgeAmberBackgroundLight
    val amberText = if (isDark) BadgeAmberTextDark else BadgeAmberTextLight
    val greenBg = if (isDark) BadgeGreenBackgroundDark else BadgeGreenBackgroundLight
    val greenText = if (isDark) BadgeGreenTextDark else BadgeGreenTextLight

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surface,
        border = if (menuExpanded) {
            BorderStroke(1.dp, MaterialTheme.colorScheme.primary)
        } else null,
        modifier = Modifier
            .fillMaxWidth()
            .semantics {
                contentDescription = "Estudar ${item.deck.name}"
                role = Role.Button
            },
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Column(
                modifier = Modifier.weight(1f),
            ) {
                Text(
                    text = item.deck.name,
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        lineHeight = 20.sp,
                    ),
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = "${languageName(item.deck.language)} · ${item.cardCount} cards",
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }

            Surface(
                shape = CircleShape,
                color = if (item.pendingCount > 0) amberBg else greenBg,
            ) {
                Text(
                    text = if (item.pendingCount > 0) "${item.pendingCount} hoje" else "Em dia",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                    ),
                    color = if (item.pendingCount > 0) amberText else greenText,
                    modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp),
                    maxLines = 1,
                    softWrap = false,
                )
            }

            Box {
                IconButton(
                    onClick = { menuExpanded = true },
                    modifier = Modifier.size(32.dp),
                ) {
                    Icon(
                        Icons.Outlined.MoreVert,
                        contentDescription = "Mais opções",
                        tint = if (menuExpanded) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        },
                        modifier = Modifier.size(18.dp),
                    )
                }
                DropdownMenu(
                    expanded = menuExpanded,
                    onDismissRequest = { menuExpanded = false },
                    modifier = Modifier.width(200.dp),
                    shape = RoundedCornerShape(14.dp),
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    shadowElevation = 8.dp,
                    tonalElevation = 0.dp,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)),
                ) {
                    DropdownMenuItem(
                        text = {
                            Text(
                                text = "Gerenciar",
                                style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp),
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.AutoMirrored.Outlined.List,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp),
                            )
                        },
                        onClick = {
                            menuExpanded = false
                            onManage()
                        },
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
                    )
                    DropdownMenuItem(
                        text = {
                            Text(
                                text = "Exportar",
                                style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp),
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Outlined.Upload,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp),
                            )
                        },
                        onClick = {
                            menuExpanded = false
                            onExport()
                        },
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
                    )
                    DropdownMenuItem(
                        text = {
                            Text(
                                text = "Categoria",
                                style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp),
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Outlined.LocalOffer,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp),
                            )
                        },
                        onClick = {
                            menuExpanded = false
                            onCategory()
                        },
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
                    )
                    DropdownMenuItem(
                        text = {
                            Text(
                                text = "Excluir",
                                style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp),
                                color = MaterialTheme.colorScheme.error,
                            )
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Outlined.Delete,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(18.dp),
                            )
                        },
                        onClick = {
                            menuExpanded = false
                            onDelete()
                        },
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
                    )
                }
            }
        }
    }
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
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onSelect),
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

private fun languageName(language: String): String = when (language) {
    "en" -> "Inglês"
    "es" -> "Espanhol"
    else -> language
}
