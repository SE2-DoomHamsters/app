package com.doomhamsters.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.doomhamsters.logic.*
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class GameBoardViewModel : ViewModel() {

    private var gameEngine: GameEngine? = null

    private val _gameState = MutableStateFlow<GameState?>(null)
    val gameState: StateFlow<GameState?> = _gameState

    private val _gameOver = MutableSharedFlow<String>()
    val gameOver: SharedFlow<String> = _gameOver


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

    fun advanceTurn() {
        gameEngine?.advanceTurn()
        _gameState.value = gameEngine?.getState()
        if (_gameState.value?.status == Status.Finished) {
            val winner = _gameState.value?.players?.find { it.isAlive() }
            viewModelScope.launch {
                _gameOver.emit(winner?.id ?: "Unknown")
            }
        }
    }
}