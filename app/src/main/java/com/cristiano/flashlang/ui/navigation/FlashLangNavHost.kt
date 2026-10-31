package com.cristiano.flashlang.ui.navigation

import android.content.Intent
import android.widget.Toast
import androidx.core.content.FileProvider
import androidx.compose.ui.platform.LocalContext
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.NavType
import androidx.navigation.navArgument
import com.cristiano.flashlang.data.repository.DeckRepository
import com.cristiano.flashlang.data.exporter.DeckExporter
import com.cristiano.flashlang.ui.home.HomeScreen
import com.cristiano.flashlang.ui.home.HomeViewModel
import com.cristiano.flashlang.ui.importdeck.ImportScreen
import com.cristiano.flashlang.ui.importdeck.ImportViewModel
import com.cristiano.flashlang.ui.study.StudyScreen
import com.cristiano.flashlang.ui.study.StudyViewModel
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private object Routes {
    const val HOME = "home"
    const val IMPORT = "import"
    const val STUDY = "study/{deckId}"
}

@Composable
fun FlashLangNavHost(repository: DeckRepository, navController: NavHostController = rememberNavController()) {
    NavHost(navController = navController, startDestination = Routes.HOME) {
        composable(Routes.HOME) {
            val homeViewModel: HomeViewModel = viewModel(factory = HomeViewModel.Factory(repository))
            val context = LocalContext.current
            val coroutineScope = rememberCoroutineScope()
            HomeScreen(
                homeViewModel,
                onImportClick = { navController.navigate(Routes.IMPORT) },
                onDeckClick = { deckId -> navController.navigate("study/$deckId") },
                onExportClick = { deckId ->
                    coroutineScope.launch {
                        try {
                            val deck = repository.getDecks().first { it.id == deckId }
                            val json = DeckExporter.toJson(deck, repository.getCards(deckId))
                            val file = withContext(Dispatchers.IO) {
                                val directory = File(context.cacheDir, "exports").apply { mkdirs() }
                                val safeName = deck.name.replace(Regex("[^A-Za-z0-9._-]"), "_").ifBlank { "baralho" }
                                File(directory, "$safeName-${deck.id}.json").apply { writeText(json) }
                            }
                            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
                            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                type = "application/json"
                                putExtra(Intent.EXTRA_STREAM, uri)
                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            }
                            context.startActivity(Intent.createChooser(shareIntent, "Compartilhar baralho"))
                        } catch (_: Exception) {
                            Toast.makeText(context, "Não foi possível exportar o baralho.", Toast.LENGTH_LONG).show()
                        }
                    }
                },
            )
        }
        composable(Routes.IMPORT) {
            val importViewModel: ImportViewModel = viewModel(factory = ImportViewModel.Factory(repository))
            ImportScreen(
                viewModel = importViewModel,
                onImportSuccess = { navController.popBackStack() },
                onBack = { navController.popBackStack() },
            )
        }
        composable(
            route = Routes.STUDY,
            arguments = listOf(navArgument("deckId") { type = NavType.LongType }),
        ) { entry ->
            val deckId = requireNotNull(entry.arguments).getLong("deckId")
            val studyViewModel: StudyViewModel = viewModel(factory = StudyViewModel.Factory(repository, deckId))
            StudyScreen(studyViewModel, onBack = { navController.popBackStack() })
        }
    }
}
