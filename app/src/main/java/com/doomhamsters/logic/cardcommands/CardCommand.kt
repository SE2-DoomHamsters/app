package com.doomhamsters.logic.cardcommands


import com.doomhamsters.cards.CardCommandId
import com.doomhamsters.model.Card
import com.doomhamsters.model.GameState
import com.doomhamsters.model.Player

interface CardCommand {
    val id: CardCommandId

    fun execute(context: CardCommandContext): CardCommandOutcome
}

data class CardCommandContext(
    val gameState: GameState,
    val player: Player,
    val card: Card,
    val engine: CardCommandSupport
)

data class CardCommandOutcome(
    val publicMessage: String,
    val privateMessage: String? = null,
    val revealedCard: Card? = null,
    val endsTurn: Boolean = false
)

interface CardCommandSupport {
    fun discardFromHand(player: Player, card: Card)
    fun peekTopCard(): Card?
}
