package com.cristiano.flashlang.ui.backup

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Backup
import androidx.compose.material.icons.outlined.FileDownload
import androidx.compose.material.icons.outlined.FileOpen
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.cristiano.flashlang.data.backup.BackupExport
import com.cristiano.flashlang.data.exporter.createJsonShareChooser
import com.cristiano.flashlang.data.exporter.writeShareableExport
import com.cristiano.flashlang.data.backup.BackupScope
import com.cristiano.flashlang.data.backup.RestoreConflictChoice
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BackupScreen(viewModel: BackupViewModel, onBack: () -> Unit) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    var includeProgress by rememberSaveable { mutableStateOf(true) }
    var showExportActions by rememberSaveable { mutableStateOf(false) }
    var confirmReplace by rememberSaveable { mutableStateOf(false) }
    var fileJson by remember { mutableStateOf<String?>(null) }

    val createDocument = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        val content = fileJson
        if (uri != null && content != null) {
            scope.launch {
                runCatching {
                    withContext(Dispatchers.IO) {
                        context.contentResolver.openOutputStream(uri)?.bufferedWriter()?.use { it.write(content) }
                            ?: error("Não foi possível abrir o arquivo para escrita.")
                    }
                }.onSuccess { snackbarHostState.showSnackbar("Backup salvo.") }
                    .onFailure { snackbarHostState.showSnackbar(it.message ?: "Não foi possível salvar o backup.") }
                fileJson = null
                viewModel.dismissExport()
                showExportActions = false
            }
        }
    }
    val openDocument = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            scope.launch {
                val content = runCatching {
                    withContext(Dispatchers.IO) {
                        context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
                            ?: error("Não foi possível ler o arquivo selecionado.")
                    }
                }
                content.onSuccess(viewModel::prepareRestore)
                    .onFailure { snackbarHostState.showSnackbar(it.message ?: "Não foi possível ler o arquivo.") }
            }
        }
    }

    LaunchedEffect(state.export) { if (state.export != null) showExportActions = true }
    LaunchedEffect(state.message, state.errorMessage) {
        state.message?.let { snackbarHostState.showSnackbar(it); viewModel.dismissMessages() }
        state.errorMessage?.let { snackbarHostState.showSnackbar(it); viewModel.dismissMessages() }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Backup") },
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
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Resumo", style = MaterialTheme.typography.titleMedium)
                    Text("${state.counts.deckCount} baralhos · ${state.counts.cardCount} cards · ${state.counts.promptCount} prompts próprios")
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Switch(checked = includeProgress, onCheckedChange = { includeProgress = it })
                Text("Incluir progresso de estudo")
            }
            Button(
                onClick = { viewModel.export(BackupScope.ALL, includeProgress) },
                enabled = !state.isLoading && !state.isRestoring,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(Icons.Outlined.Backup, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Exportar tudo")
            }
            Button(
                onClick = { viewModel.export(BackupScope.DECKS, includeProgress) },
                enabled = !state.isLoading && !state.isRestoring,
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Exportar só baralhos") }
            Button(
                onClick = { viewModel.export(BackupScope.PROMPTS, includeProgress = false) },
                enabled = !state.isLoading && !state.isRestoring,
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Exportar só prompts próprios") }
            TextButton(
                onClick = { openDocument.launch(arrayOf("application/json", "text/*", "application/octet-stream")) },
                enabled = !state.isRestoring,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(Icons.Outlined.FileOpen, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Restaurar de arquivo")
            }
            if (state.isLoading || state.isRestoring) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
            }
        }
    }

    state.export?.let { export ->
        if (showExportActions) {
            ExportActionsDialog(
                export = export,
                onSave = {
                    fileJson = export.json
                    createDocument.launch(export.suggestedFileName)
                },
                onShare = {
                    scope.launch {
                        try {
                            val uri = withContext(Dispatchers.IO) {
                                writeShareableExport(context, export.suggestedFileName, export.json)
                            }
                            context.startActivity(createJsonShareChooser(uri, export.suggestedFileName, "Compartilhar backup"))
                            viewModel.dismissExport()
                            showExportActions = false
                        } catch (exception: Exception) {
                            snackbarHostState.showSnackbar(exception.message ?: "Não foi possível compartilhar o backup.")
                        }
                    }
                },
                onDismiss = { showExportActions = false; viewModel.dismissExport() },
            )
        }
    }

    state.restorePreview?.let { preview ->
        AlertDialog(
            onDismissRequest = viewModel::dismissRestorePreview,
            title = { Text("Restaurar backup?") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("${preview.decks.size} baralhos, ${preview.cardCount} cards, ${preview.prompts.size} prompts")
                    if (preview.conflictCount > 0) {
                        Text("Conflitos com itens existentes: ${preview.conflictCount}.")
                        TextButton(onClick = { viewModel.restore(RestoreConflictChoice.KEEP_BOTH) }, modifier = Modifier.fillMaxWidth()) {
                            Text("Manter os dois (renomear com sufixo)")
                        }
                        TextButton(onClick = { confirmReplace = true }, modifier = Modifier.fillMaxWidth()) {
                            Text("Substituir")
                        }
                        TextButton(onClick = { viewModel.restore(RestoreConflictChoice.SKIP) }, modifier = Modifier.fillMaxWidth()) {
                            Text("Pular conflitos")
                        }
                    } else {
                        TextButton(onClick = { viewModel.restore(RestoreConflictChoice.KEEP_BOTH) }, modifier = Modifier.fillMaxWidth()) {
                            Text("Restaurar")
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = viewModel::dismissRestorePreview) { Text("Cancelar") } },
        )
    }

    if (confirmReplace) {
        AlertDialog(
            onDismissRequest = { confirmReplace = false },
            title = { Text("Confirmar substituição") },
            text = { Text("Os baralhos e prompts existentes com o mesmo nome serão apagados e substituídos. Os cards dos baralhos substituídos também serão apagados.") },
            confirmButton = {
                TextButton(onClick = { confirmReplace = false; viewModel.restore(RestoreConflictChoice.REPLACE) }) { Text("Substituir") }
            },
            dismissButton = { TextButton(onClick = { confirmReplace = false }) { Text("Cancelar") } },
        )
    }
}

@Composable
private fun ExportActionsDialog(export: BackupExport, onSave: () -> Unit, onShare: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Exportação pronta") },
        text = { Text("Escolha como salvar o arquivo ${export.suggestedFileName}.") },
        confirmButton = {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = onSave) {
                    Icon(Icons.Outlined.FileDownload, contentDescription = null)
                    Spacer(Modifier.width(4.dp))
                    Text("Salvar em arquivo")
                }
                TextButton(onClick = onShare) {
                    Icon(Icons.Outlined.Share, contentDescription = null)
                    Spacer(Modifier.width(4.dp))
                    Text("Compartilhar")
                }
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } },
    )
}
