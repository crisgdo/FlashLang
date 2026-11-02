package com.cristiano.flashlang.ui.prompts

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.cristiano.flashlang.data.prompts.PromptInputType
import com.cristiano.flashlang.data.prompts.PromptInputValues
import com.cristiano.flashlang.data.prompts.FORMAT_BLOCK
import com.cristiano.flashlang.data.prompts.PromptTemplate
import com.cristiano.flashlang.data.prompts.PromptTemplates
import com.cristiano.flashlang.data.model.UserPrompt
import com.cristiano.flashlang.data.prompts.buildPrompt
import com.cristiano.flashlang.data.prompts.canGeneratePrompt
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PromptsScreen(
    viewModel: UserPromptsViewModel,
    onBack: () -> Unit,
    onSelectPrompt: (String) -> Unit,
    onOpenUserPrompt: (Long) -> Unit,
    onCreateUserPrompt: () -> Unit,
    onEditUserPrompt: (Long) -> Unit,
) {
    val userPrompts by viewModel.prompts.collectAsStateWithLifecycle()
    var promptIdToDelete by rememberSaveable { mutableStateOf<Long?>(null) }
    val categories = remember { PromptTemplates.all.groupBy { it.category } }
    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = onCreateUserPrompt) {
                Icon(Icons.Outlined.Add, contentDescription = "Criar prompt")
            }
        },
        topBar = {
            TopAppBar(
                title = { Text("Prompts") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Voltar")
                    }
                },
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item(key = "my-prompts-header") {
                Text("Meus prompts", style = MaterialTheme.typography.titleMedium)
            }
            if (userPrompts.isEmpty()) {
                item(key = "my-prompts-empty") {
                    Text("Você ainda não criou prompts", style = MaterialTheme.typography.bodyMedium)
                }
            } else {
                items(userPrompts, key = { "user-${it.id}" }) { prompt ->
                    UserPromptCard(
                        prompt = prompt,
                        onOpen = { onOpenUserPrompt(prompt.id) },
                        onEdit = { onEditUserPrompt(prompt.id) },
                        onDuplicate = { viewModel.duplicate(prompt) },
                        onDelete = { promptIdToDelete = prompt.id },
                    )
                }
            }
            item(key = "ready-prompts-header") {
                Text("Prompts prontos", style = MaterialTheme.typography.titleMedium)
            }
            categories.forEach { (category, templates) ->
                item(key = "category-$category") {
                    Text(category, style = MaterialTheme.typography.titleMedium)
                }
                items(templates, key = { it.id }) { template ->
                    ReadyPromptCard(
                        template = template,
                        onOpen = { onSelectPrompt(template.id) },
                        onDuplicate = { viewModel.duplicate(template) },
                    )
                }
            }
        }
    }

    promptIdToDelete?.let { promptId ->
        val prompt = userPrompts.firstOrNull { it.id == promptId }
        if (prompt != null) {
            AlertDialog(
                onDismissRequest = { promptIdToDelete = null },
                title = { Text("Excluir prompt?") },
                text = { Text("O prompt \"${prompt.title}\" será excluído.") },
                confirmButton = {
                    TextButton(onClick = { viewModel.delete(prompt.id); promptIdToDelete = null }) { Text("Excluir") }
                },
                dismissButton = { TextButton(onClick = { promptIdToDelete = null }) { Text("Cancelar") } },
            )
        }
    }
}

