package com.doomhamsters.cheating

import com.doomhamsters.model.Card
import com.doomhamsters.model.GameState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.json.JSONObject

/** UI side effects requested after the feature handles Snack Stash state or events. */
sealed interface SnackStashUiEffect {
    object None : SnackStashUiEffect
    object WaitingForVotes : SnackStashUiEffect
    object ClearDoomSelection : SnackStashUiEffect
}

/** Result of attempting to handle one public game event as a Snack Stash event. */
data class SnackStashPublicEventResult(
    val handled: Boolean,
    val logMessage: String? = null,
    val effect: SnackStashUiEffect = SnackStashUiEffect.None
)

/** Owns the Snack Stash ViewModel state and remote actions behind small GameBoardViewModel hooks. */
class SnackStashViewModelFeature(
    private val gameId: String,
    private val localPlayerId: () -> String,
    private val isLocalPlayersTurn: () -> Boolean,
    private val pendingDoomRequiresSelection: () -> Boolean,
    private val gameState: () -> GameState?,
    private val sendAction: suspend (action: String, payload: JSONObject) -> Unit,
    private val refreshGameState: suspend () -> Unit,
    private val showWaitingForVotes: () -> Unit,
    private val clearPendingDoomUi: () -> Unit,
    private val launchAction: (userErrorMessage: String, action: suspend () -> Unit) -> Unit,
    private val logDebug: (String) -> Unit
) {
    private val remoteActions = SnackStashRemoteActions(sendAction)

    private val _pendingClaim = MutableStateFlow<SnackStashClaimEvent?>(null)
    val pendingClaim: StateFlow<SnackStashClaimEvent?> = _pendingClaim

    private val _resolutionNotice = MutableStateFlow<SnackStashResolutionEvent?>(null)
    val resolutionNotice: StateFlow<SnackStashResolutionEvent?> = _resolutionNotice

    private val currentClaim: SnackStashClaimEvent?
        get() = _pendingClaim.value

    fun handlePublicEvent(event: JSONObject): SnackStashPublicEventResult {
        SnackStashEventJson.claimFromJsonOrNull(event)?.let { claimEvent ->
            _pendingClaim.value = claimEvent
            return SnackStashPublicEventResult(
                handled = true,
                logMessage = claimEvent.message ?: "${claimEvent.playerName} claims Snack Stash.",
                effect = if (claimEvent.playerId == localPlayerId()) {
                    SnackStashUiEffect.WaitingForVotes
                } else {
                    SnackStashUiEffect.None
                }
            )
        }

        SnackStashEventJson.resolutionFromJsonOrNull(event)?.let { resolutionEvent ->
            _pendingClaim.value = null
            _resolutionNotice.value = resolutionEvent
            return SnackStashPublicEventResult(
                handled = true,
                logMessage = resolutionEvent.message
                    ?: "Snack Stash claim resolved: ${resolutionEvent.outcome.name}.",
                effect = if (resolutionEvent.outcome == SnackStashResolutionOutcome.CHEATER) {
                    SnackStashUiEffect.ClearDoomSelection
                } else {
                    SnackStashUiEffect.None
                }
            )
        }

        return SnackStashPublicEventResult(handled = false)
    }

    fun syncFromGameState(state: GameState): SnackStashUiEffect {
        val claim = state.pendingSnackStashClaim
        if (claim != null) {
            _pendingClaim.value = claim
            return if (claim.playerId == localPlayerId()) {
                SnackStashUiEffect.WaitingForVotes
            } else {
                SnackStashUiEffect.None
            }
        }

        if (state.resolvingDoomPlayerId == null || state.pendingDoomRequiresInsertion) {
            _pendingClaim.value = null
        }

        return SnackStashUiEffect.None
    }

    fun claim(card: Card) {
        launchAction("Snack Stash claim failed") {
            sendClaim(card)
        }
    }

    fun vote(claimId: String, challenge: Boolean) {
        launchAction("Snack Stash vote failed") {
            sendVote(claimId, challenge)
        }
    }

    fun acceptDoom() {
        launchAction("Accept Doom failed") {
            sendAcceptDoom()
        }
    }

    fun dismissResolutionNotice() {
        launchAction("Turn advance failed") {
            sendDismissResolutionNotice()
        }
    }

    fun clearClaim() {
        _pendingClaim.value = null
    }

    private suspend fun sendClaim(card: Card) {
        val cardId = card.id
        val playerId = localPlayerId()
        val state = gameState()
        if (
            cardId == null ||
            state == null ||
            state.resolvingDoomPlayerId != playerId ||
            state.pendingDoomRequiresInsertion ||
            !pendingDoomRequiresSelection() ||
            currentClaim != null
        ) {
            logDebug("Snack Stash claim ignored gameId=$gameId cardId=$cardId")
            return
        }

        logDebug("Sending Snack Stash claim gameId=$gameId playerId=$playerId cardId=$cardId")
        remoteActions.claim(playerId, cardId)
        showWaitingForVotes()
        refreshGameState()
    }

    private suspend fun sendVote(claimId: String, challenge: Boolean) {
        val playerId = localPlayerId()
        val claim = currentClaim ?: return
        if (claim.playerId == playerId || claim.claimId != claimId) {
            logDebug("Snack Stash vote ignored gameId=$gameId claimId=$claimId")
            return
        }
        if (claim.votedPlayerIds.contains(playerId)) {
            logDebug("Snack Stash duplicate vote ignored gameId=$gameId claimId=$claimId")
            return
        }

        logDebug("Sending Snack Stash vote gameId=$gameId playerId=$playerId claimId=$claimId challenge=$challenge")
        remoteActions.vote(playerId, claimId, challenge)
        markLocalVote(playerId)
        refreshGameState()
    }

    private suspend fun sendAcceptDoom() {
        val playerId = localPlayerId()
        val state = gameState()
        val canAccept =
            state != null &&
                state.resolvingDoomPlayerId == playerId &&
                !state.pendingDoomRequiresInsertion &&
                currentClaim == null

        if (!canAccept) {
            logDebug("Accept doom ignored gameId=$gameId canAccept=$canAccept")
            return
        }

        logDebug("Sending doom ack gameId=$gameId playerId=$playerId")
        sendAction("doom/ack", JSONObject().put("playerId", playerId))
        clearPendingDoomUi()
        refreshGameState()
        logDebug("Sending nextTurn after accepted doom gameId=$gameId")
        sendAction("nextTurn", JSONObject())
        refreshGameState()
    }

    private suspend fun sendDismissResolutionNotice() {
        val notice = dismissCurrentResolutionNotice()
        val shouldAdvance =
            notice?.outcome == SnackStashResolutionOutcome.CHEATER &&
                notice.claimingPlayerId == localPlayerId() &&
                isLocalPlayersTurn() &&
                gameState()?.resolvingDoomPlayerId == null &&
                gameState()?.pendingDoomRequiresInsertion == false

        if (!shouldAdvance) {
            val doomWasDefused =
                notice?.outcome == SnackStashResolutionOutcome.UNCHALLENGED ||
                    notice?.outcome == SnackStashResolutionOutcome.LEGITIMATE_CALL
            if (doomWasDefused) {
                refreshGameState()
            }
            return
        }

        logDebug("Sending nextTurn after Snack Stash cheat resolution gameId=$gameId")
        sendAction("nextTurn", JSONObject())
        refreshGameState()
    }

    private fun markLocalVote(playerId: String) {
        _pendingClaim.value = _pendingClaim.value.markLocalVote(playerId)
    }

    private fun dismissCurrentResolutionNotice(): SnackStashResolutionEvent? {
        val notice = _resolutionNotice.value
        _resolutionNotice.value = null
        return notice
    }
}

private fun SnackStashClaimEvent?.markLocalVote(playerId: String): SnackStashClaimEvent? {
    if (this == null || votedPlayerIds.contains(playerId)) return this
    return copy(
        votesReceived = (votesReceived + 1).coerceAtMost(votesRequired),
        votedPlayerIds = votedPlayerIds + playerId
    )
}
