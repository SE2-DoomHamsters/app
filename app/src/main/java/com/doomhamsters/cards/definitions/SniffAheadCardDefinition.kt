package com.doomhamsters.cards.definitions

import com.doomhamsters.cards.CardCommandId
import com.doomhamsters.cards.displayName
import com.doomhamsters.logic.cardcommands.CardCommand
import com.doomhamsters.logic.cardcommands.CardCommandContext
import com.doomhamsters.logic.cardcommands.CardCommandOutcome
import com.doomhamsters.model.CardType

/** Defines the Sniff Ahead card and its private top-3 peek behavior. */
object SniffAheadCardDefinition : CardDefinition {
    override val type: CardType = CardType.SniffAhead
    override val displayName: String = "Sniff Ahead"
    override val description: String = "Secretly look at the top 3 cards of the deck."
    override val command: CardCommandDefinition = CardCommandDefinition(
        id = CardCommandId.SNIFF_AHEAD,
        privateResult = true,
        executor = SniffAheadCommand
    )

    private object SniffAheadCommand : CardCommand {
        override val id: CardCommandId = CardCommandId.SNIFF_AHEAD

        override fun execute(context: CardCommandContext): CardCommandOutcome {
            context.engine.discardFromHand(context.player, context.card)
            val topCards = context.engine.peekTopCards(3)

            val privateMessage = if (topCards.isEmpty()) {
                "The deck is empty."
            } else {
                "Top ${topCards.size} card(s): ${topCards.joinToString(", ") { it.displayName() }}."
            }

            return CardCommandOutcome(
                publicMessage = "${context.player.name} activated $displayName.",
                privateMessage = privateMessage,
                revealedCards = topCards,
                endsTurn = false
            )
        }
    }
}
