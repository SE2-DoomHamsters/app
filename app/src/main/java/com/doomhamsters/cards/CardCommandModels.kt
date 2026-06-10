package com.doomhamsters.cards


import com.doomhamsters.model.Card
import com.doomhamsters.model.CardType
import org.json.JSONObject

/** Represents the payload sent when a player activates a card command. */
data class CardCommandRequest(
    val playerId: String,
    val cardId: String?,
    val cardType: CardType,
    val commandId: CardCommandId,
    val parameters: Map<String, String> = emptyMap()
    val parameters: JSONObject = JSONObject()
) {
    /** Serializes this command request into the backend JSON format. */
    fun toJson(): JSONObject = JSONObject().apply {
        put("playerId", playerId)
        cardId?.let { put("cardId", it) }
        put("cardType", cardType.name)
        put("commandId", commandId.name)
        put("parameters", parameters)
    }
}

/** Enumerates the card-command event types emitted by the backend. */
enum class CardCommandEventType {
    CARD_COMMAND_PLAYED,
    CARD_COMMAND_RESULT
}

/** Represents a card-command event received from the backend. */
data class CardCommandEvent(
    val type: CardCommandEventType,
    val commandId: CardCommandId?,
    val playerId: String?,
    val playerName: String?,
    val message: String?,
    val card: Card?,
    val revealedCard: Card?
) {
    companion object {
        /** Parses a backend JSON payload into a card-command event when possible. */
        fun fromJsonOrNull(json: JSONObject): CardCommandEvent? {
            val type = runCatching {
                CardCommandEventType.valueOf(json.optString("type").trim().uppercase())
            }.getOrNull() ?: return null

            return CardCommandEvent(
                type = type,
                commandId = CardCommandId.fromWire(
                    json.optString("commandId").ifBlank {
                        json.optString("effectId")
                    }
                ),
                playerId = json.optNullableString("playerId"),
                playerName = json.optNullableString("playerName"),
                message = json.optNullableString("message"),
                card = json.optJSONObject("card")?.let(Card::fromJson),
                revealedCard = json.optJSONObject("revealedCard")?.let(Card::fromJson)
            )
        }

        private fun JSONObject.optNullableString(key: String): String? =
            optString(key).takeUnless { it.isBlank() || it.equals("null", ignoreCase = true) }
    }
}

/** Holds the message shown to a player after a card command resolves. */
data class CardCommandNotice(
    val title: String,
    val message: String,
    val revealedCard: Card? = null
)
