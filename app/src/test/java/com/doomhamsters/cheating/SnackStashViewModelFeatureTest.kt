package com.doomhamsters.cheating

import com.doomhamsters.model.Card
import com.doomhamsters.model.CardType
import com.doomhamsters.model.GameState
import com.doomhamsters.model.Player
import com.doomhamsters.model.Status
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.json.JSONArray
import org.json.JSONObject
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SnackStashViewModelFeatureTest {

    @Test
    fun `local claim event stores pending claim and requests waiting UI`() = runTest {
        val fixture = snackStashFixture()

        val result = fixture.feature.handlePublicEvent(
            claimEvent(playerId = "player-1", playerName = "Local", message = null)
        )

        assertEquals(true, result.handled)
        assertSame(SnackStashUiEffect.WaitingForVotes, result.effect)
        assertEquals("Local claims Snack Stash.", result.logMessage)
        assertEquals("claim-1", fixture.feature.pendingClaim.value?.claimId)
    }

    @Test
    fun `resolution event clears claim and stores resolution notice`() = runTest {
        val fixture = snackStashFixture()
        fixture.feature.handlePublicEvent(claimEvent(playerId = "player-2", playerName = "Remote"))

        val result = fixture.feature.handlePublicEvent(
            resolutionEvent(
                outcome = "CHEATER",
                claimingPlayerId = "player-1",
                message = null
            )
        )

        assertEquals(true, result.handled)
        assertSame(SnackStashUiEffect.ClearDoomSelection, result.effect)
        assertEquals("Snack Stash claim resolved: CHEATER.", result.logMessage)
        assertNull(fixture.feature.pendingClaim.value)
        assertEquals(SnackStashResolutionOutcome.CHEATER, fixture.feature.resolutionNotice.value?.outcome)
    }

    @Test
    fun `sync from game state restores and clears pending claim`() = runTest {
        val fixture = snackStashFixture()
        val pendingClaim = claimModel(playerId = "player-1")

        val effect = fixture.feature.syncFromGameState(
            gameState(
                currentPlayerId = "player-1",
                resolvingDoomPlayerId = "player-1",
                pendingClaim = pendingClaim
            )
        )

        assertSame(SnackStashUiEffect.WaitingForVotes, effect)
        assertEquals("claim-1", fixture.feature.pendingClaim.value?.claimId)

        fixture.feature.syncFromGameState(gameState(currentPlayerId = "player-1"))

        assertNull(fixture.feature.pendingClaim.value)
    }

    @Test
    fun `claim sends selected card and shows waiting state`() = runTest {
        val fixture = snackStashFixture(
            state = gameState(
                currentPlayerId = "player-1",
                resolvingDoomPlayerId = "player-1"
            ),
            pendingDoomRequiresSelection = true
        )

        fixture.feature.claim(Card(CardType.Normal, id = "card-1"))
        advanceUntilIdle()

        assertEquals("snack-stash/claim", fixture.sentActions.single().action)
        assertEquals("player-1", fixture.sentActions.single().payload.optString("playerId"))
        assertEquals("card-1", fixture.sentActions.single().payload.optString("cardId"))
        assertEquals(1, fixture.waitingForVotesCount)
        assertEquals(1, fixture.refreshCount)
    }

    @Test
    fun `vote sends challenge and marks local player voted`() = runTest {
        val fixture = snackStashFixture()
        fixture.feature.handlePublicEvent(claimEvent(playerId = "player-2", playerName = "Remote"))

        fixture.feature.vote("claim-1", challenge = true)
        advanceUntilIdle()

        assertEquals("snack-stash/vote", fixture.sentActions.single().action)
        assertEquals("player-1", fixture.sentActions.single().payload.optString("playerId"))
        assertEquals("claim-1", fixture.sentActions.single().payload.optString("claimId"))
        assertEquals("NO", fixture.sentActions.single().payload.optString("vote"))
        assertTrue(fixture.feature.pendingClaim.value?.votedPlayerIds?.contains("player-1") == true)
        assertEquals(1, fixture.refreshCount)
    }

    @Test
    fun `accept doom sends ack then next turn and refreshes around both actions`() = runTest {
        val fixture = snackStashFixture(
            state = gameState(
                currentPlayerId = "player-1",
                resolvingDoomPlayerId = "player-1"
            )
        )

        fixture.feature.acceptDoom()
        advanceUntilIdle()

        assertEquals(listOf("doom/ack", "nextTurn"), fixture.sentActions.map { it.action })
        assertEquals("player-1", fixture.sentActions.first().payload.optString("playerId"))
        assertEquals(1, fixture.clearDoomUiCount)
        assertEquals(2, fixture.refreshCount)
    }

    @Test
    fun `dismiss local cheater resolution advances turn once`() = runTest {
        val fixture = snackStashFixture(
            state = gameState(currentPlayerId = "player-1"),
            isLocalPlayersTurn = true
        )
        fixture.feature.handlePublicEvent(
            resolutionEvent(
                outcome = "CHEATER",
                claimingPlayerId = "player-1",
                message = "Caught."
            )
        )

        fixture.feature.dismissResolutionNotice()
        advanceUntilIdle()

        assertEquals(listOf("nextTurn"), fixture.sentActions.map { it.action })
        assertEquals(1, fixture.refreshCount)
        assertNull(fixture.feature.resolutionNotice.value)
    }

    @Test
    fun `dismiss non advancing resolution only clears notice`() = runTest {
        val fixture = snackStashFixture(
            state = gameState(currentPlayerId = "player-1"),
            isLocalPlayersTurn = true
        )
        fixture.feature.handlePublicEvent(
            resolutionEvent(
                outcome = "LEGITIMATE_CALL",
                claimingPlayerId = "player-1",
                message = "Legit."
            )
        )

        fixture.feature.dismissResolutionNotice()
        advanceUntilIdle()

        assertEquals(emptyList<String>(), fixture.sentActions.map { it.action })
        assertEquals(1, fixture.refreshCount)
        assertNull(fixture.feature.resolutionNotice.value)
    }

    private fun TestScope.snackStashFixture(
        state: GameState = gameState(currentPlayerId = "player-1"),
        isLocalPlayersTurn: Boolean = false,
        pendingDoomRequiresSelection: Boolean = false
    ): SnackStashFixture {
        val fixture = SnackStashFixture()
        var currentState = state
        fixture.feature = SnackStashViewModelFeature(
            gameId = "game-1",
            localPlayerId = { "player-1" },
            isLocalPlayersTurn = { isLocalPlayersTurn },
            pendingDoomRequiresSelection = { pendingDoomRequiresSelection },
            gameState = { currentState },
            sendAction = { action, payload ->
                fixture.sentActions += SentAction(action, payload)
            },
            refreshGameState = {
                fixture.refreshCount += 1
                currentState = state
            },
            showWaitingForVotes = {
                fixture.waitingForVotesCount += 1
            },
            clearPendingDoomUi = {
                fixture.clearDoomUiCount += 1
            },
            launchAction = { _, action ->
                this@snackStashFixture.launch {
                    action()
                }
            },
            logDebug = {
                fixture.logs += it
            }
        )
        return fixture
    }

    private class SnackStashFixture {
        lateinit var feature: SnackStashViewModelFeature
        val sentActions = mutableListOf<SentAction>()
        val logs = mutableListOf<String>()
        var refreshCount = 0
        var waitingForVotesCount = 0
        var clearDoomUiCount = 0
    }

    private data class SentAction(
        val action: String,
        val payload: JSONObject
    )

    private fun claimEvent(
        playerId: String,
        playerName: String,
        message: String? = "Claims."
    ): JSONObject {
        return JSONObject()
            .put("type", "SNACK_STASH_CLAIM_PENDING")
            .put("claimId", "claim-1")
            .put("playerId", playerId)
            .put("playerName", playerName)
            .put("votesRequired", 2)
            .put("votesReceived", 0)
            .put("votedPlayerIds", JSONArray())
            .put("message", message)
    }

    private fun resolutionEvent(
        outcome: String,
        claimingPlayerId: String,
        message: String?
    ): JSONObject {
        return JSONObject()
            .put("type", "SNACK_STASH_RESOLVED")
            .put("outcome", outcome)
            .put("claimId", "claim-1")
            .put("claimingPlayerId", claimingPlayerId)
            .put("claimingPlayerName", "Local")
            .put("accusingPlayerIds", JSONArray())
            .put("lifeChanges", JSONArray())
            .put("affectedPlayerId", claimingPlayerId)
            .put("affectedPlayerName", "Local")
            .put("livesBefore", 3)
            .put("livesAfter", 2)
            .put("doomDefused", false)
            .put("message", message)
    }

    private fun claimModel(playerId: String): SnackStashClaimEvent {
        return SnackStashClaimEvent(
            claimId = "claim-1",
            playerId = playerId,
            playerName = "Local",
            votesRequired = 2,
            votesReceived = 0,
            votedPlayerIds = emptySet(),
            message = "Claims."
        )
    }

    private fun gameState(
        currentPlayerId: String,
        resolvingDoomPlayerId: String? = null,
        pendingDoomRequiresInsertion: Boolean = false,
        pendingClaim: SnackStashClaimEvent? = null
    ): GameState {
        return GameState(
            id = "game-1",
            players = arrayListOf(
                Player(id = "player-1", lives = 3, name = "Local", handSizeHint = 1),
                Player(id = "player-2", lives = 3, name = "Remote", handSizeHint = 1)
            ),
            currentPlayerIndex = 0,
            status = Status.Playing,
            currentPlayerId = currentPlayerId,
            turnCount = 1,
            remainingDeckSize = 30,
            resolvingDoomPlayerId = resolvingDoomPlayerId,
            pendingDoomRequiresInsertion = pendingDoomRequiresInsertion,
            pendingSnackStashClaim = pendingClaim
        )
    }
}
