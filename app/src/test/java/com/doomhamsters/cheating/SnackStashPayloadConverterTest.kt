package com.doomhamsters.cheating

import org.json.JSONArray
import org.json.JSONObject
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class SnackStashPayloadConverterTest {

    @Test
    fun `claim json round trip keeps vote progress and nullable message`() {
        val claim = SnackStashClaimEvent(
            claimId = "claim-1",
            playerId = "player-1",
            playerName = "Local",
            votesRequired = 2,
            votesReceived = 1,
            votedPlayerIds = setOf("player-2"),
            message = null
        )

        val parsed = SnackStashEventJson.claimFromJsonOrNull(SnackStashEventJson.claimToJson(claim))

        assertEquals(claim, parsed)
    }

    @Test
    fun `claim json falls back to player id when name is missing`() {
        val parsed = SnackStashEventJson.claimFromJsonOrNull(
            JSONObject()
                .put("type", "SNACK_STASH_CLAIM_PENDING")
                .put("claimId", "claim-1")
                .put("playerId", "player-1")
                .put("votesRequired", 2)
                .put("votesReceived", 0)
                .put("votedPlayerIds", JSONArray())
        )

        assertEquals("player-1", parsed?.playerName)
    }

    @Test
    fun `resolution json parses life changes and nullable wire fields`() {
        val parsed = SnackStashEventJson.resolutionFromJsonOrNull(
            JSONObject()
                .put("type", "SNACK_STASH_RESOLVED")
                .put("outcome", "cheater")
                .put("claimId", JSONObject.NULL)
                .put("claimingPlayerId", "player-1")
                .put("claimingPlayerName", JSONObject.NULL)
                .put("accusingPlayerIds", JSONArray().put("player-2").put(""))
                .put(
                    "lifeChanges",
                    JSONArray()
                        .put(
                            JSONObject()
                                .put("playerId", "player-1")
                                .put("playerName", JSONObject.NULL)
                                .put("livesBefore", 3)
                                .put("livesAfter", 2)
                        )
                )
                .put("affectedPlayerId", "player-1")
                .put("affectedPlayerName", "Local")
                .put("livesBefore", 3)
                .put("livesAfter", 2)
                .put("doomDefused", false)
                .put("message", "Caught cheating.")
        )

        assertEquals(SnackStashResolutionOutcome.CHEATER, parsed?.outcome)
        assertNull(parsed?.claimId)
        assertNull(parsed?.claimingPlayerName)
        assertEquals(setOf("player-2"), parsed?.accusingPlayerIds)
        assertEquals("player-1", parsed?.lifeChanges?.single()?.playerId)
        assertNull(parsed?.lifeChanges?.single()?.playerName)
        assertEquals(3, parsed?.lifeChanges?.single()?.livesBefore)
        assertEquals(2, parsed?.lifeChanges?.single()?.livesAfter)
        assertEquals(false, parsed?.doomDefused)
        assertEquals("Caught cheating.", parsed?.message)
    }

    @Test
    fun `non snack stash or malformed payloads are ignored`() {
        assertNull(SnackStashEventJson.claimFromJsonOrNull(JSONObject().put("type", "OTHER")))
        assertNull(
            SnackStashEventJson.claimFromJsonOrNull(
                JSONObject()
                    .put("type", "SNACK_STASH_CLAIM_PENDING")
                    .put("playerId", "player-1")
            )
        )
        assertNull(SnackStashEventJson.resolutionFromJsonOrNull(JSONObject().put("type", "OTHER")))
        assertNull(
            SnackStashEventJson.resolutionFromJsonOrNull(
                JSONObject()
                    .put("type", "SNACK_STASH_RESOLVED")
                    .put("outcome", "NOT_REAL")
            )
        )
    }

    @Test
    fun `resolution outcome exposes expected wire names`() {
        assertTrue(SnackStashResolutionOutcome.entries.map { it.name }.contains("UNCHALLENGED"))
        assertTrue(SnackStashResolutionOutcome.entries.map { it.name }.contains("CHEATER"))
        assertTrue(SnackStashResolutionOutcome.entries.map { it.name }.contains("LEGITIMATE_CALL"))
    }
}
