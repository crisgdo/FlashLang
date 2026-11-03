package com.cristiano.flashlang.ui.backup

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.FileDownload
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.Upload
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cristiano.flashlang.data.backup.BackupExport
import com.cristiano.flashlang.data.backup.BackupScope
import com.cristiano.flashlang.data.backup.RestoreConflictChoice
import com.cristiano.flashlang.data.exporter.createJsonShareChooser
import com.cristiano.flashlang.data.exporter.writeShareableExport
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
                }.onSuccess { snackbarHostState.showSnackbar("Backup salvo com sucesso.") }
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

    LaunchedEffect(state.export) {
        if (state.export != null) showExportActions = true
    }
    LaunchedEffect(state.message, state.errorMessage) {
        state.message?.let { snackbarHostState.showSnackbar(it); viewModel.dismissMessages() }
        state.errorMessage?.let { snackbarHostState.showSnackbar(it); viewModel.dismissMessages() }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Backup",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontSize = 21.sp,
                            fontWeight = FontWeight.SemiBold,
                        ),
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Voltar")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    scrolledContainerColor = MaterialTheme.colorScheme.background,
                ),
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            // Bloco de Resumo (.hero com display:block)
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(
                    modifier = Modifier.padding(18.dp, 16.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(
                        text = "Resumo",
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                    Text(
                        text = "${state.counts.deckCount} baralhos · ${state.counts.cardCount} cards",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                        ),
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                    Text(
                        text = "${state.counts.promptCount} prompts próprios",
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                }
            }

            // Checkbox: "Incluir progresso de estudo"
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { includeProgress = !includeProgress },
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Checkbox(
                        checked = includeProgress,
                        onCheckedChange = { includeProgress = it },
                    )
                    Text(
                        text = "Incluir progresso de estudo",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium,
                        ),
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
            }

            Spacer(Modifier.height(4.dp))

            // Botão Exportar Tudo (.btn-primary)
            Button(
                onClick = { viewModel.export(BackupScope.ALL, includeProgress) },
                enabled = !state.isLoading && !state.isRestoring,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                ),
            ) {
                Icon(
                    imageVector = Icons.Outlined.Upload,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "Exportar tudo",
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                    ),
                )
            }

            // Botão Exportar Só Baralhos (.btn-outline)
            OutlinedButton(
                onClick = { viewModel.export(BackupScope.DECKS, includeProgress) },
                enabled = !state.isLoading && !state.isRestoring,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.onBackground,
                ),
            ) {
                Text(
                    text = "Exportar só baralhos",
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                    ),
                )
            }

            // Botão Exportar Só Prompts Próprios (.btn-outline)
            OutlinedButton(
                onClick = { viewModel.export(BackupScope.PROMPTS, includeProgress = false) },
                enabled = !state.isLoading && !state.isRestoring,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.onBackground,
                ),
            ) {
                Text(
                    text = "Exportar só prompts próprios",
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                    ),
                )
            }

            HorizontalDivider(
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                modifier = Modifier.padding(vertical = 4.dp),
            )

            // Botão Restaurar de Arquivo (.btn-tonal)
            FilledTonalButton(
                onClick = { openDocument.launch(arrayOf("application/json", "text/*", "application/octet-stream")) },
                enabled = !state.isRestoring,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.filledTonalButtonColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                ),
            ) {
                Icon(
                    imageVector = Icons.Outlined.FileDownload,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "Restaurar de arquivo",
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                    ),
                )
            }

            if (state.isLoading || state.isRestoring) {
                Box(modifier = Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
        }
    }

    // Modal Customizado "Exportação Pronta"
    state.export?.let { export ->
        if (showExportActions) {
            ExportReadyDialog(
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
                onDismiss = {
                    showExportActions = false
                    viewModel.dismissExport()
                },
            )
        }
    }

    // Diálogo de Prévia de Restauração
    state.restorePreview?.let { preview ->
        AlertDialog(
            onDismissRequest = viewModel::dismissRestorePreview,
            shape = RoundedCornerShape(20.dp),
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

    // Diálogo de Confirmação de Substituição
    if (confirmReplace) {
        AlertDialog(
            onDismissRequest = { confirmReplace = false },
            shape = RoundedCornerShape(20.dp),
            title = { Text("Confirmar substituição") },
            text = { Text("Os baralhos e prompts existentes com o mesmo nome serão apagados e substituídos. Os cards dos baralhos substituídos também serão apagados.") },
            confirmButton = {
                TextButton(onClick = { confirmReplace = false; viewModel.restore(RestoreConflictChoice.REPLACE) }) {
                    Text("Substituir", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = { TextButton(onClick = { confirmReplace = false }) { Text("Cancelar") } },
        )
    }
}

@Composable
private fun ExportReadyDialog(
    export: BackupExport,
    onSave: () -> Unit,
    onShare: () -> Unit,
    onDismiss: () -> Unit,
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0x9E05060C))
                .clickable(onClick = onDismiss),
            contentAlignment = Alignment.Center,
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .clickable(enabled = false) {},
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                shadowElevation = 16.dp,
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "Exportação pronta",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontSize = 19.sp,
                                fontWeight = FontWeight.SemiBold,
                            ),
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Text(
                            text = buildAnnotatedString {
                                append("Escolha como salvar o arquivo ")
                                withStyle(SpanStyle(color = MaterialTheme.colorScheme.primary)) {
                                    append(export.suggestedFileName)
                                }
                                append(".")
                            },
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontSize = 14.sp,
                                lineHeight = 21.sp,
                            ),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }

                    Column(
                        modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Button(
                            onClick = onSave,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary,
                            ),
                        ) {
                            Icon(Icons.Outlined.FileDownload, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = "Salvar em arquivo",
                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                            )
                        }

                        FilledTonalButton(
                            onClick = onShare,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.filledTonalButtonColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer,
                                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                            ),
                        ) {
                            Icon(Icons.Outlined.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = "Compartilhar",
                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                            )
                        }

                        TextButton(
                            onClick = onDismiss,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(44.dp),
                        ) {
                            Text(
                                text = "Cancelar",
                                style = MaterialTheme.typography.labelLarge.copy(
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Medium,
                                ),
                            )
                        }
                    }
                }
            }
        }
    }
}
