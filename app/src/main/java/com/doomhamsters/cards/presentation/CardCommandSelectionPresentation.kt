package com.doomhamsters.cards.presentation

import com.doomhamsters.cards.CardRegistry
import com.doomhamsters.model.Card
import com.doomhamsters.model.CardType
import com.doomhamsters.model.GameState

/** Display-ready option for a targeted card-command picker. */
data class CardCommandSelectionOption(
    val value: String,
    val label: String
)

/** Display-ready card type option for commands that request a card type. */
data class CardCommandTypeOption(
    val wireValue: String,
    val label: String,
    val type: CardType
) {
    fun toPreviewCard(): Card = Card(
        type = type,
        name = label,
        effectId = CardRegistry.definitionForType(type).command?.id?.name
    )
}

/** Prepared state for the card-command selection overlay. */
data class CardCommandSelectionDisplay(
    val title: String,
    val description: String,
    val requiresTargetPlayer: Boolean,
    val requiresRequestedCardType: Boolean,
    val requiresHamsterType: Boolean,
    val targetOptions: List<CardCommandSelectionOption>,
    val cardTypeOptions: List<CardCommandTypeOption>,
    val canConfirm: Boolean
)

/** Builds display state and activation parameters for targeted card-command selection. */
object CardCommandSelectionPresentation {
    fun display(
        card: Card?,
        gameState: GameState,
        localPlayerId: String,
        selectedTargetPlayerId: String?,
        selectedCardType: String?,
        hamsterType: String
    ): CardCommandSelectionDisplay? {
        if (card == null) return null

        val definition = CardRegistry.definitionFor(card)
        val command = definition.command ?: return null
        val targetOptions = gameState.players
            .filter { player -> player.id != localPlayerId && player.isAlive() }
            .map { player -> CardCommandSelectionOption(player.id, player.name) }
        val cardTypeOptions = requestableCardTypeOptions()

        return CardCommandSelectionDisplay(
            title = definition.displayName,
            description = definition.description,
            requiresTargetPlayer = command.requiresTargetPlayer,
            requiresRequestedCardType = command.requiresCardType,
            requiresHamsterType = command.requiresHamsterType,
            targetOptions = targetOptions,
            cardTypeOptions = cardTypeOptions,
            canConfirm = canConfirm(
                requiresTargetPlayer = command.requiresTargetPlayer,
                selectedTargetPlayerId = selectedTargetPlayerId,
                requiresRequestedCardType = command.requiresCardType,
                selectedCardType = selectedCardType,
                requiresHamsterType = command.requiresHamsterType,
                hamsterType = hamsterType
            )
        )
    }

    fun initialRequestedCardType(card: Card?): String? {
        if (card == null) return null
        val command = CardRegistry.commandFor(card) ?: return null
        if (!command.requiresCardType) return null

        return requestableCardTypeOptions().firstOrNull()?.wireValue
    }

    fun activationParameters(
        targetPlayerId: String?,
        requestedCardType: String?,
        hamsterType: String?
    ): Map<String, Any> = buildMap {
        targetPlayerId?.takeIf { it.isNotBlank() }?.let { put("targetPlayerId", it) }
        requestedCardType?.takeIf { it.isNotBlank() }?.let { put("cardType", it) }
        hamsterType?.takeIf { it.isNotBlank() }?.let { put("hamsterType", it) }
    }

    fun requestableCardTypeOptions(): List<CardCommandTypeOption> =
        CardType.entries
            .filterNot { it == CardType.Doom }
            .map { type ->
                CardCommandTypeOption(
                    wireValue = type.requestWireValue(),
                    label = CardRegistry.definitionForType(type).displayName,
                    type = type
                )
            }

    private fun canConfirm(
        requiresTargetPlayer: Boolean,
        selectedTargetPlayerId: String?,
        requiresRequestedCardType: Boolean,
        selectedCardType: String?,
        requiresHamsterType: Boolean,
        hamsterType: String
    ): Boolean {
        return (!requiresTargetPlayer || !selectedTargetPlayerId.isNullOrBlank()) &&
            (!requiresRequestedCardType || !selectedCardType.isNullOrBlank()) &&
            (!requiresHamsterType || hamsterType.isNotBlank())
    }

    private fun CardType.requestWireValue(): String = when (this) {
        CardType.Doom -> "doom"
        CardType.SnackStash -> "snack_stash"
        CardType.TwoHamsters -> "HamsterTwo"
        CardType.Normal -> "normal"
        else -> name
    }
}
