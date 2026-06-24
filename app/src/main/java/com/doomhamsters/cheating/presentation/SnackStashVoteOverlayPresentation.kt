package com.doomhamsters.cheating.presentation

import com.doomhamsters.cheating.SnackStashClaimEvent

/** Display-ready content for a pending Snack Stash vote overlay. */
data class SnackStashVoteDisplay(
    val claimId: String,
    val message: String,
    val progressText: String,
    val action: SnackStashVoteAction
)

/** Vote action available to the local player for the current claim. */
enum class SnackStashVoteAction {
    CAN_VOTE,
    LOCAL_CLAIM,
    ALREADY_VOTED
}

/** Maps a pending Snack Stash claim to display content for the vote overlay. */
object SnackStashVoteOverlayPresentation {
    fun display(
        claim: SnackStashClaimEvent,
        localPlayerId: String
    ): SnackStashVoteDisplay {
        val action = when {
            claim.playerId == localPlayerId -> SnackStashVoteAction.LOCAL_CLAIM
            claim.votedPlayerIds.contains(localPlayerId) -> SnackStashVoteAction.ALREADY_VOTED
            else -> SnackStashVoteAction.CAN_VOTE
        }

        return SnackStashVoteDisplay(
            claimId = claim.claimId,
            message = claim.message ?: "${claim.playerName} claims Snack Stash.",
            progressText = "${claim.votesReceived}/${claim.votesRequired} votes in",
            action = action
        )
    }
}
