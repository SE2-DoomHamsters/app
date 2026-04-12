package com.doomhamsters.logic
import com.doomhamsters.model.*

class InvalidActionException(message: String) : Exception(message)
class InvalidDrawException(message: String) : Exception(message)

class GameEngine(playerIds: ArrayList<String>) {

    private val gameState: GameState = GameFactory.createGame(playerIds)

    fun getState(): GameState = gameState
}