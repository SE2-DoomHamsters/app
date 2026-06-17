package com.doomhamsters.cheating.presentation

import com.doomhamsters.cheating.SnackStashClaimEvent
import com.doomhamsters.cheating.SnackStashResolutionEvent
import com.doomhamsters.cheating.SnackStashResolutionOutcome
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class SnackStashNoticeOverlayPresentationTest {

    @Test
    fun `pending claim routes to vote notice`() {
        val state = SnackStashNoticeOverlayPresentation.state(
            pendingClaim = claim(),
            resolution = null,
            localPlayerId = "local"
        )

        assertEquals(SnackStashNoticeKind.VOTE, state.kind)
        assertEquals("claim-1", state.vote?.claimId)
        assertNull(state.resolution)
    }

    @Test
    fun `no claim or resolution routes to none`() {
        val state = SnackStashNoticeOverlayPresentation.state(
            pendingClaim = null,
            resolution = null,
            localPlayerId = "local"
        )

        assertEquals(SnackStashNoticeKind.NONE, state.kind)
        assertNull(state.vote)
        assertNull(state.resolution)
    }

    @Test
    fun `resolution routes to resolution notice and wins over pending claim`() {
        val state = SnackStashNoticeOverlayPresentation.state(
            pendingClaim = claim(),
            resolution = resolution(),
            localPlayerId = "local"
        )

        assertEquals(SnackStashNoticeKind.RESOLUTION, state.kind)
        assertEquals("CHEATER", state.resolution?.title)
        assertNull(state.vote)
    }

    private fun claim(): SnackStashClaimEvent {
        return SnackStashClaimEvent(
            claimId = "claim-1",
            playerId = "remote",
            playerName = "Player",
            votesRequired = 2,
            votesReceived = 0,
            votedPlayerIds = emptySet(),
            message = null
        )
    }

    private fun resolution(): SnackStashResolutionEvent {
        return SnackStashResolutionEvent(
            outcome = SnackStashResolutionOutcome.CHEATER,
            claimId = "claim-1",
            claimingPlayerId = "local",
            claimingPlayerName = "Local",
            accusingPlayerIds = emptySet(),
            lifeChanges = emptyList(),
            affectedPlayerId = "local",
            affectedPlayerName = "Local",
            livesBefore = 3,
            livesAfter = 2,
            doomDefused = false,
            message = "Caught."
        )
    }
}
