package com.doomhamsters.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.doomhamsters.logic.*
import com.doomhamsters.model.Card
import com.doomhamsters.model.CardType
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class GameBoardViewModel : ViewModel() {

    private var gameEngine: GameEngine? = null

    private val _error = MutableSharedFlow<String>()
    val error: SharedFlow<String> = _error
    private val _gameState = MutableStateFlow<GameState?>(null)
    val gameState: StateFlow<GameState?> = _gameState

    private val _gameOver = MutableSharedFlow<String>()
    val gameOver: SharedFlow<String> = _gameOver


    private val _log = MutableStateFlow<List<String>>(emptyList())
    val log: StateFlow<List<String>> = _log
    private val _pendingDoom = MutableStateFlow(false)
    val pendingDoom: StateFlow<Boolean> = _pendingDoom
    private fun addLog(message: String) {
        _log.value = _log.value + message
    }

    fun startGame(playerIds: ArrayList<String>) {
        gameEngine = GameEngine(playerIds)
        _gameState.value = gameEngine?.getState()
    }

    fun draw(playerId: String) {
        try {
            val defused = gameEngine?.draw(playerId)
            _gameState.value = gameEngine?.getState()
            if (defused == true) {
                addLog("$playerId defused a Doom card!")
                _pendingDoom.value = true
            } else {
                addLog("$playerId drew a card")
            }
        } catch (e: InvalidActionException) {
            // TODO: draw the errors on the ui somehow.
        } catch (e: InvalidDrawException) {

        }
    }


    fun insertDoom(position: Int) {
        gameEngine?.insertCard(Card(CardType.Doom), position)
        _gameState.value = gameEngine?.getState()
        _pendingDoom.value = false
        addLog("Doom inserted at position $position")
    }

    fun advanceTurn() {
        if (_pendingDoom.value) return
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