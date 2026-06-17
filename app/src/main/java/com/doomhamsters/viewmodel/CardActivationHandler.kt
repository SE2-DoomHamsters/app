package com.doomhamsters.viewmodel

import android.util.Log
import com.doomhamsters.GameRepository
import com.doomhamsters.cards.CardRegistry
import com.doomhamsters.cards.CardCommandRequest
import com.doomhamsters.model.Card
import com.doomhamsters.model.GameState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/**
 * Handles card activation for the game board.
 *
 * <p>Encapsulates canActivate checks, the targeted-card selection flow,
 * and the outbound repository call so GameBoardViewModel stays focused
 * on connection and game state concerns.
 */
class CardActivationHandler(
    private val gameId: String,
    private val repository: GameRepository,
    private val scope: CoroutineScope,
    private val error: MutableSharedFlow<String>,
    private val getLocalPlayerId: () -> String,
    private val getGameState: () -> GameState?,
    private val isPendingDoom: () -> Boolean,
    private val isLocalPlayersTurn: () -> Boolean,
    private val onStateChanged: suspend () -> Unit
) {
    private val tag = "CardActivationHandler"

    private val _pendingTargetedCard = MutableStateFlow<Card?>(null)
    val pendingTargetedCard: StateFlow<Card?> = _pendingTargetedCard

    private val _isActivatingCard = MutableStateFlow(false)
    val isActivatingCard: StateFlow<Boolean> = _isActivatingCard

    /** Returns whether the local player can activate the supplied card right now. */
    fun canActivate(card: Card): Boolean {
        val state = getGameState() ?: return false
        val localPlayerId = getLocalPlayerId()
        val localPlayer = state.players.firstOrNull { it.id == localPlayerId } ?: return false
        val isResolvingLocalDoom =
            isPendingDoom() || state.resolvingDoomPlayerId == localPlayerId
        val command = CardRegistry.commandFor(card) ?: return false

        return !_isActivatingCard.value &&
                !isResolvingLocalDoom &&
                isLocalPlayersTurn() &&
                localPlayer.hand.any { it.id == card.id || (card.id == null && it == card) } &&
                command.actionPath.isNotBlank()
    }

    /**
     * Activates a card. Cards requiring target selection park in [pendingTargetedCard]
     * and wait for [activateWithTargets] to be called by the UI.
     */
    fun activate(card: Card) {
        val command = CardRegistry.commandFor(card) ?: return
        if (!canActivate(card)) {
            Log.d(tag, "Activate ignored gameId=$gameId cardId=${card.id} cardType=${card.type}")
            return
        }

        if (command.requiresTargetPlayer || command.requiresCardType || command.requiresHamsterType) {
            _pendingTargetedCard.value = card
            return
        }
        send(card, emptyMap())
    }

    /**
     * Completes activation for a targeted card after the UI has collected
     * the required player and card-type selection.
     *
     * @param hamsterType required only for the Hamster Trio combo
     */
    fun activateWithTargets(
        card: Card,
        targetPlayerId: String,
        requestedCardType: String,
        hamsterType: String? = null
    ) {
        _pendingTargetedCard.value = null
        if (!canActivate(card)) return
        val params = buildMap<String, String> {
            put("targetPlayerId", targetPlayerId)
            put("cardType", requestedCardType)
            hamsterType?.let { put("hamsterType", it) }
        }
        send(card, params)
    }

    /** Cancels a pending targeted-card selection without sending anything. */
    fun cancelSelection() {
        _pendingTargetedCard.value = null
    }

    private fun send(card: Card, parameters: Map<String, String>) {
        val command = CardRegistry.commandFor(card) ?: return
        val localPlayerId = getLocalPlayerId()
        scope.launch {
            try {
                _isActivatingCard.value = true
                Log.d(tag, "Sending card activation gameId=$gameId playerId=$localPlayerId cardId=${card.id} commandId=${command.id}")
                repository.sendAction(
                    gameId,
                    command.actionPath,
                    CardCommandRequest(
                        playerId = localPlayerId,
                        cardId = card.id,
                        cardType = card.type,
                        commandId = command.id,
                        parameters = parameters
                    ).toJson()
                )
                onStateChanged()
            } catch (e: Exception) {
                Log.e(tag, "Card activation failed gameId=$gameId playerId=$localPlayerId cardId=${card.id}", e)
                error.emit("Card activation failed: ${e.message}")
            } finally {
                _isActivatingCard.value = false
            }
        }
    }
}