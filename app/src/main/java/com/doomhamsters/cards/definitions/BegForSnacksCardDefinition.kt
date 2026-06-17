package com.doomhamsters.cards.definitions

import com.doomhamsters.cards.CardCommandId
import com.doomhamsters.cards.displayName
import com.doomhamsters.logic.cardcommands.CardCommand
import com.doomhamsters.logic.cardcommands.CardCommandContext
import com.doomhamsters.logic.cardcommands.CardCommandOutcome
import com.doomhamsters.model.CardType

/** Defines the Beg for Snacks card and its targeted card-request behavior. */
object BegForSnacksCardDefinition : CardDefinition {
    override val type: CardType = CardType.BegForSnacks
    override val displayName: String = "Beg for Snacks"
    override val description: String =
        "Name a card type and a player. If they have one, they must give it to you."
    override val command: CardCommandDefinition = CardCommandDefinition(
        id = CardCommandId.BEG_FOR_SNACKS,
        requiresTargetPlayer = true,
        requiresCardType = true,
        executor = BegForSnacksCommand
    )

    private object BegForSnacksCommand : CardCommand {
        override val id: CardCommandId = CardCommandId.BEG_FOR_SNACKS

        override fun execute(context: CardCommandContext): CardCommandOutcome {
            context.engine.discardFromHand(context.player, context.card)
            return CardCommandOutcome(
                publicMessage = "${context.player.name} activated ${displayName}.",
                endsTurn = false
            )
        }
    }
}