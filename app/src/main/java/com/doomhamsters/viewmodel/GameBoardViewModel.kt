package com.doomhamsters.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.doomhamsters.logic.*
import com.doomhamsters.model.Card
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

    private val _error = MutableSharedFlow<String>()
    val error: SharedFlow<String> = _error

    private val _log = MutableStateFlow<List<String>>(emptyList())
    val log: StateFlow<List<String>> = _log

    private val _pendingDoom = MutableStateFlow<Card?>(null)
    val pendingDoom: StateFlow<Card?> = _pendingDoom

    private fun addLog(message: String) {
        _log.value = _log.value + message
    }

    fun startGame(playerIds: ArrayList<String>) {
        gameEngine = GameEngine(playerIds)
        _gameState.value = gameEngine?.getState()
    }

    fun draw(playerId: String) {
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

    fun insertDoom(position: Int) {
        _pendingDoom.value?.let { card ->
            gameEngine?.insertCard(card, position)
            _gameState.value = gameEngine?.getState()
            _pendingDoom.value = null
            addLog("Doom inserted at position $position")
        }
    }

    fun advanceTurn() {
        if (_pendingDoom.value != null) return
        gameEngine?.advanceTurn()
        _gameState.value = gameEngine?.getState()
        if (_gameState.value?.status == Status.Finished) {
            val winner = _gameState.value?.players?.find { it.isAlive() }
            viewModelScope.launch { _gameOver.emit(winner?.id ?: "Unknown") }
        }
    }
}