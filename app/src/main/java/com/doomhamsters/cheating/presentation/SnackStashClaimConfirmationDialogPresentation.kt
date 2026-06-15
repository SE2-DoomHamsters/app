package com.doomhamsters.cheating.presentation

import com.doomhamsters.cards.displayName
import com.doomhamsters.model.Card
import com.doomhamsters.model.CardType

data class SnackStashClaimConfirmationCopy(
    val title: String,
    val message: String,
    val confirmLabel: String
)

object SnackStashClaimConfirmationDialogPresentation {
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
}
