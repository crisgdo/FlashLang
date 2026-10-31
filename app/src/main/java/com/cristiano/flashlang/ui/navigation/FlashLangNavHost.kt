package com.cristiano.flashlang.ui.navigation

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.NavType
import androidx.navigation.navArgument
import com.cristiano.flashlang.data.repository.DeckRepository
import com.cristiano.flashlang.ui.home.HomeScreen
import com.cristiano.flashlang.ui.home.HomeViewModel
import com.cristiano.flashlang.ui.importdeck.ImportScreen
import com.cristiano.flashlang.ui.importdeck.ImportViewModel
import com.cristiano.flashlang.ui.study.StudyScreen
import com.cristiano.flashlang.ui.study.StudyViewModel

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
            HomeScreen(
                homeViewModel,
                onImportClick = { navController.navigate(Routes.IMPORT) },
                onDeckClick = { deckId -> navController.navigate("study/$deckId") },
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
