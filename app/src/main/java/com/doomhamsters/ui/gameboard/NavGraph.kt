package com.doomhamsters.ui.gameboard


import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.doomhamsters.ui.GameBoard
import com.doomhamsters.viewmodel.GameBoardViewModel

@Composable
fun NavGraph(
    viewModel: GameBoardViewModel,
    playerAvatars: Map<String, String> = emptyMap(),
    playerNames: Map<String, String> = emptyMap(),
    onReturnToLobby: () -> Unit
) {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = "gameboard") {
        composable("gameboard") {
            GameBoard(
                viewModel = viewModel,
                playerAvatars = playerAvatars,
                playerNames = playerNames,
                onGameOver = { winnerId -> navController.navigate("gameover/$winnerId") }
            )
        }
        composable(
            route = "gameover/{winnerId}",
            arguments = listOf(navArgument("winnerId") { type = NavType.StringType })
        ) { backStackEntry ->
            LaunchedEffect(Unit) {
                kotlinx.coroutines.delay(2500)
                onReturnToLobby()
            }
            val winnerId = backStackEntry.arguments?.getString("winnerId") ?: "Unknown"
            GameOverScreen(
                winnerId = winnerId,
                winnerName = resolvePlayerDisplayName(
                    playerId = winnerId,
                    fallbackName = winnerId,
                    playerNames = playerNames
                ),
                onRestart = onReturnToLobby
            )
        }
    }
}
