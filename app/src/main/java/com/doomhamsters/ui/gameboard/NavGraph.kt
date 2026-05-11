package com.doomhamsters.ui.gameboard

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.doomhamsters.ui.GameBoard
import com.doomhamsters.viewmodel.GameBoardViewModel

@Composable
fun NavGraph(viewModel: GameBoardViewModel) {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = "gameboard") {
        composable("gameboard") {
            GameBoard(
                viewModel = viewModel,
                onGameOver = { winnerId -> navController.navigate("gameover/$winnerId") }
            )
        }
        composable(
            route = "gameover/{winnerId}",
            arguments = listOf(navArgument("winnerId") { type = NavType.StringType })
        ) { backStackEntry ->
            GameOverScreen(
                winnerId = backStackEntry.arguments?.getString("winnerId") ?: "Unknown",
                onRestart = { navController.navigate("gameboard") }
            )
        }
    }
}