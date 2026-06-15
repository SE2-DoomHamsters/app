package com.doomhamsters.ui.cheating

import androidx.compose.runtime.Composable
import com.doomhamsters.cheating.presentation.SnackStashNoticeKind
import com.doomhamsters.cheating.presentation.SnackStashNoticeState

@Composable
fun SnackStashNoticeOverlay(
    state: SnackStashNoticeState,
    onVote: (String, Boolean) -> Unit,
    onResolutionDismiss: () -> Unit
) {
    when (state.kind) {
        SnackStashNoticeKind.RESOLUTION -> {
            state.resolution?.let { resolution ->
                SnackStashResolutionOverlay(
                    display = resolution,
                    onDismiss = onResolutionDismiss
                )
            }
        }

        SnackStashNoticeKind.VOTE -> {
            state.vote?.let { vote ->
                SnackStashVoteOverlay(
                    display = vote,
                    onVote = { choice -> onVote(vote.claimId, choice) }
                )
            }
        }

        SnackStashNoticeKind.NONE -> Unit
    }
}
