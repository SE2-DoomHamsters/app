package com.doomhamsters.cards.definitions



import com.doomhamsters.model.CardType

/** Defines the display metadata for Snack Stash cards. */
object SnackStashCardDefinition : CardDefinition {
    override val type: CardType = CardType.SnackStash
    override val displayName: String = "Snack Stash"
    override val description: String = "Automatically prevents one Doom. Keep it safe!"
    override val command: CardCommandDefinition? = null
}
