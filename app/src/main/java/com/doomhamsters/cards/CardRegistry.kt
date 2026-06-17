package com.doomhamsters.cards



import com.doomhamsters.cards.definitions.CardCommandDefinition
import com.doomhamsters.cards.definitions.CardDefinition
import com.doomhamsters.cards.definitions.DoomCardDefinition
import com.doomhamsters.cards.definitions.NormalCardDefinition
import com.doomhamsters.cards.definitions.HyperModeCardDefinition
import com.doomhamsters.cards.definitions.PowerNapCardDefinition
import com.doomhamsters.cards.definitions.QuickPeekCardDefinition
import com.doomhamsters.cards.definitions.SnackStashCardDefinition
import com.doomhamsters.cards.definitions.SniffAheadCardDefinition
import com.doomhamsters.cards.definitions.BegForSnacksCardDefinition
import com.doomhamsters.model.Card
import com.doomhamsters.model.CardType
import com.doomhamsters.cards.definitions.SignOfFateCardDefinition

/** Resolves card definitions and command metadata for known cards. */
object CardRegistry {
    val definitions: List<CardDefinition> = listOf(
        DoomCardDefinition,
        SnackStashCardDefinition,
        PowerNapCardDefinition,
        QuickPeekCardDefinition,
        SignOfFateCardDefinition,
        HyperModeCardDefinition,
        SniffAheadCardDefinition,
        BegForSnacksCardDefinition,
        NormalCardDefinition
    )

    private val definitionsByType = definitions.associateBy(CardDefinition::type)
    private val definitionsByCommandId = definitions
        .mapNotNull { definition -> definition.command?.id?.let { it to definition } }
        .toMap()

    /** Returns the most specific definition for a concrete card instance. */
    fun definitionFor(card: Card): CardDefinition {
        val effectDefinition = CardCommandId.fromWire(card.effectId)
            ?.let(definitionsByCommandId::get)
        return effectDefinition ?: definitionForType(card.type)
    }

    /** Returns the registered definition for a card type. */
    fun definitionForType(type: CardType): CardDefinition =
        definitionsByType[type] ?: definitionsByType.getValue(CardType.Normal)

    /** Returns the activatable command definition for a card, if it has one. */
    fun commandFor(card: Card): CardCommandDefinition? = definitionFor(card).command
}

/** Returns the display name that should be shown for this card. */
fun Card.displayName(): String = name?.takeIf { it.isNotBlank() } ?: CardRegistry.definitionFor(this).displayName

/** Returns the gameplay description that should be shown for this card. */
fun Card.cardDescription(): String = CardRegistry.definitionFor(this).description
