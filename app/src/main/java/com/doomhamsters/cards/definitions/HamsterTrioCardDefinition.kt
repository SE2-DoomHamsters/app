package com.doomhamsters.cards.definitions

import com.doomhamsters.cards.CardCommandId
import com.doomhamsters.model.CardType

/** Defines the Hamster Trio combo and its targeted named-card-take behavior. */
object HamsterTrioCardDefinition : CardDefinition {
    override val type: CardType = CardType.HamsterTrio
    override val displayName: String = "Hamster Trio"
    override val description: String =
        "Play three identical Hamster cards to take a named card from another player."
    override val command: CardCommandDefinition = CardCommandDefinition(
        id = CardCommandId.HAMSTER_TRIO,
        requiresTargetPlayer = true,
        requiresCardType = true,
        requiresHamsterType = true,
        executor = null
    )
}