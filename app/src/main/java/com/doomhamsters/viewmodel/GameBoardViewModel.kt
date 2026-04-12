package com.doomhamsters.viewmodel

import androidx.lifecycle.ViewModel
import com.doomhamsters.logic.GameEngine
import com.doomhamsters.logic.GameState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class GameBoardViewModel : ViewModel() {

    private var gameEngine: GameEngine? = null

    private val _gameState = MutableStateFlow<GameState?>(null)
    val gameState: StateFlow<GameState?> = _gameState

    fun startGame(playerIds: ArrayList<String>) {
        gameEngine = GameEngine(playerIds)
        _gameState.value = gameEngine?.getState()
    }

}