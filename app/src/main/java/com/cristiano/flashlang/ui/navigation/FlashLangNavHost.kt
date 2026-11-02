package com.cristiano.flashlang.ui.navigation

import android.widget.Toast
import androidx.compose.ui.platform.LocalContext
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.NavType
import androidx.navigation.navArgument
import com.cristiano.flashlang.data.repository.DeckRepository
import com.cristiano.flashlang.data.repository.UserPromptRepository
import com.cristiano.flashlang.data.local.AppDatabase
import com.cristiano.flashlang.data.backup.BackupService
import com.cristiano.flashlang.data.backup.RoomBackupDataStore
import com.cristiano.flashlang.data.model.UserPrompt
import com.cristiano.flashlang.data.exporter.DeckExporter
import com.cristiano.flashlang.data.exporter.writeShareableExport
import com.cristiano.flashlang.data.exporter.createJsonShareChooser
import com.cristiano.flashlang.ui.home.HomeScreen
import com.cristiano.flashlang.ui.home.HomeViewModel
import com.cristiano.flashlang.ui.backup.BackupScreen
import com.cristiano.flashlang.ui.backup.BackupViewModel
import com.cristiano.flashlang.ui.importdeck.ImportScreen
import com.cristiano.flashlang.ui.importdeck.ImportViewModel
import com.cristiano.flashlang.ui.study.StudyScreen
import com.cristiano.flashlang.ui.study.StudyViewModel
import com.cristiano.flashlang.ui.manage.ManageCardsScreen
import com.cristiano.flashlang.ui.manage.ManageCardsViewModel
import com.cristiano.flashlang.ui.manage.CardEditorScreen
import com.cristiano.flashlang.ui.manage.CardEditorViewModel
import com.cristiano.flashlang.data.prompts.PromptTemplates
import com.cristiano.flashlang.data.prompts.toPromptTemplate
import com.cristiano.flashlang.ui.prompts.PromptsScreen
import com.cristiano.flashlang.ui.prompts.PromptDetailScreen
import com.cristiano.flashlang.ui.prompts.UserPromptEditorScreen
import com.cristiano.flashlang.ui.prompts.UserPromptsViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private object Routes {
    const val HOME = "home"
    const val BACKUP = "backup"
    const val IMPORT = "import"
    const val PROMPTS = "prompts"
    const val PROMPT_DETAIL = "prompt/{promptId}"
    const val USER_PROMPT_DETAIL = "myPrompt/detail/{promptId}"
    const val NEW_USER_PROMPT = "myPrompt/new"
    const val EDIT_USER_PROMPT = "myPrompt/edit/{promptId}"
    const val STUDY = "study/{deckId}"
    const val MANAGE = "manage/{deckId}"
    const val EDIT_CARD = "editCard/{deckId}/{cardId}"
}

