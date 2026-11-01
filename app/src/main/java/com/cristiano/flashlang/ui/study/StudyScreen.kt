package com.cristiano.flashlang.ui.study

import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.speech.tts.Voice
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.IconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.automirrored.outlined.VolumeUp
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.RecordVoiceOver
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import com.cristiano.flashlang.data.model.Card as FlashCard
import com.cristiano.flashlang.data.importer.ClozeText
import com.cristiano.flashlang.data.scheduler.ReviewRating
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
    val state by viewModel.state.collectAsState()
    var showCardMenu by remember { mutableStateOf(false) }
    var showVoiceDialog by remember { mutableStateOf(false) }
    val context = LocalContext.current
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

    fun speak(text: String) {
        if (!speechEngineState.isReady) {
            if (speechEngineState.initializationFailed) {
                coroutineScope.launch { snackbarHostState.showSnackbar(VOICE_UNAVAILABLE_MESSAGE) }
            }
            return
        }
        speechController.speak(text)
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
                title = { Text("Estudar") },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Voltar") }
                },
                actions = {
                    val currentCard = state.cards.getOrNull(if (state.isAllCardsMode) state.currentIndex else 0)
                    if (currentCard != null) {
                        IconButton(onClick = { showCardMenu = true }) {
                            Icon(Icons.Outlined.MoreVert, contentDescription = "Mais opções")
                        }
                        DropdownMenu(expanded = showCardMenu, onDismissRequest = { showCardMenu = false }) {
                            DropdownMenuItem(
                                text = { Text("Editar card") },
                                leadingIcon = { Icon(Icons.Outlined.Edit, contentDescription = null) },
                                onClick = { showCardMenu = false; onEditCard(currentCard.id) },
                            )
                            DropdownMenuItem(
                                text = { Text("Voz") },
                                leadingIcon = { Icon(Icons.Outlined.RecordVoiceOver, contentDescription = null) },
                                onClick = { showCardMenu = false; showVoiceDialog = true },
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
            val progress = if (state.isAllCardsMode) {
                state.currentIndex + 1
            } else {
                (state.sessionTotal - state.cards.size + 1).coerceAtMost(state.sessionTotal)
            }
            Column(
                modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 20.dp, vertical = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(20.dp),
            ) {
                Text("$progress / ${if (state.isAllCardsMode) state.cards.size else state.sessionTotal}", style = MaterialTheme.typography.titleMedium)
                StudyCard(
                    card = card,
                    isFlipped = state.isFlipped,
                    onClick = { if (!state.isSaving) viewModel.flipCard() },
                    onSpeak = { text -> speak(text) },
                    modifier = Modifier.weight(1f).fillMaxWidth(),
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
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        maxItemsInEachRow = 2,
                    ) {
                        RatingButton("Errei", ReviewRating.AGAIN, viewModel, state.isSaving)
                        RatingButton("Difícil", ReviewRating.HARD, viewModel, state.isSaving)
                        RatingButton("Bom", ReviewRating.GOOD, viewModel, state.isSaving)
                        RatingButton("Fácil", ReviewRating.EASY, viewModel, state.isSaving)
                    }
                }
            }
        }
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
private fun RatingButton(
    label: String,
    rating: ReviewRating,
    viewModel: StudyViewModel,
    isSaving: Boolean,
    modifier: Modifier = Modifier,
) {
    Button(onClick = { viewModel.rateCurrentCard(rating) }, enabled = !isSaving, modifier = modifier) {
        Text(label, maxLines = 1, softWrap = false)
    }
}

@Composable
private fun StudyCard(card: FlashCard, isFlipped: Boolean, onClick: () -> Unit, onSpeak: (String) -> Unit, modifier: Modifier = Modifier) {
    val rotation by animateFloatAsState(if (isFlipped) 180f else 0f, label = "cardFlip")
    Card(
        modifier = modifier.graphicsLayer { rotationY = rotation }.clickable(onClick = onClick),
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            if (card.format == "cloze") {
                val fullExample = ClozeText.fillFirstGap(card.example, card.term)
                if (rotation < 90f) {
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
                    Text(
                        card.exampleTranslation,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                    )
                    Text("Toque para virar", style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 16.dp))
                } else {
                    Column(
                        modifier = Modifier.graphicsLayer { rotationY = 180f },
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                clozeAnnotatedText(
                                    fullExample,
                                    card.term,
                                    MaterialTheme.colorScheme.primary,
                                    startOverride = card.example.indexOf("___").takeIf { it >= 0 },
                                ),
                                style = MaterialTheme.typography.headlineSmall,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.weight(1f),
                            )
                            IconButton(
                                onClick = { onSpeak(fullExample) },
                                modifier = Modifier.semantics { contentDescription = "Ouvir frase completa" },
                            ) { Icon(Icons.AutoMirrored.Outlined.VolumeUp, contentDescription = "Ouvir pronúncia") }
                        }
                        Text(card.termTranslation, style = MaterialTheme.typography.titleLarge, textAlign = TextAlign.Center)
                        card.ipa?.takeIf(String::isNotBlank)?.let { ipa ->
                            Text(ipa, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
                        }
                        card.note?.takeIf(String::isNotBlank)?.let { note ->
                            Text(note, style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center)
                        }
                    }
                }
            } else if (rotation < 90f) {
                Text(if (card.type == "word") "Palavra" else "Expressão", style = MaterialTheme.typography.labelLarge)
                IconButton(
                    onClick = { onSpeak(card.term) },
                    modifier = Modifier.semantics { contentDescription = "Ouvir ${card.term}" },
                ) { Icon(Icons.AutoMirrored.Outlined.VolumeUp, contentDescription = "Ouvir pronúncia") }
                Text(card.term, style = MaterialTheme.typography.headlineMedium, textAlign = TextAlign.Center)
                Text("Toque para virar", style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 16.dp))
            } else {
                Column(
                    modifier = Modifier.graphicsLayer { rotationY = 180f },
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    Text(card.termTranslation, style = MaterialTheme.typography.headlineSmall, textAlign = TextAlign.Center)
                    card.ipa?.takeIf(String::isNotBlank)?.let { ipa ->
                        Text(
                            ipa,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                        )
                    }
                    card.note?.takeIf(String::isNotBlank)?.let { note ->
                        Text(
                            note,
                            style = MaterialTheme.typography.bodySmall,
                            textAlign = TextAlign.Center,
                        )
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(card.example, style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.Center, modifier = Modifier.weight(1f))
                        IconButton(
                            onClick = { onSpeak(card.example) },
                            modifier = Modifier.semantics { contentDescription = "Ouvir frase de exemplo" },
                        ) { Icon(Icons.AutoMirrored.Outlined.VolumeUp, contentDescription = "Ouvir pronúncia") }
                    }
                    Text(card.exampleTranslation, style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center)
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

    fun speak(text: String) {
        val textToSpeech = engine ?: return
        val locale = studyLocale(language)
        if (textToSpeech.isLanguageAvailable(locale) < TextToSpeech.LANG_AVAILABLE) return
        selectedVoice?.let(textToSpeech::setVoice) ?: textToSpeech.setLanguage(locale)
        textToSpeech.setSpeechRate(speechRate)
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
