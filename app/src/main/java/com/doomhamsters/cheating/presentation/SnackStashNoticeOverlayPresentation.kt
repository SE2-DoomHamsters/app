package com.doomhamsters.cheating.presentation

import com.doomhamsters.cheating.SnackStashClaimEvent
import com.doomhamsters.cheating.SnackStashResolutionEvent

enum class SnackStashNoticeKind {
    NONE,
    RESOLUTION,
    VOTE
}

data class SnackStashNoticeState(
    val kind: SnackStashNoticeKind,
    val vote: SnackStashVoteDisplay? = null,
    val resolution: SnackStashResolutionDisplay? = null
)

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
