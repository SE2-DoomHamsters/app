package com.doomhamsters.ui.lobby

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import com.doomhamsters.BackendConfig
import com.doomhamsters.GameRepository
import com.doomhamsters.ui.gameboard.NavGraph
import com.doomhamsters.viewmodel.GameBoardViewModel
import com.doomhamsters.viewmodel.GameBoardViewModelFactory

    /** Creates the game board view model for an active session and shows the board flow. */
    @Composable
    fun GameBoardScreen(
        gameId: String,
        playerId: String,
        playerName: String,
        playerAvatars: Map<String, String> = emptyMap(),
        playerNames: Map<String, String> = emptyMap(),
        onReturnToLobby: () -> Unit
    ) {
        val gameBoardViewModel: GameBoardViewModel = viewModel(
            key = "game-$gameId-player-$playerId",
            factory = GameBoardViewModelFactory(
                gameId = gameId,
                playerId = playerId,
                playerName = playerName,
                repository = GameRepository(BackendConfig.BASE_URL)
            )
        )

        NavGraph(
            viewModel = gameBoardViewModel,
            playerAvatars = playerAvatars,
            playerNames = playerNames,
            onReturnToLobby = onReturnToLobby
        )
    }