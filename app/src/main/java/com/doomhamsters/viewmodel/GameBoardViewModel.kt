package com.doomhamsters.viewmodel

import androidx.lifecycle.ViewModel
import com.doomhamsters.logic.*
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

    fun draw(playerId: String) {
        try {
            gameEngine?.draw(playerId)
            _gameState.value = gameEngine?.getState()
        } catch (e: InvalidActionException) {
            // TODO: draw the errors on the ui somehow.
        } catch (e: InvalidDrawException) {

        }
    }


}