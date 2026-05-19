package com.doomhamsters.cards


import com.doomhamsters.model.Card
import com.doomhamsters.model.CardType
import org.json.JSONObject

data class CardCommandRequest(
    val playerId: String,
    val cardId: String?,
    val cardType: CardType,
    val commandId: CardCommandId,
    val parameters: JSONObject = JSONObject()
) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("playerId", playerId)
        cardId?.let { put("cardId", it) }
        put("cardType", cardType.name)
        put("commandId", commandId.name)
        put("parameters", parameters)
    }
}

enum class CardCommandEventType {
    CARD_COMMAND_PLAYED,
    CARD_COMMAND_RESULT
}

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

data class CardCommandNotice(
    val title: String,
    val message: String,
    val revealedCard: Card? = null
)
