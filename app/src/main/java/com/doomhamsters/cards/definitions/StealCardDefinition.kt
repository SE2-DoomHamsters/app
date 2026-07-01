package com.doomhamsters.cards.definitions
import com.doomhamsters.cards.CardCommandId
import com.doomhamsters.logic.cardcommands.CardCommand
import com.doomhamsters.logic.cardcommands.CardCommandContext
import com.doomhamsters.logic.cardcommands.CardCommandOutcome
import com.doomhamsters.model.CardType


    /** Defines the Steal Card and its player-targeting activation behavior. */
    object StealCardDefinition : CardDefinition {
        override val type: CardType = CardType.StealCard
        override val displayName: String = "Tiny Thief"
        override val description: String = "Steals a random card from a player of your choice."

        override val command: CardCommandDefinition = CardCommandDefinition(
            id = CardCommandId.STEAL_CARD,
            privateResult = true,
            endsTurn = false,
            requiresTargetPlayer = true,
            actionPath = "card/activate",
            executor = StealCardCommand
        )

        private object StealCardCommand : CardCommand {
            override val id: CardCommandId = CardCommandId.STEAL_CARD

            override fun execute(context: CardCommandContext): CardCommandOutcome {
                // Legt die Karte im Frontend lokal auf den Ablagestapel
                context.engine.discardFromHand(context.player, context.card)

                return CardCommandOutcome(
                    publicMessage = "${context.player.name} aktiviert ${displayName}.",
                    endsTurn = false
                )
            }
        }
    }