package com.doomhamsters.cards.definitions


import com.doomhamsters.model.CardType

/** Defines the display metadata for Doom cards. */
object DoomCardDefinition : CardDefinition {
    override val type: CardType = CardType.Doom
    override val displayName: String = "Doom"
    override val description: String = "Lose 1 life unless Snack Stash saves you."
    override val command: CardCommandDefinition? = null
}
