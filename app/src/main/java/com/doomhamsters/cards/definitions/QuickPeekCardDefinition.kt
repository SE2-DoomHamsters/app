package com.doomhamsters.cards.definitions



import com.doomhamsters.cards.CardCommandId
import com.doomhamsters.cards.displayName
import com.doomhamsters.logic.cardcommands.CardCommand
import com.doomhamsters.logic.cardcommands.CardCommandContext
import com.doomhamsters.logic.cardcommands.CardCommandOutcome
import com.doomhamsters.model.CardType

object QuickPeekCardDefinition : CardDefinition {
    override val type: CardType = CardType.QuickPeek
    override val displayName: String = "Quick Peek"
    override val description: String = "Privately look at the top card of the deck."
    override val command: CardCommandDefinition = CardCommandDefinition(
        id = CardCommandId.QUICK_PEEK,
        privateResult = true,
        executor = QuickPeekCommand
    )

    private object QuickPeekCommand : CardCommand {
        override val id: CardCommandId = CardCommandId.QUICK_PEEK

        override fun execute(context: CardCommandContext): CardCommandOutcome {
            context.engine.discardFromHand(context.player, context.card)
            val topCard = context.engine.peekTopCard()

            return CardCommandOutcome(
                publicMessage = "${context.player.name} activated ${displayName}.",
                privateMessage = topCard?.let { "Top card: ${it.displayName()}." } ?: "The deck is empty.",
                revealedCard = topCard,
                endsTurn = false
            )
        }
    }
}
