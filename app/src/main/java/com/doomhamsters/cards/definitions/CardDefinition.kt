package com.doomhamsters.cards.definitions

import com.doomhamsters.cards.CardCommandId
import com.doomhamsters.logic.cardcommands.CardCommand
import com.doomhamsters.model.CardType

interface CardDefinition {
    val type: CardType
    val displayName: String
    val description: String
    val command: CardCommandDefinition?
}

data class CardCommandDefinition(
    val id: CardCommandId,
    val actionPath: String = "card/activate",
    val endsTurn: Boolean = false,
    val privateResult: Boolean = false,
    val executor: CardCommand? = null
)
