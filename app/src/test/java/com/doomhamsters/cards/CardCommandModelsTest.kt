package com.doomhamsters.cards

import com.doomhamsters.model.Card
import com.doomhamsters.model.CardType
import org.json.JSONArray
import org.json.JSONObject
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class CardCommandModelsTest {

    // region CardCommandRequest.toJson()

    @Test
    fun `toJson includes all fields when cardId and parameters are present`() {
        val request = CardCommandRequest(
            playerId = "player1",
            cardId = "card42",
            cardType = CardType.PowerNap,
            commandId = CardCommandId.POWER_NAP,
            parameters = mapOf("key" to "value")
        )
        val json = request.toJson()

        assertEquals("player1", json.getString("playerId"))
        assertEquals("card42", json.getString("cardId"))
        assertEquals("PowerNap", json.getString("cardType"))
        assertEquals("POWER_NAP", json.getString("commandId"))
        assertEquals("value", json.getJSONObject("parameters").getString("key"))
    }

    @Test
    fun `toJson omits cardId when null`() {
        val request = CardCommandRequest(
            playerId = "player1",
            cardId = null,
            cardType = CardType.Doom,
            commandId = CardCommandId.POWER_NAP
        )
        assertFalse(request.toJson().has("cardId"))
    }

    @Test
    fun `toJson omits parameters when empty`() {
        val request = CardCommandRequest(
            playerId = "player1",
            cardId = "c1",
            cardType = CardType.Doom,
            commandId = CardCommandId.POWER_NAP,
            parameters = emptyMap()
        )
        assertFalse(request.toJson().has("parameters"))
    }

    // endregion

    // region CardCommandEvent.fromJsonOrNull()

    @Test
    fun `fromJsonOrNull returns null for unknown type`() {
        val json = JSONObject().apply { put("type", "UNKNOWN_TYPE") }
        assertNull(CardCommandEvent.fromJsonOrNull(json))
    }

    @Test
    fun `fromJsonOrNull returns null when type is missing`() {
        assertNull(CardCommandEvent.fromJsonOrNull(JSONObject()))
    }

    @Test
    fun `fromJsonOrNull parses CARD_COMMAND_PLAYED event`() {
        val json = JSONObject().apply {
            put("type", "CARD_COMMAND_PLAYED")
            put("playerId", "p1")
            put("playerName", "Alice")
            put("commandId", "POWER_NAP")
        }
        val event = CardCommandEvent.fromJsonOrNull(json)

        assertNotNull(event)
        assertEquals(CardCommandEventType.CARD_COMMAND_PLAYED, event!!.type)
        assertEquals("p1", event.playerId)
        assertEquals("Alice", event.playerName)
        assertEquals(CardCommandId.POWER_NAP, event.commandId)
    }

    @Test
    fun `fromJsonOrNull parses CARD_COMMAND_RESULT event`() {
        val json = JSONObject().apply {
            put("type", "CARD_COMMAND_RESULT")
            put("message", "success")
        }
        val event = CardCommandEvent.fromJsonOrNull(json)

        assertNotNull(event)
        assertEquals(CardCommandEventType.CARD_COMMAND_RESULT, event!!.type)
        assertEquals("success", event.message)
    }

    @Test
    fun `fromJsonOrNull falls back to effectId when commandId is blank`() {
        val json = JSONObject().apply {
            put("type", "CARD_COMMAND_RESULT")
            put("commandId", "")
            put("effectId", "QUICK_PEEK")
        }
        val event = CardCommandEvent.fromJsonOrNull(json)

        assertEquals(CardCommandId.QUICK_PEEK, event!!.commandId)
    }

    @Test
    fun `fromJsonOrNull parses revealedCards array`() {
        val cardJson = JSONObject().apply { put("type", "PowerNap") }
        val json = JSONObject().apply {
            put("type", "CARD_COMMAND_RESULT")
            put("revealedCards", JSONArray().apply { put(cardJson) })
        }
        val event = CardCommandEvent.fromJsonOrNull(json)

        assertEquals(1, event!!.revealedCards.size)
        assertEquals(CardType.PowerNap, event.revealedCards[0].type)
    }

    @Test
    fun `fromJsonOrNull parses card and revealedCard fields`() {
        val cardJson = JSONObject().apply { put("type", "Doom") }
        val json = JSONObject().apply {
            put("type", "CARD_COMMAND_RESULT")
            put("card", cardJson)
            put("revealedCard", cardJson)
        }
        val event = CardCommandEvent.fromJsonOrNull(json)

        assertEquals(CardType.Doom, event!!.card!!.type)
        assertEquals(CardType.Doom, event.revealedCard!!.type)
    }

    @Test
    fun `fromJsonOrNull treats blank and null-string fields as null`() {
        val json = JSONObject().apply {
            put("type", "CARD_COMMAND_PLAYED")
            put("playerId", "  ")
            put("playerName", "null")
            put("message", "")
        }
        val event = CardCommandEvent.fromJsonOrNull(json)

        assertNull(event!!.playerId)
        assertNull(event.playerName)
        assertNull(event.message)
    }

    @Test
    fun `fromJsonOrNull defaults to empty list when revealedCards is absent`() {
        val json = JSONObject().apply { put("type", "CARD_COMMAND_PLAYED") }
        assertTrue(CardCommandEvent.fromJsonOrNull(json)!!.revealedCards.isEmpty())
    }

    // endregion

    // region CardCommandNotice

    @Test
    fun `CardCommandNotice holds title and message with defaults`() {
        val notice = CardCommandNotice(title = "Win", message = "You won!")

        assertEquals("Win", notice.title)
        assertEquals("You won!", notice.message)
        assertNull(notice.revealedCard)
        assertTrue(notice.revealedCards.isEmpty())
    }

    @Test
    fun `CardCommandNotice stores revealedCard when provided`() {
        val card = Card(CardType.PowerNap, id = "c1")
        val notice = CardCommandNotice(title = "Peek", message = "You saw a card", revealedCard = card)

        assertEquals(card, notice.revealedCard)
    }

    // endregion
}
