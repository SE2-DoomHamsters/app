package com.doomhamsters.cards.definitions

import com.doomhamsters.cards.CardCommandId
import com.doomhamsters.logic.cardcommands.CardCommand
import com.doomhamsters.model.CardType

/** Describes how a card type should be named, explained, and activated. */
interface CardDefinition {
    val type: CardType
    val displayName: String
    val description: String
    val command: CardCommandDefinition?
}

/** Defines how a card command is routed and optionally executed locally. */
data class CardCommandDefinition(
    val id: CardCommandId,
    val actionPath: String = "card/activate",
    val endsTurn: Boolean = false,
    val privateResult: Boolean = false,
    val executor: CardCommand? = null
)
