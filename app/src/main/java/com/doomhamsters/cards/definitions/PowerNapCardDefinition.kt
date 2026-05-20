package com.doomhamsters.cards.definitions


import com.doomhamsters.cards.CardCommandId
import com.doomhamsters.logic.cardcommands.CardCommand
import com.doomhamsters.logic.cardcommands.CardCommandContext
import com.doomhamsters.logic.cardcommands.CardCommandOutcome
import com.doomhamsters.model.CardType

/** Defines the Power Nap card and its skip-draw activation behavior. */
object PowerNapCardDefinition : CardDefinition {
    override val type: CardType = CardType.PowerNap
    override val displayName: String = "Power Nap"
    override val description: String = "Skip your draw phase and end your turn."
    override val command: CardCommandDefinition = CardCommandDefinition(
        id = CardCommandId.POWER_NAP,
        endsTurn = true,
        executor = PowerNapCommand
    )

    private object PowerNapCommand : CardCommand {
        override val id: CardCommandId = CardCommandId.POWER_NAP

        override fun execute(context: CardCommandContext): CardCommandOutcome {
            context.engine.discardFromHand(context.player, context.card)
            return CardCommandOutcome(
                publicMessage = "${context.player.name} activated ${displayName} and skipped drawing.",
                endsTurn = true
            )
        }
    }
}
