package com.doomhamsters.cheating.presentation

import com.doomhamsters.cheating.SnackStashLifeChangeEvent
import com.doomhamsters.cheating.SnackStashResolutionEvent
import com.doomhamsters.cheating.SnackStashResolutionOutcome
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class SnackStashResolutionOverlayPresentationTest {

    @Test
    fun `cheater resolution uses doom tone and life change lines`() {
        val display = SnackStashResolutionOverlayPresentation.display(
            resolution = resolution(
                outcome = SnackStashResolutionOutcome.CHEATER,
                message = "Caught.",
                lifeChanges = listOf(lifeChange(playerName = "Local", playerId = "local"))
            )
        )

        assertEquals("CHEATER", display.title)
        assertEquals(SnackStashResolutionTone.DOOM, display.tone)
        assertEquals("Caught.", display.message)
        assertEquals(listOf("Local: 3 -> 2 lives"), display.lifeChangeLines)
    }

    @Test
    fun `legitimate call uses snack stash tone`() {
        val display = SnackStashResolutionOverlayPresentation.display(
            resolution = resolution(
                outcome = SnackStashResolutionOutcome.LEGITIMATE_CALL,
                message = null,
                lifeChanges = emptyList()
            )
        )

        assertEquals("LEGITIMATE_CALL", display.title)
        assertEquals(SnackStashResolutionTone.SNACK_STASH, display.tone)
        assertEquals("Snack Stash claim resolved.", display.message)
    }

    @Test
    fun `unchallenged uses accent tone`() {
        val display = SnackStashResolutionOverlayPresentation.display(
            resolution = resolution(
                outcome = SnackStashResolutionOutcome.UNCHALLENGED,
                message = "Safe.",
                lifeChanges = emptyList()
            )
        )

        assertEquals("UNCHALLENGED", display.title)
        assertEquals(SnackStashResolutionTone.ACCENT, display.tone)
    }

    @Test
    fun `life change line falls back to player id then generic player`() {
        val display = SnackStashResolutionOverlayPresentation.display(
            resolution = resolution(
                outcome = SnackStashResolutionOutcome.CHEATER,
                message = null,
                lifeChanges = listOf(
                    lifeChange(playerName = null, playerId = "player-2"),
                    lifeChange(playerName = null, playerId = null)
                )
            )
        )

        assertEquals(
            listOf("player-2: 3 -> 2 lives", "Player: 3 -> 2 lives"),
            display.lifeChangeLines
        )
    }

    private fun resolution(
        outcome: SnackStashResolutionOutcome,
        message: String?,
        lifeChanges: List<SnackStashLifeChangeEvent>
    ): SnackStashResolutionEvent {
        return SnackStashResolutionEvent(
            outcome = outcome,
            claimId = "claim-1",
            claimingPlayerId = "local",
            claimingPlayerName = "Local",
            accusingPlayerIds = emptySet(),
            lifeChanges = lifeChanges,
            affectedPlayerId = "local",
            affectedPlayerName = "Local",
            livesBefore = 3,
            livesAfter = 2,
            doomDefused = false,
            message = message
        )
    }

    private fun lifeChange(
        playerName: String?,
        playerId: String?
    ): SnackStashLifeChangeEvent {
        return SnackStashLifeChangeEvent(
            playerId = playerId,
            playerName = playerName,
            livesBefore = 3,
            livesAfter = 2
        )
    }
}
