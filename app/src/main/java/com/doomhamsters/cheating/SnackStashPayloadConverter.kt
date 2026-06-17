package com.doomhamsters.cheating

import org.json.JSONArray
import org.json.JSONObject

/** Converts Snack Stash wire JSON into feature models, and feature models back to wire JSON. */
object SnackStashEventJson {
    fun claimFromJsonOrNull(json: JSONObject): SnackStashClaimEvent? {
        if (json.optString("type").trim().uppercase() != "SNACK_STASH_CLAIM_PENDING") {
            return null
        }

        val claimId = json.optString("claimId").takeIf { it.isNotBlank() } ?: return null
        val playerId = json.optString("playerId").takeIf { it.isNotBlank() } ?: return null
        val playerName = json.optString("playerName").takeIf { it.isNotBlank() } ?: playerId

        return SnackStashClaimEvent(
            claimId = claimId,
            playerId = playerId,
            playerName = playerName,
            votesRequired = json.optInt("votesRequired"),
            votesReceived = json.optInt("votesReceived"),
            votedPlayerIds = json.optStringSet("votedPlayerIds"),
            message = json.optNullableString("message")
        )
    }

    fun claimToJson(claim: SnackStashClaimEvent): JSONObject {
        return JSONObject()
            .put("type", "SNACK_STASH_CLAIM_PENDING")
            .put("claimId", claim.claimId)
            .put("playerId", claim.playerId)
            .put("playerName", claim.playerName)
            .put("votesRequired", claim.votesRequired)
            .put("votesReceived", claim.votesReceived)
            .put("votedPlayerIds", JSONArray().apply { claim.votedPlayerIds.forEach(::put) })
            .put("message", claim.message)
    }

    fun resolutionFromJsonOrNull(json: JSONObject): SnackStashResolutionEvent? {
        if (json.optString("type").trim().uppercase() != "SNACK_STASH_RESOLVED") {
            return null
        }

        val outcome = resolutionOutcomeFromWire(json.optString("outcome")) ?: return null
        return SnackStashResolutionEvent(
            outcome = outcome,
            claimId = json.optNullableString("claimId"),
            claimingPlayerId = json.optNullableString("claimingPlayerId"),
            claimingPlayerName = json.optNullableString("claimingPlayerName"),
            accusingPlayerIds = json.optStringSet("accusingPlayerIds"),
            lifeChanges = json.optLifeChanges("lifeChanges"),
            affectedPlayerId = json.optNullableString("affectedPlayerId"),
            affectedPlayerName = json.optNullableString("affectedPlayerName"),
            livesBefore = json.optInt("livesBefore"),
            livesAfter = json.optInt("livesAfter"),
            doomDefused = json.optBoolean("doomDefused"),
            message = json.optNullableString("message")
        )
    }

    private fun resolutionOutcomeFromWire(value: String): SnackStashResolutionOutcome? =
        runCatching { SnackStashResolutionOutcome.valueOf(value.trim().uppercase()) }.getOrNull()

    private fun JSONObject.optNullableString(key: String): String? =
        optString(key).takeUnless { it.isBlank() || it.equals("null", ignoreCase = true) }

    private fun JSONObject.optStringSet(key: String): Set<String> {
        val values = optJSONArray(key) ?: return emptySet()
        return buildSet {
            for (index in 0 until values.length()) {
                values.optString(index).takeIf { it.isNotBlank() }?.let(::add)
            }
        }
    }

    private fun JSONObject.optLifeChanges(key: String): List<SnackStashLifeChangeEvent> {
        val values = optJSONArray(key) ?: return emptyList()
        return buildList {
            for (index in 0 until values.length()) {
                values.optJSONObject(index)?.let { add(it.toLifeChangeEvent()) }
            }
        }
    }

    private fun JSONObject.toLifeChangeEvent(): SnackStashLifeChangeEvent =
        SnackStashLifeChangeEvent(
            playerId = optNullableString("playerId"),
            playerName = optNullableString("playerName"),
            livesBefore = optInt("livesBefore"),
            livesAfter = optInt("livesAfter")
        )
}