@Composable
private fun UserPromptCard(
    prompt: UserPrompt,
    onOpen: () -> Unit,
    onEdit: () -> Unit,
    onDuplicate: () -> Unit,
    onDelete: () -> Unit,
) {
    var menuExpanded by remember(prompt.id) { mutableStateOf(false) }
    Card(Modifier.fillMaxWidth().clickable(onClick = onOpen)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(start = 16.dp, top = 8.dp, bottom = 8.dp, end = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f).padding(vertical = 8.dp)) {
                Text(prompt.title, style = MaterialTheme.typography.titleSmall)
                if (prompt.description.isNotBlank()) {
                    Spacer(Modifier.height(4.dp))
                    Text(prompt.description, style = MaterialTheme.typography.bodyMedium)
                }
            }
            Box {
                IconButton(onClick = { menuExpanded = true }) {
                    Icon(Icons.Outlined.MoreVert, contentDescription = "Mais opções")
                }
                DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                    DropdownMenuItem(
                        text = { Text("Editar") },
                        leadingIcon = { Icon(Icons.Outlined.Edit, contentDescription = null) },
                        onClick = { menuExpanded = false; onEdit() },
                    )
                    DropdownMenuItem(
                        text = { Text("Duplicar") },
                        leadingIcon = { Icon(Icons.Outlined.ContentCopy, contentDescription = null) },
                        onClick = { menuExpanded = false; onDuplicate() },
                    )
                    DropdownMenuItem(
                        text = { Text("Excluir", color = MaterialTheme.colorScheme.error) },
                        leadingIcon = { Icon(Icons.Outlined.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
                        onClick = { menuExpanded = false; onDelete() },
                    )
                }
            }
        }
    }
}

