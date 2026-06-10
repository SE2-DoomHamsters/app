package com.doomhamsters.cards.definitions

import com.doomhamsters.cards.CardCommandId
import com.doomhamsters.logic.cardcommands.CardCommand
import com.doomhamsters.logic.cardcommands.CardCommandContext
import com.doomhamsters.logic.cardcommands.CardCommandOutcome
import com.doomhamsters.model.CardType

/** Defines the Hamster Combo card and its life-stealing activation behavior. */
object FourHamstersCardDefinition : CardDefinition {
    override val type = CardType.FourHamsters
    override val displayName = "Hamster Combo: 4-of-a-Kind — Steal 1 Life"
    override val description = "Play 4 identical Hamster cards to steal 1 life from a target player."
    override val command = CardCommandDefinition(
        id = CardCommandId.FOUR_HAMSTERS,
        endsTurn = false, // Das Spiel läuft danach für den Spieler weiter
        executor = FourHamstersCommand
    )

    private object FourHamstersCommand : CardCommand {
        override val id: CardCommandId = CardCommandId.FOUR_HAMSTERS

        override fun execute(context: CardCommandContext): CardCommandOutcome {

            // Lokale Vorab-Ausführung auf dem Client:
            context.engine.discardFromHand(context.player, context.card)

            // die finalen Lebensbalken-Updates via WebSocket.
            return CardCommandOutcome(
                publicMessage = "${context.player.name} activated $displayName.",
                endsTurn = false
            )
        }
    }
}