package com.doomhamsters.logic
import com.doomhamsters.model.*

class InvalidActionException(message: String) : Exception(message)
class InvalidDrawException(message: String) : Exception(message)

class GameEngine(playerIds: ArrayList<String>) {

    private val gameState: GameState = GameFactory.createGame(playerIds)

    fun getState(): GameState = gameState


    //TODO: Restructure it so the logic of what a card does is handled by the cards, strategy pattern.
    fun draw(playerId: String) {
        val player = gameState.players.find { it.id == playerId }
            ?: throw InvalidActionException("Player $playerId not found")

        val card = gameState.deck.draw()
            ?: throw InvalidDrawException("Deck is empty")

        if (card.type == CardType.Doom) {
            val snackStash = player.hand.find { it.type == CardType.SnackStash }
            if (snackStash != null) {
                player.hand.remove(snackStash)
                gameState.discard.add(snackStash)
                gameState.discard.add(card)
            } else {
                player.lives--
                gameState.discard.add(card)
                if (player.lives <= 0) {
                    gameState.players.remove(player)
                }
            }
        } else {
            player.hand.add(card)
        }
    }

    fun insertCard(card: Card, position: Int) {
        gameState.deck.insertAt(card, position)
    }



}