@Composable
fun FlashLangNavHost(
    repository: DeckRepository,
    userPromptRepository: UserPromptRepository,
    database: AppDatabase,
    navController: NavHostController = rememberNavController(),
) {
    NavHost(navController = navController, startDestination = Routes.HOME) {
        composable(Routes.HOME) {
            val homeViewModel: HomeViewModel = viewModel(factory = HomeViewModel.Factory(repository))
            val context = LocalContext.current
            val coroutineScope = rememberCoroutineScope()
            HomeScreen(
                homeViewModel,
                onImportClick = { navController.navigate(Routes.IMPORT) },
                onPromptsClick = { navController.navigate(Routes.PROMPTS) },
                onBackupClick = { navController.navigate(Routes.BACKUP) },
                onDeckClick = { deckId -> navController.navigate("study/$deckId") },
                onManageClick = { deckId -> navController.navigate("manage/$deckId") },
                onExportClick = { deckId ->
                    coroutineScope.launch {
                        try {
                            val deck = repository.getDecks().first { it.id == deckId }
                            val json = DeckExporter.toJson(deck, repository.getCards(deckId))
                            val safeName = deck.name.replace(Regex("[^A-Za-z0-9._-]"), "_").ifBlank { "baralho" }
                            val fileName = "$safeName-${deck.id}.json"
                            val uri = withContext(Dispatchers.IO) {
                                writeShareableExport(context, fileName, json)
                            }
                            context.startActivity(createJsonShareChooser(uri, fileName, "Compartilhar baralho"))
                        } catch (_: Exception) {
                            Toast.makeText(context, "Não foi possível exportar o baralho.", Toast.LENGTH_LONG).show()
                        }
                    }
                },
            )
        }
        composable(Routes.BACKUP) {
            val backupService = remember(database) { BackupService(RoomBackupDataStore(database)) }
            val backupViewModel: BackupViewModel = viewModel(factory = BackupViewModel.Factory(backupService))
            BackupScreen(backupViewModel, onBack = { navController.popBackStack() })
        }
        composable(Routes.IMPORT) {
            val importViewModel: ImportViewModel = viewModel(factory = ImportViewModel.Factory(repository))
            ImportScreen(
                viewModel = importViewModel,
                onImportSuccess = { navController.popBackStack() },
                onBack = { navController.popBackStack() },
                onMorePrompts = { navController.navigate(Routes.PROMPTS) },
            )
        }
        composable(Routes.PROMPTS) {
            val promptsViewModel: UserPromptsViewModel = viewModel(
                factory = UserPromptsViewModel.Factory(userPromptRepository),
            )
            PromptsScreen(
                viewModel = promptsViewModel,
                onBack = { navController.popBackStack() },
                onSelectPrompt = { promptId -> navController.navigate("prompt/$promptId") },
                onOpenUserPrompt = { promptId -> navController.navigate("myPrompt/detail/$promptId") },
                onCreateUserPrompt = { navController.navigate(Routes.NEW_USER_PROMPT) },
                onEditUserPrompt = { promptId -> navController.navigate("myPrompt/edit/$promptId") },
            )
        }
        composable(
            route = Routes.PROMPT_DETAIL,
            arguments = listOf(navArgument("promptId") { type = NavType.StringType }),
        ) { entry ->
            val promptId = requireNotNull(entry.arguments).getString("promptId")
            val template = PromptTemplates.all.firstOrNull { it.id == promptId }
            if (template != null) {
                PromptDetailScreen(
                    template = template,
                    onBack = { navController.popBackStack() },
                    onImport = { navController.navigate(Routes.IMPORT) },
                )
            }
        }
        composable(
            route = Routes.USER_PROMPT_DETAIL,
            arguments = listOf(navArgument("promptId") { type = NavType.LongType }),
        ) { entry ->
            val promptId = requireNotNull(entry.arguments).getLong("promptId")
            val prompts by userPromptRepository.observeAll().collectAsStateWithLifecycle(initialValue = emptyList())
            val prompt = prompts.firstOrNull { it.id == promptId }
            if (prompt != null) {
                PromptDetailScreen(
                    template = prompt.toPromptTemplate(),
                    onBack = { navController.popBackStack() },
                    onImport = { navController.navigate(Routes.IMPORT) },
                )
            }
        }
        composable(Routes.NEW_USER_PROMPT) {
            UserPromptEditorScreen(
                prompt = null,
                onBack = { navController.popBackStack() },
                onSave = { title, description, body, generatesDeck ->
                    val id = userPromptRepository.create(title, description, body, generatesDeck)
                    navController.navigate("myPrompt/detail/$id") {
                        popUpTo(Routes.NEW_USER_PROMPT) { inclusive = true }
                    }
                },
            )
        }
        composable(
            route = Routes.EDIT_USER_PROMPT,
            arguments = listOf(navArgument("promptId") { type = NavType.LongType }),
        ) { entry ->
            val promptId = requireNotNull(entry.arguments).getLong("promptId")
            var prompt by remember(promptId) { mutableStateOf<UserPrompt?>(null) }
            var loaded by remember(promptId) { mutableStateOf(false) }
            LaunchedEffect(promptId) {
                prompt = userPromptRepository.getById(promptId)
                loaded = true
            }
            if (loaded && prompt != null) {
                UserPromptEditorScreen(
                    prompt = prompt,
                    onBack = { navController.popBackStack() },
                    onSave = { title, description, body, generatesDeck ->
                        userPromptRepository.update(promptId, title, description, body, generatesDeck)
                        navController.popBackStack()
                    },
                )
            }
        }
        composable(
            route = Routes.STUDY,
            arguments = listOf(navArgument("deckId") { type = NavType.LongType }),
        ) { entry ->
            val deckId = requireNotNull(entry.arguments).getLong("deckId")
            val studyViewModel: StudyViewModel = viewModel(factory = StudyViewModel.Factory(repository, deckId))
            val updatedCardId by entry.savedStateHandle.getStateFlow<Long?>("updatedCardId", null).collectAsStateWithLifecycle()
            LaunchedEffect(updatedCardId) {
                updatedCardId?.let {
                    studyViewModel.refreshCard(it)
                    entry.savedStateHandle["updatedCardId"] = null
                }
            }
            StudyScreen(
                studyViewModel,
                onBack = { navController.popBackStack() },
                onEditCard = { cardId -> navController.navigate("editCard/$deckId/$cardId") },
                onManageDeck = { navController.navigate("manage/$deckId") },
            )
        }
        composable(
            route = Routes.MANAGE,
            arguments = listOf(navArgument("deckId") { type = NavType.LongType }),
        ) { entry ->
            val deckId = requireNotNull(entry.arguments).getLong("deckId")
            val manageViewModel: ManageCardsViewModel = viewModel(factory = ManageCardsViewModel.Factory(repository, deckId))
            ManageCardsScreen(
                viewModel = manageViewModel,
                onBack = { navController.popBackStack() },
                onStudy = { navController.navigate("study/$deckId") },
                onAddCard = { navController.navigate("editCard/$deckId/-1") },
                onEditCard = { cardId -> navController.navigate("editCard/$deckId/$cardId") },
            )
        }
        composable(
            route = Routes.EDIT_CARD,
            arguments = listOf(
                navArgument("deckId") { type = NavType.LongType },
                navArgument("cardId") { type = NavType.LongType },
            ),
        ) { entry ->
            val deckId = requireNotNull(entry.arguments).getLong("deckId")
            val cardId = requireNotNull(entry.arguments).getLong("cardId").takeIf { it >= 0 }
            val editorViewModel: CardEditorViewModel = viewModel(factory = CardEditorViewModel.Factory(repository, deckId, cardId))
            CardEditorScreen(
                viewModel = editorViewModel,
                isEditing = cardId != null,
                onBack = { navController.popBackStack() },
                onSaved = {
                    if (cardId != null) navController.previousBackStackEntry?.savedStateHandle?.set("updatedCardId", cardId)
                    navController.popBackStack()
                },
            )
        }
    }
}
