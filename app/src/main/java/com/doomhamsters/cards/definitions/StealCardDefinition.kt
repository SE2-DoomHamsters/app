package com.doomhamsters.cards.definitions
import com.doomhamsters.cards.CardCommandId
import com.doomhamsters.logic.cardcommands.CardCommand
import com.doomhamsters.logic.cardcommands.CardCommandContext
import com.doomhamsters.logic.cardcommands.CardCommandOutcome
import com.doomhamsters.model.CardType


    /** Defines the Steal Card and its player-targeting activation behavior. */
    object StealCardDefinition : CardDefinition {
        override val type: CardType = CardType.StealCard
        override val displayName: String = "Kartenklau"
        override val description: String = "Klaut einem Mitspieler deiner Wahl eine zufällige Handkarte."

        override val command: CardCommandDefinition = CardCommandDefinition(
            id = CardCommandId.STEAL_CARD,
            privateResult = true,
            endsTurn = false,
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