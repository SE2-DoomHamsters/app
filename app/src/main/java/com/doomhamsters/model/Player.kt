package com.doomhamsters.model

import org.json.JSONArray
import org.json.JSONObject

/** Represents a player within a game state snapshot. */
class Player(
    val id: String,
    var lives: Int,
    val name: String = id,
    val avatar: String = "",
    private val aliveFlag: Boolean? = null,
    private val handSizeHint: Int = 0
) {
    val hand = ArrayList<Card>()
    /** Returns whether the player is still alive. */
    fun isAlive(): Boolean = aliveFlag ?: (lives > 0)
    /** Returns the hand size that should be shown in the UI. */
    fun visibleHandSize(): Int = maxOf(hand.size, handSizeHint)

    /** Serializes this player into the JSON format used by the backend. */
    fun toJson(): JSONObject = JSONObject().apply {
        put("id", id)
        put("playerName", name)
        put("avatar", avatar)
        put("lives", lives)
        put("alive", isAlive())
        put("handSize", visibleHandSize())
        val handArray = JSONArray()
        hand.forEach { handArray.put(it.toJson()) }
        put("hand", handArray)
    }

    companion object {
        /** Builds a player from a backend JSON object. */
        fun fromJson(json: JSONObject): Player {
            val playerId = if (json.has("playerId")) json.getString("playerId") else json.getString("id")
            val handArray = json.optJSONArray("hand") ?: JSONArray()
            val rawName = json.optString("playerName")
                .ifBlank { json.optString("name") }
            val p = Player(
                id = playerId,
                lives = json.getInt("lives"),
                name = displayName(rawName, playerId),
                avatar = json.optString("avatar")
                    .ifBlank { json.optString("playerAvatar") },
                aliveFlag = json.optBoolean("alive").takeIf { json.has("alive") },
                handSizeHint = json.optInt("handSize", handArray.length())
            )
            for (i in 0 until handArray.length()) {
                p.hand.add(Card.fromJson(handArray.getJSONObject(i)))
            }
            return p
        }

        private fun displayName(rawName: String, playerId: String): String {
            val normalizedName = rawName.trim()
            if (normalizedName.isNotBlank() && normalizedName != playerId) {
                return normalizedName
            }
            return if (playerId.length > 12 || playerId.contains('-')) {
                "Player ${playerId.takeLast(4)}"
            } else {
                playerId
            }
        }
    }
}
