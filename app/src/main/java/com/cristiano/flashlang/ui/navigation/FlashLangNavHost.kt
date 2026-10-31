package com.cristiano.flashlang.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.cristiano.flashlang.data.repository.DeckRepository
import com.cristiano.flashlang.ui.home.HomeScreen
import com.cristiano.flashlang.ui.home.HomeViewModel

private object Routes {
    const val HOME = "home"
    const val IMPORT = "import"
}

@Composable
fun FlashLangNavHost(repository: DeckRepository, navController: NavHostController = rememberNavController()) {
    NavHost(navController = navController, startDestination = Routes.HOME) {
        composable(Routes.HOME) {
            val homeViewModel: HomeViewModel = viewModel(factory = HomeViewModel.Factory(repository))
            HomeScreen(homeViewModel, onImportClick = { navController.navigate(Routes.IMPORT) })
        }
        composable(Routes.IMPORT) {
            Scaffold { padding ->
                Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                    Text("Importar baralho")
                }
            }
        }
    }
}
