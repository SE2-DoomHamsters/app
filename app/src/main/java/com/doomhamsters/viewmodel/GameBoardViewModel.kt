package com.doomhamsters.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.doomhamsters.logic.*
import com.doomhamsters.model.Card
import com.doomhamsters.model.GameState
import com.doomhamsters.model.Status
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

open class GameBoardViewModel(
    protected var gameEngine: GameEngine? = null
) : ViewModel() {

    protected val _gameState = MutableStateFlow<GameState?>(null)
    open val gameState: StateFlow<GameState?> = _gameState

    protected val _gameOver = MutableSharedFlow<String>()
    open val gameOver: SharedFlow<String> = _gameOver

    protected val _error = MutableSharedFlow<String>()
    open val error: SharedFlow<String> = _error

    protected val _log = MutableStateFlow<List<String>>(emptyList())
    open val log: StateFlow<List<String>> = _log

    protected val _pendingDoom = MutableStateFlow<Card?>(null)
    open val pendingDoom: StateFlow<Card?> = _pendingDoom

    protected fun addLog(message: String) {
        _log.value += message
    }

    open fun startGame(playerIds: ArrayList<String>) {
        if (gameEngine == null) {
            gameEngine = GameEngine(playerIds)
        }
        _gameState.value = gameEngine?.getState()
    }

    open fun draw(playerId: String) {
        try {
            val doomCard = gameEngine?.draw(playerId)
            _gameState.value = gameEngine?.getState()
            if (doomCard != null) {
                _pendingDoom.value = doomCard
                addLog("$playerId defused a Doom card!")
            } else {
                addLog("$playerId drew a card")
            }
        } catch (e: InvalidActionException) {
            viewModelScope.launch { _error.emit(e.message ?: "Invalid action") }
        } catch (e: InvalidDrawException) {
            viewModelScope.launch { _error.emit(e.message ?: "Invalid draw") }
        }
    }

    open fun insertDoom(position: Int) {
        _pendingDoom.value?.let { card ->
            gameEngine?.insertCard(card, position)
            _gameState.value = gameEngine?.getState()
            _pendingDoom.value = null
            addLog("Doom inserted at position $position")
        }
    }

    open fun advanceTurn() {
        if (_pendingDoom.value != null) return
        gameEngine?.advanceTurn()
        _gameState.value = gameEngine?.getState()
        if (_gameState.value?.status == Status.Finished) {
            val winner = _gameState.value?.players?.find { it.isAlive() }
            viewModelScope.launch { _gameOver.emit(winner?.id ?: "Unknown") }
        }
    }
}