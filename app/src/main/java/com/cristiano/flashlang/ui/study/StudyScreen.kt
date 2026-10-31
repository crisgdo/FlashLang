package com.cristiano.flashlang.ui.study

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.cristiano.flashlang.data.model.Card as FlashCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudyScreen(viewModel: StudyViewModel, onBack: () -> Unit) {
    val state by viewModel.state.collectAsState()
    Scaffold(
        topBar = { TopAppBar(title = { Text("Estudar") }, navigationIcon = { TextButton(onClick = onBack) { Text("Voltar") } }) },
    ) { padding ->
        if (state.cards.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(padding).padding(24.dp), contentAlignment = Alignment.Center) {
                Text("Este baralho ainda não tem cards.", textAlign = TextAlign.Center)
            }
        } else {
            val card = state.cards[state.currentIndex]
            Column(
                modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 20.dp, vertical = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(20.dp),
            ) {
                Text("${state.currentIndex + 1} / ${state.cards.size}", style = MaterialTheme.typography.titleMedium)
                StudyCard(
                    card = card,
                    isFlipped = state.isFlipped,
                    onClick = viewModel::flipCard,
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                )
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Button(onClick = viewModel::showPrevious, enabled = state.currentIndex > 0, modifier = Modifier.weight(1f)) {
                        Text("Anterior")
                    }
                    Button(onClick = viewModel::showNext, enabled = state.currentIndex < state.cards.lastIndex, modifier = Modifier.weight(1f)) {
                        Text("Próximo")
                    }
                }
            }
        }
    }
}

@Composable
private fun StudyCard(card: FlashCard, isFlipped: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
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
                Text(card.term, style = MaterialTheme.typography.headlineMedium, textAlign = TextAlign.Center)
                Text("Toque para virar", style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 16.dp))
            } else {
                Column(
                    modifier = Modifier.graphicsLayer { rotationY = 180f },
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    Text(card.termTranslation, style = MaterialTheme.typography.headlineSmall, textAlign = TextAlign.Center)
                    Text(card.example, style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.Center)
                    Text(card.exampleTranslation, style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center)
                }
            }
        }
    }
}
