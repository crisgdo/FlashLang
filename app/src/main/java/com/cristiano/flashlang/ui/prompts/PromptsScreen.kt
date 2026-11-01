package com.cristiano.flashlang.ui.prompts

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.cristiano.flashlang.data.prompts.PromptInputType
import com.cristiano.flashlang.data.prompts.PromptInputValues
import com.cristiano.flashlang.data.prompts.PromptTemplate
import com.cristiano.flashlang.data.prompts.PromptTemplates
import com.cristiano.flashlang.data.prompts.buildPrompt
import com.cristiano.flashlang.data.prompts.canGeneratePrompt
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PromptsScreen(onBack: () -> Unit, onSelectPrompt: (String) -> Unit) {
    val categories = remember { PromptTemplates.all.groupBy { it.category } }
    Scaffold(
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
            categories.forEach { (category, templates) ->
                item(key = "category-$category") {
                    Text(category, style = MaterialTheme.typography.titleMedium)
                }
                items(templates, key = { it.id }) { template ->
                    Card(
                        modifier = Modifier.fillMaxWidth().clickable { onSelectPrompt(template.id) },
                    ) {
                        Column(Modifier.fillMaxWidth().padding(16.dp)) {
                            Text(template.title, style = MaterialTheme.typography.titleSmall)
                            Spacer(Modifier.height(4.dp))
                            Text(template.description, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PromptDetailScreen(
    template: PromptTemplate,
    onBack: () -> Unit,
    onImport: () -> Unit,
) {
    var language by remember(template.id) { mutableStateOf("en") }
    var text by remember(template.id) { mutableStateOf("") }
    var list by remember(template.id) { mutableStateOf("") }
    var focus by remember(template.id) { mutableStateOf("") }
    var previewExpanded by remember(template.id) { mutableStateOf(false) }
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
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(selected = language == "en", onClick = { language = "en" })
                    Text("Inglês")
                    Spacer(Modifier.width(16.dp))
                    RadioButton(selected = language == "es", onClick = { language = "es" })
                    Text("Espanhol")
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
            Button(onClick = onImport, modifier = Modifier.fillMaxWidth()) {
                Text("Ir para importar")
            }
        }
    }
}
