package com.doomhamsters.cheating.presentation

import com.doomhamsters.cards.displayName
import com.doomhamsters.cheating.SnackStashClaimEvent
import com.doomhamsters.model.Card
import com.doomhamsters.model.CardType

/** Prepared state for showing the Snack Stash claim confirmation dialog. */
data class SnackStashClaimDialogState(
    val selectedCard: Card,
    val copy: SnackStashClaimConfirmationCopy
)

/** Selected hand-card indexes used by GameBoard while resolving a Snack Stash claim. */
data class SnackStashHandSelectionState(
    val selectedPlayerCardIndex: Int,
    val claimConfirmationCardIndex: Int
)

/** Text shown by the Snack Stash claim confirmation dialog. */
data class SnackStashClaimConfirmationCopy(
    val title: String,
    val message: String,
    val confirmLabel: String
)

/** Builds dialog state, copy, and hand-selection state for the claim confirmation UI. */
object SnackStashClaimConfirmationDialogPresentation {
    fun state(
        hand: List<Card>,
        selectedCardIndex: Int,
        pendingDoomRequiresSelection: Boolean,
        pendingClaim: SnackStashClaimEvent?
    ): SnackStashClaimDialogState? {
        val selectedCard = selectedClaimCard(
            hand = hand,
            cardIndex = selectedCardIndex,
            pendingDoomRequiresSelection = pendingDoomRequiresSelection,
            pendingClaim = pendingClaim
        ) ?: return null

        return SnackStashClaimDialogState(
            selectedCard = selectedCard,
            copy = copyFor(selectedCard)
        )
    }

    fun handSelectionAfterToggle(
        currentSelectedCardIndex: Int,
        toggledCardIndex: Int,
        pendingDoomRequiresSelection: Boolean,
        pendingClaim: SnackStashClaimEvent?
    ): SnackStashHandSelectionState {
        val nextSelectedCardIndex =
            if (currentSelectedCardIndex == toggledCardIndex) -1 else toggledCardIndex

        return handSelectionForCardIndex(
            selectedCardIndex = nextSelectedCardIndex,
            pendingDoomRequiresSelection = pendingDoomRequiresSelection,
            pendingClaim = pendingClaim
        )
    }

    fun clearedHandSelection(): SnackStashHandSelectionState {
        return SnackStashHandSelectionState(
            selectedPlayerCardIndex = -1,
            claimConfirmationCardIndex = -1
        )
    }

    fun copyFor(card: Card): SnackStashClaimConfirmationCopy {
        if (card.type == CardType.SnackStash) {
            return SnackStashClaimConfirmationCopy(
                title = "Use actual Snack Stash?",
                message = "Use this Snack Stash to block Doom. Other players can still challenge it.",
                confirmLabel = "Use Snack Stash"
            )
        }

        return SnackStashClaimConfirmationCopy(
            title = "Claim Snack Stash?",
            message = "Claim ${card.displayName()} as Snack Stash. "
                + "Other players will vote on whether they believe you.",
            confirmLabel = "Confirm Claim"
        )
    }

    private fun handSelectionForCardIndex(
        selectedCardIndex: Int,
        pendingDoomRequiresSelection: Boolean,
        pendingClaim: SnackStashClaimEvent?
    ): SnackStashHandSelectionState {
        return SnackStashHandSelectionState(
            selectedPlayerCardIndex = selectedCardIndex,
            claimConfirmationCardIndex = confirmationCardIndex(
                selectedCardIndex = selectedCardIndex,
                pendingDoomRequiresSelection = pendingDoomRequiresSelection,
                pendingClaim = pendingClaim
            )
        )
    }

    private fun confirmationCardIndex(
        selectedCardIndex: Int,
        pendingDoomRequiresSelection: Boolean,
        pendingClaim: SnackStashClaimEvent?
    ): Int {
        return if (selectedCardIndex >= 0 && canOpenClaimDialog(pendingDoomRequiresSelection, pendingClaim)) {
            selectedCardIndex
        } else {
            -1
        }
    }

    private fun selectedClaimCard(
        hand: List<Card>,
        cardIndex: Int,
        pendingDoomRequiresSelection: Boolean,
        pendingClaim: SnackStashClaimEvent?
    ): Card? {
        if (!canOpenClaimDialog(pendingDoomRequiresSelection, pendingClaim)) {
            return null
        }

        return hand.getOrNull(cardIndex)
    }

    private fun canOpenClaimDialog(
        pendingDoomRequiresSelection: Boolean,
        pendingClaim: SnackStashClaimEvent?
    ): Boolean {
        return pendingDoomRequiresSelection && pendingClaim == null
    }
}
