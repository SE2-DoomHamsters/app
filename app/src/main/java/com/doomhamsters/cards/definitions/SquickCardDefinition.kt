package com.doomhamsters.cards.definitions

import com.doomhamsters.cards.CardCommandId
import com.doomhamsters.model.CardType

/** Defines the Squick card that cancels the most recently executed card effect. */
object SquickCardDefinition : CardDefinition {
    override val type: CardType = CardType.Squick
    override val displayName: String = "Squick"
    override val description: String =
        "Cancel the most recently played card effect and restore the game to its previous state."
    override val command: CardCommandDefinition = CardCommandDefinition(
        id = CardCommandId.SQUICK,
        executor = null
    )
}
