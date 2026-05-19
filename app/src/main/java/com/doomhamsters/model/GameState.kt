package com.doomhamsters.model
import org.json.JSONArray
import org.json.JSONObject

enum class Status {
    Lobby,
    Playing,
    Finished;

    companion object {
        fun fromWire(value: String): Status = when (value.trim().uppercase()) {
            "SETUP", "LOBBY" -> Lobby
            "RUNNING", "PLAYING" -> Playing
            "FINISHED" -> Finished
            else -> runCatching { valueOf(value) }.getOrDefault(Lobby)
        }
    }
}

data class GameState(
    val id: String,
    val players: ArrayList<Player>,
    val deck: Deck = Deck(),
    val discard: Discard = Discard(),
    var currentPlayerIndex: Int = 0,
    var status: Status,
    val currentPlayerId: String? = null,
    val turnCount: Int = 0,
    val remainingDeckSize: Int = 0,
    val resolvingDoomPlayerId: String? = null,
    val pendingDoomRequiresInsertion: Boolean = false,
    val pendingDoomCardId: String? = null
)  {
    val deckSize: Int
        get() = remainingDeckSize.takeIf { it >= 0 } ?: deck.size()

    val currentTurnPlayerId: String?
        get() = currentPlayerId ?: players.getOrNull(currentPlayerIndex)?.id

    fun toJson(): JSONObject {
        return JSONObject().apply {
            put("id", id)
            put("players", JSONArray().apply { players.forEach { put(it.toJson()) } })
            put("deck", deck.toJson())
            put("discard", discard.toJson())
            put("currentPlayerIndex", currentPlayerIndex)
            put("status", status.name)
            currentPlayerId?.let { put("currentPlayerId", it) }
            put("turnCount", turnCount)
            put("remainingDeckSize", remainingDeckSize)
            resolvingDoomPlayerId?.let { put("resolvingDoomPlayerId", it) }
            put("pendingDoomRequiresInsertion", pendingDoomRequiresInsertion)
            pendingDoomCardId?.let { put("pendingDoomCardId", it) }
        }
    }

    companion object {
        private fun JSONObject.optNullableString(key: String): String? =
            optString(key).takeUnless { it.isBlank() || it.equals("null", ignoreCase = true) }

        fun fromJson(json: JSONObject): GameState {
            if (json.has("gameId")) {
                return fromBackendJson(json)
            }

            val playersList = ArrayList<Player>()
            val playersArray = json.getJSONArray("players")
            for (i in 0 until playersArray.length()) {
                playersList.add(Player.fromJson(playersArray.getJSONObject(i)))
            }
            val parsedDeck = Deck.fromJson(json.getJSONArray("deck"))
            val parsedDiscard = Discard.fromJson(json.getJSONArray("discard"))

            return GameState(
                id = json.getString("id"),
                players = playersList,
                deck = parsedDeck,
                discard = parsedDiscard,
                currentPlayerIndex = json.getInt("currentPlayerIndex"),
                status = Status.fromWire(json.getString("status")),
                currentPlayerId = json.optNullableString("currentPlayerId"),
                turnCount = json.optInt("turnCount"),
                remainingDeckSize = json.optInt("remainingDeckSize", parsedDeck.size()),
                resolvingDoomPlayerId = json.optNullableString("resolvingDoomPlayerId"),
                pendingDoomRequiresInsertion = json.optBoolean("pendingDoomRequiresInsertion"),
                pendingDoomCardId = json.optNullableString("pendingDoomCardId")
            )
        }

        fun fromBackendJson(json: JSONObject): GameState {
            val playersList = ArrayList<Player>()
            val playersArray = json.getJSONArray("players")
            for (i in 0 until playersArray.length()) {
                playersList.add(Player.fromJson(playersArray.getJSONObject(i)))
            }

            val currentPlayerId = json.optNullableString("currentPlayerId")
            val currentPlayerIndex = playersList.indexOfFirst { it.id == currentPlayerId }
                .let { index -> if (index >= 0) index else 0 }

            return GameState(
                id = json.getString("gameId"),
                players = playersList,
                currentPlayerIndex = currentPlayerIndex,
                status = Status.fromWire(json.getString("gameState")),
                currentPlayerId = currentPlayerId,
                turnCount = json.optInt("turnCount"),
                remainingDeckSize = json.optInt("remainingDeckSize"),
                resolvingDoomPlayerId = json.optNullableString("resolvingDoomPlayerId"),
                pendingDoomRequiresInsertion = json.optBoolean("pendingDoomRequiresInsertion"),
                pendingDoomCardId = json.optNullableString("pendingDoomCardId")
            )
        }
    }
}
