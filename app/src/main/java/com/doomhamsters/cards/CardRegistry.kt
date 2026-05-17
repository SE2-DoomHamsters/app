package com.doomhamsters.cards



import com.doomhamsters.cards.definitions.CardCommandDefinition
import com.doomhamsters.cards.definitions.CardDefinition
import com.doomhamsters.cards.definitions.DoomCardDefinition
import com.doomhamsters.cards.definitions.NormalCardDefinition
import com.doomhamsters.cards.definitions.PowerNapCardDefinition
import com.doomhamsters.cards.definitions.QuickPeekCardDefinition
import com.doomhamsters.cards.definitions.SnackStashCardDefinition
import com.doomhamsters.model.Card
import com.doomhamsters.model.CardType

object CardRegistry {
    val definitions: List<CardDefinition> = listOf(
        DoomCardDefinition,
        SnackStashCardDefinition,
        PowerNapCardDefinition,
        QuickPeekCardDefinition,
        NormalCardDefinition
    )

    private val definitionsByType = definitions.associateBy(CardDefinition::type)
    private val definitionsByCommandId = definitions
        .mapNotNull { definition -> definition.command?.id?.let { it to definition } }
        .toMap()

    fun definitionFor(card: Card): CardDefinition {
        val effectDefinition = CardCommandId.fromWire(card.effectId)
            ?.let(definitionsByCommandId::get)
        return effectDefinition ?: definitionForType(card.type)
    }

    fun definitionForType(type: CardType): CardDefinition =
        definitionsByType[type] ?: definitionsByType.getValue(CardType.Normal)

    fun commandFor(card: Card): CardCommandDefinition? = definitionFor(card).command
}

fun Card.displayName(): String = name?.takeIf { it.isNotBlank() } ?: CardRegistry.definitionFor(this).displayName

fun Card.cardDescription(): String = CardRegistry.definitionFor(this).description
