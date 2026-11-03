package com.cristiano.flashlang.ui.study

import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.speech.tts.Voice
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.IconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.automirrored.outlined.VolumeUp
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.RecordVoiceOver
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VolumeUp
import androidx.compose.material.icons.outlined.School
import androidx.compose.material.icons.outlined.PlayCircle
import androidx.compose.material.icons.outlined.Replay
import androidx.compose.material.icons.outlined.SlowMotionVideo
import androidx.compose.material.icons.outlined.FormatQuote
import androidx.compose.material.icons.outlined.Headphones
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import com.cristiano.flashlang.data.model.Card as FlashCard
import com.cristiano.flashlang.data.importer.ClozeText
import com.cristiano.flashlang.data.scheduler.ReviewRating
import com.cristiano.flashlang.ui.theme.extendedColors
import com.cristiano.flashlang.ui.theme.spacing
import com.cristiano.flashlang.BuildConfig
import java.util.Locale
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun StudyScreen(
    viewModel: StudyViewModel,
    onBack: () -> Unit,
    onEditCard: (Long) -> Unit,
    onManageDeck: () -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val currentCard = state.cards.getOrNull(if (state.isAllCardsMode) state.currentIndex else 0)
    var showCardMenu by rememberSaveable { mutableStateOf(false) }
    var showVoiceDialog by rememberSaveable { mutableStateOf(false) }
    var showStudyModeDialog by rememberSaveable { mutableStateOf(false) }
    val context = LocalContext.current
    val deckPreferences = remember(context) {
        val sharedPreferences = context.getSharedPreferences("flashlang_study_preferences", Context.MODE_PRIVATE)
        DeckStudyPreferences(
            object : StudyPreferenceStore {
                override fun getString(key: String): String? = sharedPreferences.getString(key, null)
                override fun putString(key: String, value: String) { sharedPreferences.edit().putString(key, value).apply() }
            },
        )
    }
    var studyMode by remember(state.deckId) { mutableStateOf(deckPreferences.getMode(state.deckId)) }
    var autoplayEnabled by remember(state.deckId) { mutableStateOf(deckPreferences.isAutoplayEnabled(state.deckId)) }
    var wordRevealed by rememberSaveable(state.deckId, currentCard?.id) { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    var speechEngineState by remember { mutableStateOf(SpeechEngineState()) }
    val speechController = remember(context) {
        SpeechEngineController(
            context.applicationContext,
            onStateChanged = { speechEngineState = it },
            onNetworkVoiceFailure = {
                coroutineScope.launch {
                    snackbarHostState.showSnackbar("A voz online falhou por falta de conexão. Uma voz offline foi selecionada.")
                }
            },
        )
    }
    DisposableEffect(speechController, state.language) {
        speechController.initialize(state.language)
        onDispose {
            speechController.shutdown()
        }
    }

    DisposableEffect(state.deckId, currentCard?.id, state.currentIndex, speechController) {
        onDispose { speechController.stop() }
    }

    LaunchedEffect(
        state.deckId,
        currentCard?.id,
        state.currentIndex,
        state.isFlipped,
        studyMode,
        autoplayEnabled,
        speechEngineState.isReady,
    ) {
        if (!speechEngineState.isReady || !autoplayEnabled || studyMode != StudyMode.PRONUNCIATION) return@LaunchedEffect
        val card = currentCard ?: return@LaunchedEffect
        when {
            card.format == "cloze" && state.isFlipped -> speechController.speak(ClozeText.fillFirstGap(card.example, card.term))
            card.format == "basic" && !state.isFlipped -> speechController.speak(card.term)
        }
    }

    fun speak(text: String, rate: Float? = null) {
        if (!speechEngineState.isReady) {
            if (speechEngineState.initializationFailed) {
                coroutineScope.launch { snackbarHostState.showSnackbar(VOICE_UNAVAILABLE_MESSAGE) }
            }
            return
        }
        speechController.speak(text, rate)
    }

    LaunchedEffect(speechEngineState.engine, state.language) {
        val textToSpeech = speechEngineState.engine
        if (speechEngineState.isReady && textToSpeech != null &&
            textToSpeech.isLanguageAvailable(studyLocale(state.language)) < TextToSpeech.LANG_AVAILABLE
        ) {
            snackbarHostState.showSnackbar(VOICE_UNAVAILABLE_MESSAGE)
        }
    }

    LaunchedEffect(state.errorMessage) {
        state.errorMessage?.let { snackbarHostState.showSnackbar(it) }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = state.deckName.ifBlank { "Estudar" },
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontSize = 21.sp,
                            fontWeight = FontWeight.SemiBold,
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Voltar") }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    scrolledContainerColor = MaterialTheme.colorScheme.background,
                ),
                actions = {
                    if (state.deckId > 0) {
                        IconButton(onClick = { showCardMenu = true }) {
                            Icon(Icons.Outlined.MoreVert, contentDescription = "Mais opções")
                        }
                        DropdownMenu(expanded = showCardMenu, onDismissRequest = { showCardMenu = false }) {
                            if (currentCard != null) {
                                DropdownMenuItem(
                                    text = { Text("Editar card") },
                                    leadingIcon = { Icon(Icons.Outlined.Edit, contentDescription = null) },
                                    onClick = { showCardMenu = false; onEditCard(currentCard.id) },
                                )
                                HorizontalDivider()
                                DropdownMenuItem(
                                    text = { Text("Voz") },
                                    leadingIcon = { Icon(Icons.Outlined.RecordVoiceOver, contentDescription = null) },
                                    onClick = { showCardMenu = false; showVoiceDialog = true },
                                )
                            }
                            DropdownMenuItem(
                                text = {
                                    Column {
                                        Text("Modo de estudo")
                                        Text(
                                            if (studyMode == StudyMode.STANDARD) "Padrão" else "Pronúncia (ouvir primeiro)",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    }
                                },
                                leadingIcon = { Icon(Icons.Outlined.School, contentDescription = null) },
                                onClick = { showCardMenu = false; showStudyModeDialog = true },
                            )
                            DropdownMenuItem(
                                enabled = studyMode == StudyMode.PRONUNCIATION,
                                text = {
                                    Column {
                                        Text("Reproduzir automaticamente")
                                        if (studyMode == StudyMode.STANDARD) {
                                            Text(
                                                "Disponível no modo Pronúncia",
                                                style = MaterialTheme.typography.bodySmall,
                                            )
                                        }
                                    }
                                },
                                leadingIcon = { Icon(Icons.Outlined.PlayCircle, contentDescription = null) },
                                trailingIcon = {
                                    Switch(
                                        checked = autoplayEnabled,
                                        enabled = studyMode == StudyMode.PRONUNCIATION,
                                        onCheckedChange = if (studyMode == StudyMode.PRONUNCIATION) { enabled ->
                                            autoplayEnabled = enabled
                                            deckPreferences.setAutoplayEnabled(state.deckId, enabled)
                                        } else null,
                                    )
                                },
                                onClick = {
                                    if (studyMode == StudyMode.PRONUNCIATION) {
                                        autoplayEnabled = !autoplayEnabled
                                        deckPreferences.setAutoplayEnabled(state.deckId, autoplayEnabled)
                                    }
                                },
                            )
                        }
                    }
                },
            )
        },
    ) { padding ->
        if (state.isLoading) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else if (state.cards.isEmpty()) {
            Column(
                modifier = Modifier.fillMaxSize().padding(padding).padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                if (state.allCards.isEmpty()) {
                    Text("Este baralho ainda não tem cards", textAlign = TextAlign.Center)
                    Button(onClick = onManageDeck, modifier = Modifier.padding(top = 16.dp)) {
                        Text("Gerenciar baralho")
                    }
                } else {
                    Text("Você terminou por hoje!", style = MaterialTheme.typography.headlineSmall, textAlign = TextAlign.Center)
                    Button(onClick = viewModel::studyAllCards, modifier = Modifier.padding(top = 20.dp)) {
                        Icon(Icons.AutoMirrored.Outlined.MenuBook, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Estudar tudo mesmo assim")
                    }
                }
            }
        } else {
            val card = if (state.isAllCardsMode) state.cards[state.currentIndex] else state.cards.first()
            val currentNumber = if (state.isAllCardsMode) {
                state.currentIndex + 1
            } else {
                (state.sessionTotal - state.cards.size + 1).coerceAtMost(state.sessionTotal)
            }
            val totalNumber = if (state.isAllCardsMode) state.cards.size else state.sessionTotal
            val progressFraction = if (totalNumber > 0) (currentNumber.toFloat() / totalNumber.toFloat()).coerceIn(0f, 1f) else 0f
            val progressPercent = (progressFraction * 100).toInt()

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = "$currentNumber de $totalNumber cartões",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(
                            text = "$progressPercent%",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    LinearProgressIndicator(
                        progress = { progressFraction },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                        strokeCap = StrokeCap.Round,
                    )
                }

                StudyCard(
                    card = card,
                    isFlipped = state.isFlipped,
                    pronunciationMode = studyMode == StudyMode.PRONUNCIATION,
                    wordRevealed = wordRevealed,
                    onClick = { if (!state.isSaving) viewModel.flipCard() },
                    onSpeak = { text -> speak(text) },
                    onSpeakSlow = { text -> speak(text, pronunciationSlowRate(speechEngineState.speechRate)) },
                    onRevealWord = { wordRevealed = true },
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(top = 12.dp),
                )

                if (state.isAllCardsMode) {
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        maxItemsInEachRow = 2,
                    ) {
                        Button(onClick = viewModel::showPrevious, enabled = state.currentIndex > 0) {
                            Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("Anterior", maxLines = 1, softWrap = false)
                        }
                        Button(onClick = viewModel::showNext, enabled = state.currentIndex < state.cards.lastIndex) {
                            Icon(Icons.AutoMirrored.Outlined.ArrowForward, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("Próximo", maxLines = 1, softWrap = false)
                        }
                    }
                } else if (state.isFlipped) {
                    StudyRatingBar(
                        card = card,
                        enabled = !state.isSaving,
                        onRate = viewModel::rateCurrentCard,
                    )
                }
            }
        }
    }

    if (showStudyModeDialog) {
        AlertDialog(
            onDismissRequest = { showStudyModeDialog = false },
            title = { Text("Modo de estudo") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    StudyMode.entries.forEach { mode ->
                        Row(
                            modifier = Modifier.fillMaxWidth().clickable {
                                studyMode = mode
                                deckPreferences.setMode(state.deckId, mode)
                                showStudyModeDialog = false
                            },
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            RadioButton(selected = studyMode == mode, onClick = {
                                studyMode = mode
                                deckPreferences.setMode(state.deckId, mode)
                                showStudyModeDialog = false
                            })
                            Text(
                                if (mode == StudyMode.STANDARD) "Padrão" else "Pronúncia (ouvir primeiro)",
                                modifier = Modifier.weight(1f),
                            )
                            if (studyMode == mode) Icon(Icons.Outlined.Check, contentDescription = "Selecionado")
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showStudyModeDialog = false }) { Text("Fechar") } },
        )
    }

    if (showVoiceDialog) {
        VoiceSettingsDialog(
            speechState = speechEngineState,
            onSelectVoice = speechController::selectVoice,
            onTestVoice = speechController::testVoice,
            onSelectRate = speechController::setSpeechRate,
            onOpenAndroidSettings = {
                context.startActivity(Intent("com.android.settings.TTS_SETTINGS"))
            },
            onDismiss = { showVoiceDialog = false },
        )
    }
}

