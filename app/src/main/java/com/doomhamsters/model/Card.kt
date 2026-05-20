package com.doomhamsters.model



import org.json.JSONObject

/** Lists the supported card types used by the game. */
enum class CardType {
    Doom,
    SnackStash,
    PowerNap,
    QuickPeek,
    Normal;

    companion object {
        /** Maps backend card type strings to a known card type. */
        fun fromWire(value: String): CardType = when (value.trim().uppercase()) {
            "DOOM" -> Doom
            "SNACK_STASH", "SNACKSTASH" -> SnackStash
            "POWER_NAP", "POWERNAP" -> PowerNap
            "QUICK_PEEK", "QUICKPEEK" -> QuickPeek
            "NORMAL" -> Normal
            else -> runCatching { valueOf(value) }.getOrDefault(Normal)
        }
    }
}

/** Represents a card instance exchanged between gameplay, UI, and backend layers. */
data class Card(
    val type: CardType,
    val id: String? = null,
    val name: String? = null,
    val effectId: String? = null
) {
    /** Serializes this card into the JSON format used by the backend. */
    fun toJson(): JSONObject = JSONObject().apply {
        put("type", type.name)
        id?.let { put("id", it) }
        name?.let { put("name", it) }
        effectId?.let { put("effectId", it) }
    }

    companion object {
        private fun JSONObject.optNullableString(key: String): String? =
            optString(key).takeUnless { it.isBlank() || it.equals("null", ignoreCase = true) }

        /** Builds a card from a backend JSON object. */
        fun fromJson(json: JSONObject) = Card(
            type = CardType.fromWire(json.getString("type")),
            id = json.optNullableString("id"),
            name = json.optNullableString("name"),
            effectId = json.optNullableString("effectId") ?: json.optNullableString("commandId")
        )
    }
}
