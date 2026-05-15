package com.doomhamsters.model
import org.json.JSONArray
import org.json.JSONObject
import java.io.Serializable

enum class Status {
    Lobby,
    Playing,
    Finished
}

data class GameState(
    val id: String,
    val players: ArrayList<Player>,
    val deck: Deck,
    val discard: Discard,
    var currentPlayerIndex: Int,
    var status: Status
)  {

    fun toJson(): JSONObject {
        return JSONObject().apply {
            put("id", id)
            put("players", JSONArray().apply { players.forEach { put(it.toJson()) } })
            put("deck", deck.toJson())
            put("discard", discard.toJson())
            put("currentPlayerIndex", currentPlayerIndex)
            put("status", status.name)
        }
    }

    companion object {
        fun fromJson(json: JSONObject): GameState {
            val playersList = ArrayList<Player>()
            val playersArray = json.getJSONArray("players")
            for (i in 0 until playersArray.length()) {
                playersList.add(Player.fromJson(playersArray.getJSONObject(i)))
            }

            return GameState(
                id = json.getString("id"),
                players = playersList,
                deck = Deck.fromJson(json.getJSONArray("deck")),
                discard = Discard.fromJson(json.getJSONArray("discard")),
                currentPlayerIndex = json.getInt("currentPlayerIndex"),
                status = Status.valueOf(json.getString("status"))
            )
        }
    }
}