package com.doomhamsters.cards.definitions

import com.doomhamsters.cards.CardCommandId
import com.doomhamsters.model.CardType

/** Defines the Tunnel Chaos card; the backend shuffles the deck when it is activated. */
object TunnelChaosCardDefinition : CardDefinition {
    override val type: CardType = CardType.TunnelChaos
    override val displayName: String = "Tunnel Chaos"
    override val description: String = "Shuffle the deck."
    override val command: CardCommandDefinition = CardCommandDefinition(
        id = CardCommandId.TUNNEL_CHAOS
        // no executor — the backend performs the shuffle
    )
}
