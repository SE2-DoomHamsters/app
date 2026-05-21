package com.doomhamsters.viewmodel


import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.doomhamsters.GameRepository

/** Creates `GameBoardViewModel` instances for an active game session. */
class GameBoardViewModelFactory(
    private val gameId: String,
    private val playerId: String,
    private val playerName: String,
    private val repository: GameRepository
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    /** Builds a `GameBoardViewModel` when the requested type matches. */
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        require(modelClass.isAssignableFrom(GameBoardViewModel::class.java)) {
            "Unknown ViewModel class: ${modelClass.name}"
        }

        return GameBoardViewModel(
            gameId = gameId,
            initialLocalPlayerId = playerId,
            localPlayerName = playerName,
            repository = repository
        ) as T
    }
}
