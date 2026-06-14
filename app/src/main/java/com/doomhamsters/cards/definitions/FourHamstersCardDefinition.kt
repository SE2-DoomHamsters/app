package com.doomhamsters.cards.definitions

import com.doomhamsters.cards.CardCommandId
import com.doomhamsters.logic.cardcommands.CardCommand
import com.doomhamsters.logic.cardcommands.CardCommandContext
import com.doomhamsters.logic.cardcommands.CardCommandOutcome
import com.doomhamsters.model.CardType

/** Defines the Four Hamsters combo behavior for stealing a life. */
object FourHamstersCardDefinition : CardDefinition {
    override val type: CardType = CardType.Normal
    override val displayName: String = "Hamster Combo: 4-of-a-Kind"
    override val description: String =
        "Play 4 identical Hamster cards to steal 1 life from a target player."

    override val command: CardCommandDefinition = CardCommandDefinition(
        id = CardCommandId.FOUR_HAMSTERS,
        requiresTargetPlayer = true,
        requiresCardType = false,
        requiresHamsterType = true,
        executor = FourHamstersCommand
    )

    private object FourHamstersCommand : CardCommand {
        override val id: CardCommandId = CardCommandId.FOUR_HAMSTERS

        override fun execute(context: CardCommandContext): CardCommandOutcome {

            return CardCommandOutcome(
                publicMessage = "${context.player.name} played a 4-of-a-Kind Hamster Combo.",
                endsTurn = false
            )
        }
    }
}