@Composable
private fun ReadyPromptCard(template: PromptTemplate, onOpen: () -> Unit, onDuplicate: () -> Unit) {
    var menuExpanded by remember(template.id) { mutableStateOf(false) }
    Card(Modifier.fillMaxWidth().clickable(onClick = onOpen)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(start = 16.dp, top = 8.dp, bottom = 8.dp, end = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f).padding(vertical = 8.dp)) {
                Text(template.title, style = MaterialTheme.typography.titleSmall)
                Spacer(Modifier.height(4.dp))
                Text(template.description, style = MaterialTheme.typography.bodyMedium)
            }
            Box {
                IconButton(onClick = { menuExpanded = true }) {
                    Icon(Icons.Outlined.MoreVert, contentDescription = "Mais opções")
                }
                DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                    DropdownMenuItem(
                        text = { Text("Duplicar como meu prompt") },
                        leadingIcon = { Icon(Icons.Outlined.ContentCopy, contentDescription = null) },
                        onClick = { menuExpanded = false; onDuplicate() },
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun PromptDetailScreen(
    template: PromptTemplate,
    onBack: () -> Unit,
    onImport: () -> Unit,
) {
    var language by rememberSaveable(template.id) { mutableStateOf("en") }
    var text by rememberSaveable(template.id) { mutableStateOf("") }
    var list by rememberSaveable(template.id) { mutableStateOf("") }
    var focus by rememberSaveable(template.id) { mutableStateOf("") }
    var previewExpanded by rememberSaveable(template.id) { mutableStateOf(false) }
    val clipboardManager = LocalClipboardManager.current
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    val values = PromptInputValues(text = text, list = list, focus = focus)
    val prompt = buildPrompt(template, language, values)

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(template.title, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Voltar")
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(template.description, style = MaterialTheme.typography.bodyLarge)
            if (template.needsLanguage) {
                Text("Idioma de estudo", style = MaterialTheme.typography.titleSmall)
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(selected = language == "en", onClick = { language = "en" })
                        Text("Inglês", maxLines = 1, softWrap = false)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(selected = language == "es", onClick = { language = "es" })
                        Text("Espanhol", maxLines = 1, softWrap = false)
                    }
                }
            }
            template.inputs.forEach { input ->
                val value = when (input.type) {
                    PromptInputType.TEXT -> text
                    PromptInputType.LIST -> list
                    PromptInputType.FOCUS -> focus
                }
                val onValueChange: (String) -> Unit = { updated ->
                    when (input.type) {
                        PromptInputType.TEXT -> text = updated
                        PromptInputType.LIST -> list = updated
                        PromptInputType.FOCUS -> focus = updated
                    }
                }
                OutlinedTextField(
                    value = value,
                    onValueChange = onValueChange,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(input.label + if (input.required) "" else " (opcional)") },
                    placeholder = { Text(input.hint) },
                    minLines = when (input.type) {
                        PromptInputType.TEXT -> 8
                        PromptInputType.LIST -> 5
                        PromptInputType.FOCUS -> 2
                    },
                )
            }
            TextButton(onClick = { previewExpanded = !previewExpanded }) {
                Text(if (previewExpanded) "Ocultar prévia" else "Mostrar prévia do prompt")
            }
            if (previewExpanded) {
                Card(Modifier.fillMaxWidth()) {
                    Text(
                        prompt,
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
            Button(
                onClick = {
                    clipboardManager.setText(AnnotatedString(prompt))
                    coroutineScope.launch { snackbarHostState.showSnackbar("Prompt copiado!") }
                },
                enabled = canGeneratePrompt(template, values),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(Icons.Outlined.ContentCopy, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Copiar prompt")
            }
            if (template.generatesDeck) {
                Button(onClick = onImport, modifier = Modifier.fillMaxWidth()) {
                    Text("Ir para importar")
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun UserPromptEditorScreen(
    prompt: UserPrompt?,
    onBack: () -> Unit,
    onSave: suspend (title: String, description: String, body: String, generatesDeck: Boolean) -> Unit,
) {
    var title by rememberSaveable(prompt?.id) { mutableStateOf(prompt?.title.orEmpty()) }
    var description by rememberSaveable(prompt?.id) { mutableStateOf(prompt?.description.orEmpty()) }
    var body by rememberSaveable(prompt?.id, stateSaver = TextFieldValue.Saver) {
        mutableStateOf(TextFieldValue(prompt?.body.orEmpty(), selection = TextRange(prompt?.body.orEmpty().length)))
    }
    var generatesDeck by rememberSaveable(prompt?.id) { mutableStateOf(prompt?.generatesDeck ?: true) }
    var isSaving by remember(prompt?.id) { mutableStateOf(false) }
    var errorMessage by rememberSaveable(prompt?.id) { mutableStateOf<String?>(null) }
    val coroutineScope = rememberCoroutineScope()

    fun insertMarker(marker: String) {
        val start = minOf(body.selection.start, body.selection.end).coerceIn(0, body.text.length)
        val end = maxOf(body.selection.start, body.selection.end).coerceIn(0, body.text.length)
        val updated = body.text.replaceRange(start, end, marker)
        body = TextFieldValue(updated, selection = TextRange(start + marker.length))
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (prompt == null) "Criar prompt" else "Editar prompt") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Voltar")
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            OutlinedTextField(
                value = title,
                onValueChange = { title = it; errorMessage = null },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Título (obrigatório)") },
                singleLine = true,
            )
            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Descrição (opcional)") },
                minLines = 2,
            )
            OutlinedTextField(
                value = body,
                onValueChange = { body = it; errorMessage = null },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Corpo do prompt (obrigatório)") },
                minLines = 10,
            )
            Text("Inserir marcador no cursor", style = MaterialTheme.typography.titleSmall)
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                listOf("{{IDIOMA}}", "{{TEXTO}}", "{{LISTA}}", "{{FOCO}}").forEach { marker ->
                    AssistChip(
                        onClick = { insertMarker(marker) },
                        label = { Text(marker, maxLines = 1, softWrap = false) },
                    )
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text("Este prompt gera um baralho")
                Switch(checked = generatesDeck, onCheckedChange = { generatesDeck = it })
            }
            if (generatesDeck) {
                Text(
                    "Este trecho é adicionado automaticamente para o app conseguir importar a resposta",
                    style = MaterialTheme.typography.bodyMedium,
                )
                Card(Modifier.fillMaxWidth()) {
                    Text(
                        FORMAT_BLOCK,
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
            errorMessage?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            Button(
                onClick = {
                    coroutineScope.launch {
                        isSaving = true
                        errorMessage = null
                        runCatching { onSave(title, description, body.text, generatesDeck) }
                            .onFailure { errorMessage = "Não foi possível salvar o prompt. Tente novamente." }
                        isSaving = false
                    }
                },
                enabled = !isSaving && title.isNotBlank() && body.text.isNotBlank(),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(if (isSaving) "Salvando…" else "Salvar prompt")
            }
        }
    }
}
