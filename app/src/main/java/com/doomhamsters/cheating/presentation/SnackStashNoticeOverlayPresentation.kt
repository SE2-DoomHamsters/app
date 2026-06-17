package com.doomhamsters.cheating.presentation

import com.doomhamsters.cheating.SnackStashClaimEvent
import com.doomhamsters.cheating.SnackStashResolutionEvent

/** Public Snack Stash notice variant currently shown by the board overlay layer. */
enum class SnackStashNoticeKind {
    NONE,
    RESOLUTION,
    VOTE
}

/** Prepared state for routing to the Snack Stash vote or resolution overlay. */
data class SnackStashNoticeState(
    val kind: SnackStashNoticeKind,
    val vote: SnackStashVoteDisplay? = null,
    val resolution: SnackStashResolutionDisplay? = null
)

/** Chooses which Snack Stash public notice overlay should be rendered. */
object SnackStashNoticeOverlayPresentation {
    fun state(
        pendingClaim: SnackStashClaimEvent?,
        resolution: SnackStashResolutionEvent?,
        localPlayerId: String
    ): SnackStashNoticeState {
        return when {
            resolution != null -> SnackStashNoticeState(
                kind = SnackStashNoticeKind.RESOLUTION,
                resolution = SnackStashResolutionOverlayPresentation.display(resolution)
            )

            pendingClaim != null -> SnackStashNoticeState(
                kind = SnackStashNoticeKind.VOTE,
                vote = SnackStashVoteOverlayPresentation.display(
                    claim = pendingClaim,
                    localPlayerId = localPlayerId
                )
            )

            else -> SnackStashNoticeState(kind = SnackStashNoticeKind.NONE)
        }
    }
}
