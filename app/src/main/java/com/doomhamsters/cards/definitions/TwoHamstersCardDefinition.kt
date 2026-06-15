package com.doomhamsters.cards.definitions

import com.doomhamsters.cards.CardCommandId
import com.doomhamsters.logic.cardcommands.CardCommand
import com.doomhamsters.logic.cardcommands.CardCommandContext
import com.doomhamsters.logic.cardcommands.CardCommandOutcome
import com.doomhamsters.model.CardType

/** Defines the Two Hamsters combo behavior for stealing a random card. */
object TwoHamstersCardDefinition : CardDefinition {
    override val type: CardType = CardType.Normal
    override val displayName: String = "Hamster Combo: 2-of-a-Kind"
    override val description: String =
        "Play 2 identical Hamster cards to steal a random card from a target player."

    override val command: CardCommandDefinition = CardCommandDefinition(
        id = CardCommandId.TWO_HAMSTERS,
        privateResult = true,
        requiresTargetPlayer = true,
        requiresHamsterType = true,
        executor = TwoHamstersCommand
    )

    private object TwoHamstersCommand : CardCommand {
        override val id: CardCommandId = CardCommandId.TWO_HAMSTERS

        override fun execute(context: CardCommandContext): CardCommandOutcome {
            // Lokales Abwerfen der Karte für die UI-Animation
            context.engine.discardFromHand(context.player, context.card)

            return CardCommandOutcome(
                publicMessage = "${context.player.name} played a 2-of-a-Kind Hamster Combo.",
                endsTurn = false
            )
        }
    }
}