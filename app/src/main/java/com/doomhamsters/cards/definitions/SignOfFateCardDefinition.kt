package com.doomhamsters.cards.definitions

import com.doomhamsters.cards.CardCommandId
import com.doomhamsters.logic.cardcommands.CardCommand
import com.doomhamsters.logic.cardcommands.CardCommandContext
import com.doomhamsters.logic.cardcommands.CardCommandOutcome
import com.doomhamsters.model.CardType

/** Defines the Sign of Fate card and its life-gain activation behavior. */
object SignOfFateCardDefinition : CardDefinition {

    override val type: CardType = CardType.SignOfFate

    override val displayName: String = "Sign of Fate"

    override val description: String = "Gain 1 life."

    override val command: CardCommandDefinition = CardCommandDefinition(
        id = CardCommandId.SIGN_OF_FATE,
        executor = SignOfFateCommand
    )

    private object SignOfFateCommand : CardCommand {

        override val id: CardCommandId = CardCommandId.SIGN_OF_FATE

        override fun execute(context: CardCommandContext): CardCommandOutcome {
            context.engine.discardFromHand(context.player, context.card)

            return CardCommandOutcome(
                publicMessage = "${context.player.name} activated $displayName.",
                endsTurn = false
            )
        }
    }
}