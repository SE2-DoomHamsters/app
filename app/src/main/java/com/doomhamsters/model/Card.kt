package com.doomhamsters.model



import org.json.JSONObject

enum class CardType {
    Doom,
    SnackStash,
    PowerNap,
    QuickPeek,
    Normal;

    companion object {
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

data class Card(
    val type: CardType,
    val id: String? = null,
    val name: String? = null,
    val effectId: String? = null
) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("type", type.name)
        id?.let { put("id", it) }
        name?.let { put("name", it) }
        effectId?.let { put("effectId", it) }
    }

    companion object {
        private fun JSONObject.optNullableString(key: String): String? =
            optString(key).takeUnless { it.isBlank() || it.equals("null", ignoreCase = true) }

        fun fromJson(json: JSONObject) = Card(
            type = CardType.fromWire(json.getString("type")),
            id = json.optNullableString("id"),
            name = json.optNullableString("name"),
            effectId = json.optNullableString("effectId") ?: json.optNullableString("commandId")
        )
    }
}
