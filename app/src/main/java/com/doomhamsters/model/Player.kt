package com.doomhamsters.model

import org.json.JSONArray
import org.json.JSONObject

class Player(
    val id: String,
    var lives: Int
) {
    val hand = ArrayList<Card>()
    fun isAlive(): Boolean = lives > 0

    fun toJson(): JSONObject = JSONObject().apply {
        put("id", id)
        put("lives", lives)
        val handArray = JSONArray()
        hand.forEach { handArray.put(it.toJson()) }
        put("hand", handArray)
    }

    companion object {
        fun fromJson(json: JSONObject): Player {
            val p = Player(json.getString("id"), json.getInt("lives"))
            val handArray = json.getJSONArray("hand")
            for (i in 0 until handArray.length()) {
                p.hand.add(Card.fromJson(handArray.getJSONObject(i)))
            }
            return p
        }
    }
}