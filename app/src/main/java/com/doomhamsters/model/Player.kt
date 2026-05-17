package com.doomhamsters.model

import org.json.JSONArray
import org.json.JSONObject

class Player(
    val id: String,
    var lives: Int,
    val name: String = id,
    val avatar: String = "",
    private val aliveFlag: Boolean? = null,
    private val handSizeHint: Int = 0
) {
    val hand = ArrayList<Card>()
    fun isAlive(): Boolean = aliveFlag ?: (lives > 0)
    fun visibleHandSize(): Int = maxOf(hand.size, handSizeHint)

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
        fun fromJson(json: JSONObject): Player {
            val playerId = if (json.has("playerId")) json.getString("playerId") else json.getString("id")
            val handArray = json.optJSONArray("hand") ?: JSONArray()
            val p = Player(
                id = playerId,
                lives = json.getInt("lives"),
                name = json.optString("playerName").ifBlank { playerId },
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
    }
}
