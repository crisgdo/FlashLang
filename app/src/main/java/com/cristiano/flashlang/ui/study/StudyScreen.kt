package com.cristiano.flashlang.ui.study

import android.speech.tts.TextToSpeech
import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.automirrored.outlined.VolumeUp
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import com.cristiano.flashlang.data.model.Card as FlashCard
import com.cristiano.flashlang.data.scheduler.ReviewRating
import java.util.Locale
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudyScreen(viewModel: StudyViewModel, onBack: () -> Unit, onEditCard: (Long) -> Unit) {
    val state by viewModel.state.collectAsState()
    var showCardMenu by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    var ttsReady by remember { mutableStateOf(false) }
    var ttsInitializationFailed by remember { mutableStateOf(false) }
    val textToSpeech = remember(context) {
        TextToSpeech(context.applicationContext) { status ->
            ttsReady = status == TextToSpeech.SUCCESS
            ttsInitializationFailed = status != TextToSpeech.SUCCESS
        }
    }
    DisposableEffect(textToSpeech) {
        onDispose {
            textToSpeech.stop()
            textToSpeech.shutdown()
        }
    }

    fun speak(text: String) {
        if (!ttsReady) {
            if (ttsInitializationFailed) {
                coroutineScope.launch { snackbarHostState.showSnackbar(VOICE_UNAVAILABLE_MESSAGE) }
            }
            return
        }
        val locale = when (state.language) {
            "es" -> Locale("es", "ES")
            else -> Locale.US
        }
        val languageStatus = textToSpeech.setLanguage(locale)
        if (languageStatus == TextToSpeech.LANG_MISSING_DATA || languageStatus == TextToSpeech.LANG_NOT_SUPPORTED) {
            coroutineScope.launch { snackbarHostState.showSnackbar(VOICE_UNAVAILABLE_MESSAGE) }
        } else {
            textToSpeech.speak(text, TextToSpeech.QUEUE_FLUSH, null, "flashlang-study")
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
                    Text("Este baralho ainda não tem cards.", textAlign = TextAlign.Center)
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
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Button(onClick = viewModel::showPrevious, enabled = state.currentIndex > 0, modifier = Modifier.weight(1f)) {
                            Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("Anterior")
                        }
                        Button(onClick = viewModel::showNext, enabled = state.currentIndex < state.cards.lastIndex, modifier = Modifier.weight(1f)) {
                            Icon(Icons.AutoMirrored.Outlined.ArrowForward, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("Próximo")
                        }
                    }
                } else if (state.isFlipped) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            RatingButton("Errei", ReviewRating.AGAIN, viewModel, state.isSaving, Modifier.weight(1f))
                            RatingButton("Difícil", ReviewRating.HARD, viewModel, state.isSaving, Modifier.weight(1f))
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            RatingButton("Bom", ReviewRating.GOOD, viewModel, state.isSaving, Modifier.weight(1f))
                            RatingButton("Fácil", ReviewRating.EASY, viewModel, state.isSaving, Modifier.weight(1f))
                        }
                    }
                }
            }
        }
    }
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
        Text(label)
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
            if (rotation < 90f) {
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

private const val VOICE_UNAVAILABLE_MESSAGE =
    "Voz não disponível. Instale o idioma nas configurações de texto para fala do Android."
