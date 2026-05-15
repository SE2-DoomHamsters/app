package com.doomhamsters.model

import org.json.JSONObject

enum class CardType {
    Doom,
    SnackStash,
    Normal
}

data class Card(val type: CardType) {
    fun toJson(): JSONObject = JSONObject().put("type", type.name)
    companion object {
        fun fromJson(json: JSONObject) = Card(CardType.valueOf(json.getString("type")))
    }
}