@Composable
private fun VoiceSettingsDialog(
    speechState: SpeechEngineState,
    onSelectVoice: (Voice) -> Unit,
    onTestVoice: (Voice) -> Unit,
    onSelectRate: (Float) -> Unit,
    onOpenAndroidSettings: () -> Unit,
    onDismiss: () -> Unit,
) {
    val speeds = listOf(0.75f, 1f, 1.25f)
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Voz") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                if (speechState.voices.isEmpty()) {
                    Text("Nenhuma voz disponível para este idioma.")
                } else {
                    speechState.voices.forEachIndexed { index, voice ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(
                                selected = voice.name == speechState.selectedVoiceName,
                                onClick = { onSelectVoice(voice) },
                            )
                            Column(Modifier.weight(1f)) {
                                Text(
                                    "Voz ${index + 1}${if (voice.isNetworkConnectionRequired) " (online)" else ""}",
                                )
                                Text(voice.name, style = MaterialTheme.typography.bodySmall)
                            }
                            TextButton(onClick = { onTestVoice(voice) }) { Text("Testar") }
                        }
                    }
                }
                Text("Velocidade", style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(top = 12.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    speeds.forEach { speed ->
                        TextButton(onClick = { onSelectRate(speed) }) {
                            Text(if (speechState.speechRate == speed) "✓ ${speed}x" else "${speed}x", maxLines = 1, softWrap = false)
                        }
                    }
                }
                TextButton(onClick = onOpenAndroidSettings) {
                    Text("Configurações de voz do Android")
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Fechar") } },
    )
}

