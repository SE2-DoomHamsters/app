package com.doomhamsters.logic



import com.doomhamsters.cards.CardRegistry
import com.doomhamsters.model.*
import com.doomhamsters.logic.cardcommands.CardCommandContext
import com.doomhamsters.logic.cardcommands.CardCommandOutcome
import com.doomhamsters.logic.cardcommands.CardCommandSupport

class InvalidActionException(message: String) : Exception(message)
class InvalidDrawException(message: String) : Exception(message)

class GameEngine(playerIds: ArrayList<String>) : CardCommandSupport {

    private val gameState: GameState = GameFactory.createGame(playerIds)

    fun getState(): GameState = gameState.copy()


    //Later Restructure it so the logic of what a card does is handled by the cards, strategy pattern.
    fun draw(playerId: String): Card? {
        val player = gameState.players.find { it.id == playerId }
            ?: throw InvalidActionException("Player $playerId not found")

        if (!player.isAlive()) throw InvalidActionException("Player $playerId is dead")

        val card = gameState.deck.draw()
            ?: throw InvalidDrawException("Deck is empty")

        return if (card.type == CardType.Doom) {
            val snackStash = player.hand.find { it.type == CardType.SnackStash }
            if (snackStash != null) {
                player.hand.remove(snackStash)
                gameState.discard.add(snackStash)
                card
            } else {
                player.lives--
                val randomPosition = (0..gameState.deck.size()).random()
                gameState.deck.insertFromTop(card, randomPosition)
                null
            }
        } else {
            player.hand.add(card)
            null
        }
    }

    fun insertCard(card: Card, position: Int) {
        gameState.deck.insertFromTop(card, position)
    }

    fun activateCard(playerId: String, cardId: String): CardCommandOutcome {
        val player = gameState.players.find { it.id == playerId }
            ?: throw InvalidActionException("Player $playerId not found")

        if (!player.isAlive()) throw InvalidActionException("Player $playerId is dead")
        if (gameState.currentTurnPlayerId != playerId) {
            throw InvalidActionException("Player $playerId cannot activate a card outside their turn")
        }

        val card = player.hand.firstOrNull { it.id == cardId }
            ?: throw InvalidActionException("Card $cardId is not in player $playerId hand")
        val command = CardRegistry.commandFor(card)
            ?: throw InvalidActionException("Card ${card.type} has no activatable command")
        val executor = command.executor
            ?: throw InvalidActionException("Card ${card.type} has no local executor")

        val outcome = executor.execute(
            CardCommandContext(
                gameState = gameState,
                player = player,
                card = card,
                engine = this
            )
        )

        if (outcome.endsTurn) {
            advanceTurn()
        }

        return outcome
    }

    fun advanceTurn() {
        val alivePlayers = gameState.players.filter { it.isAlive() }
        if (alivePlayers.size == 1) {
            gameState.status = Status.Finished
            return
        }

        var nextIndex = (gameState.currentPlayerIndex + 1) % gameState.players.size
        while (!gameState.players[nextIndex].isAlive()) {
            nextIndex = (nextIndex + 1) % gameState.players.size
        }
        gameState.currentPlayerIndex = nextIndex
    }

    override fun discardFromHand(player: Player, card: Card) {
        val removed = player.hand.remove(card)
        if (!removed) {
            throw InvalidActionException("Card ${card.id ?: card.type.name} is not in ${player.id} hand")
        }
        gameState.discard.add(card)
    }

    override fun peekTopCard(): Card? = gameState.deck.peekTop()

}
