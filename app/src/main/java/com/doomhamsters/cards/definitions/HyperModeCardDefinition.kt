package com.doomhamsters.cards.definitions

import com.doomhamsters.cards.CardCommandId
import com.doomhamsters.cards.displayName
import com.doomhamsters.logic.cardcommands.CardCommand
import com.doomhamsters.logic.cardcommands.CardCommandContext
import com.doomhamsters.logic.cardcommands.CardCommandOutcome
import com.doomhamsters.model.CardType

/** Defines the Hyper Mode card and its targeted extra turn behavior. */
object HyperModeCardDefinition : CardDefinition {
    override val type: CardType = CardType.HyperMode
    override val displayName: String = "Hyper Mode"
    override val description: String =
        "Adds +1 turn to the next player."
    override val command: CardCommandDefinition = CardCommandDefinition(
        id = CardCommandId.HYPER_MODE,
        executor = HyperModeCommand
    )

    private object HyperModeCommand : CardCommand {
        override val id: CardCommandId = CardCommandId.HYPER_MODE

        override fun execute(context: CardCommandContext): CardCommandOutcome {
            context.engine.discardFromHand(context.player, context.card)
            return CardCommandOutcome(
                publicMessage = "${context.player.name} activated ${displayName}.",
                endsTurn = false
            )
        }
    }
}