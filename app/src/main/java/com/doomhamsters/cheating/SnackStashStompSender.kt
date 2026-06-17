package com.doomhamsters.cheating

import org.json.JSONObject

/** Encapsulates the STOMP commands used by the Snack Stash claim/vote feature. */
class SnackStashRemoteActions(
    private val sendAction: suspend (action: String, payload: JSONObject) -> Unit
) {
    suspend fun claim(playerId: String, cardId: String) {
        sendAction(
            "snack-stash/claim",
            JSONObject()
                .put("playerId", playerId)
                .put("cardId", cardId)
        )
    }

    suspend fun vote(playerId: String, claimId: String, challenge: Boolean) {
        sendAction(
            "snack-stash/vote",
            JSONObject()
                .put("playerId", playerId)
                .put("claimId", claimId)
                .put("vote", if (challenge) "NO" else "YES")
        )
    }
}
