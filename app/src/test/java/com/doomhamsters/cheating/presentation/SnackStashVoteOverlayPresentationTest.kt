package com.doomhamsters.cheating.presentation

import com.doomhamsters.cheating.SnackStashClaimEvent
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class SnackStashVoteOverlayPresentationTest {

    @Test
    fun `remote unvoted claim allows voting`() {
        val display = SnackStashVoteOverlayPresentation.display(
            claim = claim(playerId = "remote", votedPlayerIds = emptySet(), message = "Claims."),
            localPlayerId = "local"
        )

        assertEquals("claim-1", display.claimId)
        assertEquals("Claims.", display.message)
        assertEquals("1/3 votes in", display.progressText)
        assertEquals(SnackStashVoteAction.CAN_VOTE, display.action)
    }

    @Test
    fun `local claim waits for other votes`() {
        val display = SnackStashVoteOverlayPresentation.display(
            claim = claim(playerId = "local", votedPlayerIds = emptySet()),
            localPlayerId = "local"
        )

        assertEquals(SnackStashVoteAction.LOCAL_CLAIM, display.action)
    }

    @Test
    fun `already voted claim blocks duplicate vote`() {
        val display = SnackStashVoteOverlayPresentation.display(
            claim = claim(playerId = "remote", votedPlayerIds = setOf("local")),
            localPlayerId = "local"
        )

        assertEquals(SnackStashVoteAction.ALREADY_VOTED, display.action)
    }

    @Test
    fun `missing message falls back to player claim copy`() {
        val display = SnackStashVoteOverlayPresentation.display(
            claim = claim(playerId = "remote", playerName = "Mina", votedPlayerIds = emptySet(), message = null),
            localPlayerId = "local"
        )

        assertEquals("Mina claims Snack Stash.", display.message)
    }

    private fun claim(
        playerId: String,
        playerName: String = "Remote",
        votedPlayerIds: Set<String>,
        message: String? = null
    ): SnackStashClaimEvent {
        return SnackStashClaimEvent(
            claimId = "claim-1",
            playerId = playerId,
            playerName = playerName,
            votesRequired = 3,
            votesReceived = 1,
            votedPlayerIds = votedPlayerIds,
            message = message
        )
    }
}
