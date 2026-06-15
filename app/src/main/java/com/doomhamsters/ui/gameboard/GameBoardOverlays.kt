package com.doomhamsters.ui.gameboard

import androidx.compose.runtime.Composable
import com.doomhamsters.cards.CardCommandNotice
import com.doomhamsters.cheating.presentation.SnackStashNoticeKind
import com.doomhamsters.cheating.presentation.SnackStashNoticeState
import com.doomhamsters.model.Card
import com.doomhamsters.model.Player
import com.doomhamsters.ui.cheating.SnackStashClaimPromptOverlay
import com.doomhamsters.ui.cheating.SnackStashNoticeOverlay

internal data class BoardOverlayState(
    val pendingDoom: Card?,
    val pendingDoomMessage: String?,
    val pendingDoomRequiresSelection: Boolean,
    val pendingDoomRequiresInsertionUi: Boolean,
    val pausedRemotePlayerName: String?,
    val pausedForDoomPlayerName: String?,
    val pausedForDoomMessage: String?,
    val pausedForDoomDetail: String?,
    val cardCommandNotice: CardCommandNotice?,
    val snackStashNoticeState: SnackStashNoticeState
)

internal data class BoardOverlayContext(
    val currentPlayer: Player,
    val deckSize: Int,
    val selectedPlayerCardIndex: Int,
    val doomSliderPosition: Float
)

internal data class BoardOverlayCallbacks(
    val onDoomSliderChange: (Float) -> Unit,
    val onDoomConfirmed: () -> Unit,
    val onDoomDismiss: () -> Unit,
    val onAcceptDoom: () -> Unit,
    val onCardCommandDismiss: () -> Unit,
    val onSnackStashVote: (String, Boolean) -> Unit,
    val onSnackStashResolutionDismiss: () -> Unit
)

@Composable
internal fun BoardOverlays(
    overlayState: BoardOverlayState,
    context: BoardOverlayContext,
    callbacks: BoardOverlayCallbacks
) {
    if (overlayState.snackStashNoticeState.kind != SnackStashNoticeKind.NONE) {
        SnackStashNoticeOverlay(
            state = overlayState.snackStashNoticeState,
            onVote = callbacks.onSnackStashVote,
            onResolutionDismiss = callbacks.onSnackStashResolutionDismiss
        )
    } else {
        when {
            overlayState.pendingDoom != null && overlayState.pendingDoomRequiresInsertionUi -> {
                DoomOverlay(
                    state = DoomOverlayState(
                        pendingDoom = overlayState.pendingDoom,
                        deckSize = context.deckSize,
                        currentPlayer = context.currentPlayer,
                        selectedPlayerCardIndex = context.selectedPlayerCardIndex,
                        hasDefused = true,
                        doomSliderPosition = context.doomSliderPosition
                    ),
                    onDefuseTriggered = {},
                    onDoomSliderChange = callbacks.onDoomSliderChange,
                    onDoomConfirmed = callbacks.onDoomConfirmed,
                    onAcceptDoom = {}
                )
            }

            overlayState.pendingDoom != null && overlayState.pendingDoomRequiresSelection -> {
                SnackStashClaimPromptOverlay(
                    pendingDoom = overlayState.pendingDoom,
                    message = overlayState.pendingDoomMessage
                        ?: "Use Snack Stash, bluff with another card, or accept Doom.",
                    onAcceptDoom = callbacks.onAcceptDoom
                )
            }

            overlayState.pendingDoom != null && overlayState.pendingDoomMessage != null -> {
                DoomResolvedOverlay(
                    card = overlayState.pendingDoom,
                    currentPlayer = context.currentPlayer,
                    selectedPlayerCardIndex = context.selectedPlayerCardIndex,
                    message = overlayState.pendingDoomMessage,
                    requiresSnackSelection = overlayState.pendingDoomRequiresSelection,
                    onDismiss = callbacks.onDoomDismiss
                )
            }

            overlayState.pausedForDoomPlayerName != null &&
                overlayState.pausedForDoomMessage != null &&
                overlayState.pausedForDoomDetail != null -> {
                RemoteDoomPauseOverlay(
                    playerName = overlayState.pausedRemotePlayerName ?: overlayState.pausedForDoomPlayerName,
                    message = overlayState.pausedForDoomMessage,
                    detail = overlayState.pausedForDoomDetail
                )
            }
        }
    }

    overlayState.cardCommandNotice?.let { notice ->
        CardCommandNoticeOverlay(
            notice = notice,
            onDismiss = callbacks.onCardCommandDismiss
        )
    }
}
