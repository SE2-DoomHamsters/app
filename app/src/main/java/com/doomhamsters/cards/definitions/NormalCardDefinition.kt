package com.doomhamsters.cards.definitions

/* TEMP COMMIT NOTE
Commit with:
Task 2 / Commit 4 - feat(cards): add self-contained card definitions and command template
Delete this note after staging.
*/

import com.doomhamsters.model.CardType

/** Defines the display metadata for regular cards. */
object NormalCardDefinition : CardDefinition {
    override val type: CardType = CardType.Normal
    override val displayName: String = "Normal"
    override val description: String = "Just a regular card. Nothing happens when you play it."
    override val command: CardCommandDefinition? = null
}