@Composable
@OptIn(ExperimentalLayoutApi::class)
private fun StudyCard(
    card: FlashCard,
    isFlipped: Boolean,
    pronunciationMode: Boolean,
    wordRevealed: Boolean,
    onClick: () -> Unit,
    onSpeak: (String) -> Unit,
    onSpeakSlow: (String) -> Unit,
    onRevealWord: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val rotation by animateFloatAsState(if (isFlipped) 180f else 0f, label = "cardFlip")

    Box(
        modifier = modifier,
        contentAlignment = Alignment.TopCenter,
    ) {
        // Camada 2 (fundo distante):
        Surface(
            modifier = Modifier
                .matchParentSize()
                .padding(horizontal = 24.dp)
                .offset(y = (-12).dp),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.45f),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f)),
        ) {}

        // Camada 1 (fundo intermediário):
        Surface(
            modifier = Modifier
                .matchParentSize()
                .padding(horizontal = 12.dp)
                .offset(y = (-6).dp),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.75f),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
        ) {}

        // Cartão Principal:
        Card(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer { rotationY = rotation }
                .clickable(onClick = onClick),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface,
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween,
            ) {
                if (card.format == "cloze") {
                    val fullExample = ClozeText.fillFirstGap(card.example, card.term)
                    if (rotation < 90f) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.surfaceContainerHighest,
                            ) {
                                Text(
                                    "Lacuna",
                                    style = MaterialTheme.typography.labelMedium,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                )
                            }
                        }
                        Column(
                            modifier = Modifier.fillMaxWidth().weight(1f, fill = false).padding(vertical = 16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            Text(
                                clozeAnnotatedText(
                                    card.example,
                                    "___",
                                    MaterialTheme.colorScheme.primary,
                                    highlightedFontSize = MaterialTheme.typography.headlineSmall.fontSize,
                                ),
                                style = MaterialTheme.typography.bodyLarge,
                                textAlign = TextAlign.Center,
                            )
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.surfaceContainerLow,
                            ) {
                                Text(
                                    card.exampleTranslation,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                                )
                            }
                        }
                        Text(
                            "Toque para virar",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 8.dp),
                        )
                    } else {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .graphicsLayer { rotationY = 180f },
                            verticalArrangement = Arrangement.spacedBy(14.dp),
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = MaterialTheme.extendedColors.successContainer,
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    ) {
                                        Icon(
                                            Icons.Outlined.Check,
                                            contentDescription = null,
                                            tint = MaterialTheme.extendedColors.onSuccessContainer,
                                            modifier = Modifier.size(16.dp),
                                        )
                                        Text(
                                            "Resposta revelada",
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.extendedColors.onSuccessContainer,
                                        )
                                    }
                                }
                                Surface(
                                    shape = CircleShape,
                                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                                    onClick = { onSpeak(fullExample) },
                                ) {
                                    Icon(
                                        Icons.AutoMirrored.Outlined.VolumeUp,
                                        contentDescription = "Ouvir frase completa",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.padding(8.dp).size(20.dp),
                                    )
                                }
                            }

                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                Text(
                                    clozeAnnotatedText(
                                        fullExample,
                                        card.term,
                                        MaterialTheme.colorScheme.primary,
                                        startOverride = card.example.indexOf("___").takeIf { it >= 0 },
                                    ),
                                    style = MaterialTheme.typography.headlineSmall,
                                    textAlign = TextAlign.Center,
                                )
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = MaterialTheme.extendedColors.successContainer.copy(alpha = 0.6f),
                                ) {
                                    Text(
                                        card.termTranslation,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.extendedColors.onSuccessContainer,
                                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                                    )
                                }
                            }

                            HorizontalDivider(
                                modifier = Modifier.padding(vertical = 4.dp),
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                            )

                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                ) {
                                    Icon(
                                        Icons.Outlined.FormatQuote,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(18.dp),
                                    )
                                    Text(
                                        "Tradução",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary,
                                    )
                                }
                                Text(
                                    card.exampleTranslation,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }

                            card.note?.takeIf(String::isNotBlank)?.let { note ->
                                Text(
                                    note,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.fillMaxWidth(),
                                )
                            }

                            card.ipa?.takeIf(String::isNotBlank)?.let { ipa ->
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    ) {
                                        Icon(
                                            Icons.Outlined.Headphones,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onSurface,
                                            modifier = Modifier.size(18.dp),
                                        )
                                        Text("Pronúncia", style = MaterialTheme.typography.bodyMedium)
                                    }
                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                                    ) {
                                        Text(
                                            ipa,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                        )
                                    }
                                }
                            }
                        }
                    }
                } else if (rotation < 90f && pronunciationMode) {
                    // Topo: Badge de Pronúncia
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Start,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.primaryContainer,
                        ) {
                            Text(
                                text = "Pronúncia",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 12.sp,
                                ),
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            )
                        }
                    }

                    // Centro: Termo revelado, Botão 96x96dp e Pílulas
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f, fill = false)
                            .padding(vertical = 16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(20.dp),
                    ) {
                        if (wordRevealed) {
                            Text(
                                text = card.term,
                                style = MaterialTheme.typography.headlineMedium.copy(
                                    fontSize = 28.sp,
                                    fontWeight = FontWeight.Bold,
                                ),
                                color = MaterialTheme.colorScheme.onSurface,
                                textAlign = TextAlign.Center,
                            )
                        }

                        // Botão redondo grande de áudio (96x96dp)
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primary,
                            onClick = { onSpeak(card.term) },
                            modifier = Modifier.size(96.dp),
                            shadowElevation = 4.dp,
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Outlined.VolumeUp,
                                    contentDescription = "Ouvir termo",
                                    tint = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier.size(40.dp),
                                )
                            }
                        }

                        // Pílulas: Ouvir de novo e Ouvir devagar
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            // Ouvir de novo (.pill)
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                                onClick = { onSpeak(card.term) },
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.Replay,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(16.dp),
                                    )
                                    Text(
                                        text = "Ouvir de novo",
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Medium,
                                        ),
                                        color = MaterialTheme.colorScheme.onSurface,
                                    )
                                }
                            }

                            // Ouvir devagar (.pill)
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                                onClick = { onSpeakSlow(card.term) },
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.SlowMotionVideo,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(16.dp),
                                    )
                                    Text(
                                        text = "Ouvir devagar",
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Medium,
                                        ),
                                        color = MaterialTheme.colorScheme.onSurface,
                                    )
                                }
                            }
                        }

                        // Pílula Mostrar palavra (.pill com contorno sutil)
                        if (!wordRevealed) {
                            Surface(
                                shape = CircleShape,
                                color = Color.Transparent,
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                                onClick = onRevealWord,
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.Visibility,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(16.dp),
                                    )
                                    Text(
                                        text = "Mostrar palavra",
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Medium,
                                        ),
                                        color = MaterialTheme.colorScheme.onSurface,
                                    )
                                }
                            }
                        }
                    }

                    // Base: Dica (.hint com microfone)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Mic,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                            modifier = Modifier.size(16.dp),
                        )
                        Text(
                            text = "Repita em voz alta e toque no card para conferir",
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                            textAlign = TextAlign.Center,
                        )
                    }
                } else if (rotation < 90f) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceContainerHighest,
                        ) {
                            Text(
                                if (card.type == "word") "Palavra" else "Expressão",
                                style = MaterialTheme.typography.labelMedium,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            )
                        }
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                            onClick = { onSpeak(card.term) },
                        ) {
                            Icon(
                                Icons.AutoMirrored.Outlined.VolumeUp,
                                contentDescription = "Ouvir",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(8.dp).size(20.dp),
                            )
                        }
                    }
                    Column(
                        modifier = Modifier.fillMaxWidth().weight(1f, fill = false).padding(vertical = 24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                    ) {
                        Text(
                            card.term,
                            style = MaterialTheme.typography.headlineLarge,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                        )
                    }
                    Text(
                        "Toque no cartão para conferir",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                    )
                } else {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .graphicsLayer { rotationY = 180f },
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.extendedColors.successContainer,
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                ) {
                                    Icon(
                                        Icons.Outlined.Check,
                                        contentDescription = null,
                                        tint = MaterialTheme.extendedColors.onSuccessContainer,
                                        modifier = Modifier.size(16.dp),
                                    )
                                    Text(
                                        "Resposta revelada",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.extendedColors.onSuccessContainer,
                                    )
                                }
                            }
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                                onClick = { onSpeak(card.term) },
                            ) {
                                Icon(
                                    Icons.AutoMirrored.Outlined.VolumeUp,
                                    contentDescription = "Ouvir",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(8.dp).size(20.dp),
                                )
                            }
                        }

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onSpeak(card.term) }
                                .padding(vertical = 8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            Text(
                                card.term,
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center,
                            )
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.extendedColors.successContainer.copy(alpha = 0.6f),
                            ) {
                                Text(
                                    card.termTranslation,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.extendedColors.onSuccessContainer,
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                                )
                            }
                        }

                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 4.dp),
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                        )

                        if (card.example.isNotBlank()) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onSpeak(card.example) }
                                    .padding(vertical = 4.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp),
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                ) {
                                    Icon(
                                        Icons.Outlined.FormatQuote,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp),
                                    )
                                    Text(
                                        "Exemplo",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary,
                                    )
                                    Spacer(Modifier.weight(1f))
                                    Surface(
                                        shape = CircleShape,
                                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                                        onClick = { onSpeak(card.example) },
                                    ) {
                                        Icon(
                                            Icons.AutoMirrored.Outlined.VolumeUp,
                                            contentDescription = "Ouvir frase de exemplo",
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.padding(6.dp).size(18.dp),
                                        )
                                    }
                                }
                                Text(
                                    card.example,
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Bold,
                                )
                                if (card.exampleTranslation.isNotBlank()) {
                                    Text(
                                        card.exampleTranslation,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                        }

                        card.note?.takeIf(String::isNotBlank)?.let { note ->
                            Text(
                                note,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                            )
                        }

                        card.ipa?.takeIf(String::isNotBlank)?.let { ipa ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onSpeak(card.term) }
                                    .padding(top = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                ) {
                                    Icon(
                                        Icons.Outlined.Headphones,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.size(20.dp),
                                    )
                                    Text(
                                        "Pronúncia",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Medium,
                                    )
                                }
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                                ) {
                                    Text(
                                        ipa,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun clozeAnnotatedText(
    text: String,
    highlighted: String,
    color: androidx.compose.ui.graphics.Color,
    startOverride: Int? = null,
    highlightedFontSize: TextUnit = TextUnit.Unspecified,
): AnnotatedString {
    val start = startOverride ?: text.indexOf(highlighted, ignoreCase = true)
    if (start < 0) return AnnotatedString(text)
    val end = start + highlighted.length
    return buildAnnotatedString {
        append(text.substring(0, start))
        withStyle(SpanStyle(color = color, fontWeight = FontWeight.Bold, fontSize = highlightedFontSize)) {
            append(text.substring(start, end))
        }
        append(text.substring(end))
    }
}

private const val VOICE_UNAVAILABLE_MESSAGE =
    "Voz não disponível. Instale o idioma nas configurações de texto para fala do Android."

private const val TTS_LOG_TAG = "FlashLangTTS"

private data class SpeechEngineState(
    val engine: TextToSpeech? = null,
    val isReady: Boolean = false,
    val initializationFailed: Boolean = false,
    val voices: List<Voice> = emptyList(),
    val selectedVoiceName: String? = null,
    val speechRate: Float = 1f,
)

private fun studyLocale(language: String): Locale = when (language) {
    "es" -> Locale("es", "ES")
    else -> Locale.US
}

private class SpeechEngineController(
    private val context: Context,
    private val onStateChanged: (SpeechEngineState) -> Unit,
    private val onNetworkVoiceFailure: () -> Unit,
) {
    private var engine: TextToSpeech? = null
    private var generation = 0
    private var language = "en"
    private var availableVoices: List<Voice> = emptyList()
    private var selectedVoice: Voice? = null
    private var speechRate = 1f
    private val preferences = context.getSharedPreferences("flashlang_tts_preferences", Context.MODE_PRIVATE)

    fun initialize(language: String) {
        this.language = language
        speechRate = preferences.getFloat(rateKey(), 1f)
        initializeEngine()
    }

    fun selectVoice(voice: Voice) {
        if (availableVoices.none { it.name == voice.name }) return
        selectedVoice = voice
        preferences.edit().putString(voiceKey(), voice.name).apply()
        engine?.setVoice(voice)
        publishReadyState()
    }

    fun setSpeechRate(rate: Float) {
        if (rate !in listOf(0.75f, 1f, 1.25f)) return
        speechRate = rate
        preferences.edit().putFloat(rateKey(), rate).apply()
        engine?.setSpeechRate(rate)
        publishReadyState()
    }

    fun speak(text: String, rate: Float? = null) {
        val textToSpeech = engine ?: return
        val locale = studyLocale(language)
        if (textToSpeech.isLanguageAvailable(locale) < TextToSpeech.LANG_AVAILABLE) return
        selectedVoice?.let(textToSpeech::setVoice) ?: textToSpeech.setLanguage(locale)
        textToSpeech.setSpeechRate(rate ?: speechRate)
        textToSpeech.speak(text, TextToSpeech.QUEUE_FLUSH, null, "flashlang-study")
    }

    fun testVoice(voice: Voice) {
        val textToSpeech = engine ?: return
        val previousVoice = selectedVoice
        textToSpeech.setVoice(voice)
        textToSpeech.setSpeechRate(speechRate)
        val sample = if (language == "es") "Hola, esta es una muestra de voz." else "Hello, this is a sample voice."
        textToSpeech.speak(sample, TextToSpeech.QUEUE_FLUSH, null, "flashlang-voice-test")
        previousVoice?.let(textToSpeech::setVoice) ?: textToSpeech.setLanguage(studyLocale(language))
    }

    fun shutdown() {
        generation++
        engine?.stop()
        engine?.shutdown()
        engine = null
    }

    fun stop() {
        engine?.stop()
    }

    private fun initializeEngine() {
        val requestGeneration = ++generation
        lateinit var candidate: TextToSpeech
        val listener = TextToSpeech.OnInitListener { status ->
            if (requestGeneration != generation) {
                candidate.shutdown()
            } else if (status == TextToSpeech.SUCCESS) {
                engine = candidate
                val locale = studyLocale(language)
                availableVoices = candidate.voices.orEmpty()
                    .filter { it.locale.language == locale.language && it.locale.country == locale.country }
                val savedVoiceName = preferences.getString(voiceKey(), null)
                selectedVoice = availableVoices.firstOrNull { it.name == savedVoiceName }
                    ?: availableVoices.sortedWith(
                        compareBy<Voice> { it.isNetworkConnectionRequired }
                            .thenByDescending { it.quality }
                            .thenBy { it.name.lowercase(Locale.ROOT) },
                    ).firstOrNull()
                selectedVoice?.let(candidate::setVoice) ?: candidate.setLanguage(locale)
                candidate.setSpeechRate(speechRate)
                if (savedVoiceName != null && selectedVoice?.name != savedVoiceName) {
                    preferences.edit().remove(voiceKey()).apply()
                }
                if (BuildConfig.DEBUG) Log.i(TTS_LOG_TAG, "tts.defaultEngine=${candidate.defaultEngine}")
                candidate.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                    override fun onStart(utteranceId: String) = Unit
                    override fun onDone(utteranceId: String) = Unit
                    override fun onError(utteranceId: String) {
                        fallbackFromFailedOnlineVoice(utteranceId)
                    }
                    override fun onError(utteranceId: String, errorCode: Int) {
                        if (errorCode == TextToSpeech.ERROR_NETWORK) {
                            fallbackFromFailedOnlineVoice(utteranceId)
                        }
                    }
                })
                publishReadyState()
            } else {
                candidate.shutdown()
                if (engine === candidate) engine = null
                onStateChanged(SpeechEngineState(initializationFailed = true))
            }
        }

        try {
            candidate = TextToSpeech(context, listener)
            engine = candidate
        } catch (_: Exception) {
            onStateChanged(SpeechEngineState(initializationFailed = true))
        }
    }

    private fun fallbackFromFailedOnlineVoice(utteranceId: String) {
        if (utteranceId != "flashlang-study" && utteranceId != "flashlang-voice-test") return
        val failedVoice = selectedVoice ?: return
        if (!failedVoice.isNetworkConnectionRequired) return
        val offlineVoice = availableVoices
            .filterNot { it.isNetworkConnectionRequired }
            .sortedWith(
                compareByDescending<Voice> { it.quality }
                    .thenBy { it.name.lowercase(Locale.ROOT) },
            )
            .firstOrNull()
            ?: return
        selectedVoice = offlineVoice
        preferences.edit().putString(voiceKey(), offlineVoice.name).apply()
        engine?.setVoice(offlineVoice)
        engine?.setSpeechRate(speechRate)
        publishReadyState()
        onNetworkVoiceFailure()
    }

    private fun publishReadyState() {
        onStateChanged(
            SpeechEngineState(
                engine = engine,
                isReady = engine != null,
                voices = availableVoices.sortedWith(
                    compareBy<Voice> { it.name != selectedVoice?.name }
                        .thenBy { it.name.lowercase(Locale.ROOT) },
                ),
                selectedVoiceName = selectedVoice?.name,
                speechRate = speechRate,
            ),
        )
    }

    private fun voiceKey() = "voice_$language"
    private fun rateKey() = "rate_$language"
}
