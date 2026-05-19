package com.doomhamsters.logic.cardcommands


import com.doomhamsters.cards.CardCommandId
import com.doomhamsters.model.Card
import com.doomhamsters.model.GameState
import com.doomhamsters.model.Player

/** Executes the gameplay effect behind an activatable card. */
interface CardCommand {
    val id: CardCommandId

    /** Runs the command against the supplied game context. */
    fun execute(context: CardCommandContext): CardCommandOutcome
}

/** Provides the data a card command needs to resolve its effect. */
data class CardCommandContext(
    val gameState: GameState,
    val player: Player,
    val card: Card,
    val engine: CardCommandSupport
)

/** Describes the public and private results of a resolved card command. */
data class CardCommandOutcome(
    val publicMessage: String,
    val privateMessage: String? = null,
    val revealedCard: Card? = null,
    val endsTurn: Boolean = false
)

/** Provides the game mutations card commands are allowed to perform. */
interface CardCommandSupport {
    /** Moves a card from a player's hand into the discard pile. */
    fun discardFromHand(player: Player, card: Card)
    /** Returns the current top card of the deck without removing it. */
    fun peekTopCard(): Card?
}
