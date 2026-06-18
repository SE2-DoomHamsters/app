package com.doomhamsters.cards.definitions

import com.doomhamsters.cards.CardCommandId
import com.doomhamsters.logic.cardcommands.CardCommand
import com.doomhamsters.logic.cardcommands.CardCommandContext
import com.doomhamsters.logic.cardcommands.CardCommandOutcome
import com.doomhamsters.model.CardType

/** Defines the Hamster Trio combo and its targeted named-card-take behavior. */
object HamsterTrioCardDefinition : CardDefinition {
    override val type: CardType = CardType.HamsterTrio  // virtual combo; no single card type
    override val displayName: String = "Hamster Trio"
    override val description: String =
        "Play three identical Hamster cards to take a named card from another player."
    override val command: CardCommandDefinition = CardCommandDefinition(
        id = CardCommandId.HAMSTER_TRIO,
        requiresTargetPlayer = true,
        requiresCardType = true,
        requiresHamsterType = true,
        executor = null
    )

    private object HamsterTrioCommand : CardCommand {
        override val id: CardCommandId = CardCommandId.HAMSTER_TRIO

        override fun execute(context: CardCommandContext): CardCommandOutcome {
            context.engine.discardFromHand(context.player, context.card)
            return CardCommandOutcome(
                publicMessage = "${context.player.name} played a Hamster Trio.",
                endsTurn = false
            )
        }
    }
}