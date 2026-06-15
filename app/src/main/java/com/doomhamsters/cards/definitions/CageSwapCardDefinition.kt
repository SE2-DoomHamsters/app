package com.doomhamsters.cards.definitions
import com.doomhamsters.cards.CardCommandId
import com.doomhamsters.model.CardType

/**
 * Defines the Cage Swap card.
 * Swaps the player's entire hand with the next player.
 */
object CageSwapCardDefinition : CardDefinition {
    override val type = CardType.CageSwap
    override val displayName = "Cage Swap"
    override val description = "Swap your entire hand with the next player in turn order."

    // Da der Server das Ziel automatisch ermittelt, reicht hier die ID.
    // Wir brauchen keinen lokalen "executor".
    override val command = CardCommandDefinition(
        id = CardCommandId.CAGE_SWAP
    )
}
