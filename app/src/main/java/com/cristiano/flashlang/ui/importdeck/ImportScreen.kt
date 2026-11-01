package com.cristiano.flashlang.ui.importdeck

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TextButton
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.FileOpen
import androidx.compose.material.icons.outlined.FileDownload
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
fun ImportScreen(
    viewModel: ImportViewModel,
    onImportSuccess: () -> Unit,
    onBack: () -> Unit,
    onMorePrompts: () -> Unit,
) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current
    var selectedLanguage by remember { mutableStateOf("inglês") }
    val clipboardManager = LocalClipboardManager.current
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    val documentPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            coroutineScope.launch {
                val content = runCatching {
                    withContext(Dispatchers.IO) {
                        context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
                            ?: error("Arquivo vazio")
                    }
                }
                content.onSuccess(viewModel::onJsonChanged).onFailure { viewModel.onFileReadError() }
            }
        }
    }
    LaunchedEffect(state.imported) {
        if (state.imported) onImportSuccess()
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Importar baralho") },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Voltar") }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text("Idioma de estudo", style = MaterialTheme.typography.titleMedium)
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                    RadioButton(selected = selectedLanguage == "inglês", onClick = { selectedLanguage = "inglês" })
                    Text("Inglês", maxLines = 1, softWrap = false)
                }
                Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                    RadioButton(selected = selectedLanguage == "espanhol", onClick = { selectedLanguage = "espanhol" })
                    Text("Espanhol", maxLines = 1, softWrap = false)
                }
            }
            TextButton(
                onClick = {
                    clipboardManager.setText(AnnotatedString(buildImportPrompt(selectedLanguage)))
                    coroutineScope.launch { snackbarHostState.showSnackbar("Prompt copiado!") }
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(Icons.Outlined.ContentCopy, contentDescription = null)
                androidx.compose.foundation.layout.Spacer(Modifier.width(8.dp))
                Text("Copiar prompt")
            }
            TextButton(onClick = onMorePrompts, modifier = Modifier.fillMaxWidth()) {
                Text("Mais prompts")
            }
            Text("Cole abaixo o JSON do baralho.", style = MaterialTheme.typography.bodyLarge)
            OutlinedTextField(
                value = state.categoryName,
                onValueChange = viewModel::onCategoryChanged,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Categoria (opcional)") },
                singleLine = true,
            )
            Button(
                onClick = { documentPicker.launch(arrayOf("application/json", "text/*", "application/octet-stream")) },
                enabled = !state.isSaving,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(Icons.Outlined.FileOpen, contentDescription = null)
                androidx.compose.foundation.layout.Spacer(Modifier.width(8.dp))
                Text("Escolher arquivo")
            }
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
                else {
                    Icon(Icons.Outlined.FileDownload, contentDescription = null)
                    androidx.compose.foundation.layout.Spacer(Modifier.width(8.dp))
                    Text("Importar")
                }
            }
        }
    }

    state.duplicateDeckName?.let { name ->
        AlertDialog(
            onDismissRequest = { viewModel.resolveDuplicate(DuplicateAction.CANCEL) },
            title = { Text("Baralho já existe") },
            text = { Text("Já existe um baralho chamado \"$name\". O que deseja fazer?") },
            confirmButton = {
                TextButton(onClick = { viewModel.resolveDuplicate(DuplicateAction.REPLACE) }) { Text("Substituir") }
            },
            dismissButton = {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    TextButton(onClick = { viewModel.resolveDuplicate(DuplicateAction.KEEP_BOTH) }) {
                        Text("Manter os dois", maxLines = 1, softWrap = false)
                    }
                    TextButton(onClick = { viewModel.resolveDuplicate(DuplicateAction.CANCEL) }) {
                        Text("Cancelar", maxLines = 1, softWrap = false)
                    }
                }
            },
        )
    }
}
