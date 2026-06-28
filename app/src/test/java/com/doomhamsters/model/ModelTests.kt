package com.doomhamsters.model

import org.json.JSONArray
import org.json.JSONObject
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class ModelTests {

    // region Deck

    @Test
    fun `Deck shuffle keeps same number of cards`() {
        val deck = Deck()
        repeat(5) { deck.insertAt(Card(CardType.Normal), it) }
        deck.shuffle()
        assertEquals(5, deck.size())
    }

    @Test
    fun `Deck toJson serializes all cards`() {
        val deck = Deck()
        deck.insertAt(Card(CardType.Normal), 0)
        deck.insertAt(Card(CardType.Doom), 1)
        val json = deck.toJson()
        assertEquals(2, json.length())
    }

    @Test
    fun `Deck fromJson deserializes cards correctly`() {
        val arr = JSONArray().apply {
            put(JSONObject().apply { put("type", "Normal") })
            put(JSONObject().apply { put("type", "Doom") })
        }
        val deck = Deck.fromJson(arr)
        assertEquals(2, deck.size())
        assertEquals(CardType.Doom, deck.draw()?.type)
    }

    @Test
    fun `Deck fromJson empty array produces empty deck`() {
        val deck = Deck.fromJson(JSONArray())
        assertEquals(0, deck.size())
        assertNull(deck.draw())
    }

    @Test
    fun `Deck insertFromTop at position 0 makes card new top`() {
        val deck = Deck()
        deck.insertAt(Card(CardType.Normal), 0)
        val newTop = Card(CardType.Doom)
        deck.insertFromTop(newTop, 0)
        assertEquals(CardType.Doom, deck.peekTop()?.type)
    }

    @Test
    fun `Deck insertFromTop at deck size places card at bottom`() {
        val deck = Deck()
        deck.insertAt(Card(CardType.Normal), 0)
        deck.insertAt(Card(CardType.Normal), 1)
        val bottom = Card(CardType.PowerNap)
        deck.insertFromTop(bottom, deck.size())
        // Draw all cards until empty; last draw before null should be PowerNap
        var last: Card? = null
        var card = deck.draw()
        while (card != null) {
            last = card
            card = deck.draw()
        }
        assertEquals(CardType.PowerNap, last?.type)
    }

    @Test
    fun `Deck peekTopN returns cards top-first without removing them`() {
        val deck = Deck()
        deck.insertAt(Card(CardType.Normal), 0)
        deck.insertAt(Card(CardType.Doom), 1)
        val sizeBefore = deck.size()
        val peeked = deck.peekTopN(2)
        assertEquals(2, peeked.size)
        assertEquals(sizeBefore, deck.size())
        assertEquals(CardType.Doom, peeked[0].type)
        assertEquals(CardType.Normal, peeked[1].type)
    }

    @Test
    fun `Deck peekTopN with n larger than deck returns all cards`() {
        val deck = Deck()
        deck.insertAt(Card(CardType.Normal), 0)
        val peeked = deck.peekTopN(10)
        assertEquals(1, peeked.size)
    }

    @Test
    fun `Deck peekTop on empty deck returns null`() {
        assertNull(Deck().peekTop())
    }

    // endregion

    // region Discard

    @Test
    fun `Discard peekTop on empty pile returns null`() {
        assertNull(Discard().peekTop())
    }

    @Test
    fun `Discard toJson serializes all cards`() {
        val discard = Discard()
        discard.add(Card(CardType.Normal))
        discard.add(Card(CardType.Doom))
        assertEquals(2, discard.toJson().length())
    }

    @Test
    fun `Discard toJson on empty pile returns empty array`() {
        assertEquals(0, Discard().toJson().length())
    }

    @Test
    fun `Discard fromJson deserializes cards correctly`() {
        val arr = JSONArray().apply {
            put(JSONObject().apply { put("type", "Normal") })
            put(JSONObject().apply { put("type", "Doom") })
        }
        val discard = Discard.fromJson(arr)
        assertEquals(CardType.Doom, discard.peekTop()?.type)
    }

    @Test
    fun `Discard fromJson empty array produces empty pile`() {
        val discard = Discard.fromJson(JSONArray())
        assertNull(discard.peekTop())
    }

    // endregion

    // region CardType.fromWire

    @Test fun `CardType fromWire DOOM returns Doom`() = assertEquals(CardType.Doom, CardType.fromWire("DOOM"))
    @Test fun `CardType fromWire SNACK_STASH returns SnackStash`() = assertEquals(CardType.SnackStash, CardType.fromWire("SNACK_STASH"))
    @Test fun `CardType fromWire SNACKSTASH returns SnackStash`() = assertEquals(CardType.SnackStash, CardType.fromWire("SNACKSTASH"))
    @Test fun `CardType fromWire POWER_NAP returns PowerNap`() = assertEquals(CardType.PowerNap, CardType.fromWire("POWER_NAP"))
    @Test fun `CardType fromWire POWERNAP returns PowerNap`() = assertEquals(CardType.PowerNap, CardType.fromWire("POWERNAP"))
    @Test fun `CardType fromWire QUICK_PEEK returns QuickPeek`() = assertEquals(CardType.QuickPeek, CardType.fromWire("QUICK_PEEK"))
    @Test fun `CardType fromWire QUICKPEEK returns QuickPeek`() = assertEquals(CardType.QuickPeek, CardType.fromWire("QUICKPEEK"))
    @Test fun `CardType fromWire CAGE_SWAP returns CageSwap`() = assertEquals(CardType.CageSwap, CardType.fromWire("CAGE_SWAP"))
    @Test fun `CardType fromWire CAGESWAP returns CageSwap`() = assertEquals(CardType.CageSwap, CardType.fromWire("CAGESWAP"))
    @Test fun `CardType fromWire SIGN_OF_FATE returns SignOfFate`() = assertEquals(CardType.SignOfFate, CardType.fromWire("SIGN_OF_FATE"))
    @Test fun `CardType fromWire SIGNOFFATE returns SignOfFate`() = assertEquals(CardType.SignOfFate, CardType.fromWire("SIGNOFFATE"))
    @Test fun `CardType fromWire SNIFF_AHEAD returns SniffAhead`() = assertEquals(CardType.SniffAhead, CardType.fromWire("SNIFF_AHEAD"))
    @Test fun `CardType fromWire SNIFFAHEAD returns SniffAhead`() = assertEquals(CardType.SniffAhead, CardType.fromWire("SNIFFAHEAD"))
    @Test fun `CardType fromWire BEG_FOR_SNACKS returns BegForSnacks`() = assertEquals(CardType.BegForSnacks, CardType.fromWire("BEG_FOR_SNACKS"))
    @Test fun `CardType fromWire BEGFORSNACKS returns BegForSnacks`() = assertEquals(CardType.BegForSnacks, CardType.fromWire("BEGFORSNACKS"))
    @Test fun `CardType fromWire TUNNEL_CHAOS returns TunnelChaos`() = assertEquals(CardType.TunnelChaos, CardType.fromWire("TUNNEL_CHAOS"))
    @Test fun `CardType fromWire TUNNELCHAOS returns TunnelChaos`() = assertEquals(CardType.TunnelChaos, CardType.fromWire("TUNNELCHAOS"))
    @Test fun `CardType fromWire STEAL_CARD returns StealCard`() = assertEquals(CardType.StealCard, CardType.fromWire("STEAL_CARD"))
    @Test fun `CardType fromWire STEALCARD returns StealCard`() = assertEquals(CardType.StealCard, CardType.fromWire("STEALCARD"))
    @Test fun `CardType fromWire HYPER_MODE returns HyperMode`() = assertEquals(CardType.HyperMode, CardType.fromWire("HYPER_MODE"))
    @Test fun `CardType fromWire HYPERMODE returns HyperMode`() = assertEquals(CardType.HyperMode, CardType.fromWire("HYPERMODE"))
    @Test fun `CardType fromWire HAMSTER_FOUR returns FourHamsters`() = assertEquals(CardType.FourHamsters, CardType.fromWire("HAMSTER_FOUR"))
    @Test fun `CardType fromWire HAMSTER4 returns FourHamsters`() = assertEquals(CardType.FourHamsters, CardType.fromWire("HAMSTER4"))
    @Test fun `CardType fromWire HAMSTER_TWO returns TwoHamsters`() = assertEquals(CardType.TwoHamsters, CardType.fromWire("HAMSTER_TWO"))
    @Test fun `CardType fromWire HAMSTER2 returns TwoHamsters`() = assertEquals(CardType.TwoHamsters, CardType.fromWire("HAMSTER2"))
    @Test fun `CardType fromWire HAMSTER_TRIO returns HamsterTrio`() = assertEquals(CardType.HamsterTrio, CardType.fromWire("HAMSTER_TRIO"))
    @Test fun `CardType fromWire HAMSTERTRIO returns HamsterTrio`() = assertEquals(CardType.HamsterTrio, CardType.fromWire("HAMSTERTRIO"))
    @Test fun `CardType fromWire SQUICK returns Squick`() = assertEquals(CardType.Squick, CardType.fromWire("SQUICK"))
    @Test fun `CardType fromWire NORMAL returns Normal`() = assertEquals(CardType.Normal, CardType.fromWire("NORMAL"))
    @Test fun `CardType fromWire exact enum name falls back to valueOf`() = assertEquals(CardType.FourHamsters, CardType.fromWire("FourHamsters"))
    @Test fun `CardType fromWire unknown value returns Normal`() = assertEquals(CardType.Normal, CardType.fromWire("UNKNOWN_CARD"))
    @Test fun `CardType fromWire trims whitespace`() = assertEquals(CardType.Doom, CardType.fromWire("  doom  "))

    // endregion

    // region Status.fromWire

    @Test fun `Status fromWire SETUP returns Lobby`() = assertEquals(Status.Lobby, Status.fromWire("SETUP"))
    @Test fun `Status fromWire LOBBY returns Lobby`() = assertEquals(Status.Lobby, Status.fromWire("LOBBY"))
    @Test fun `Status fromWire RUNNING returns Playing`() = assertEquals(Status.Playing, Status.fromWire("RUNNING"))
    @Test fun `Status fromWire PLAYING returns Playing`() = assertEquals(Status.Playing, Status.fromWire("PLAYING"))
    @Test fun `Status fromWire FINISHED returns Finished`() = assertEquals(Status.Finished, Status.fromWire("FINISHED"))
    @Test fun `Status fromWire exact enum name falls back to valueOf`() = assertEquals(Status.Playing, Status.fromWire("Playing"))
    @Test fun `Status fromWire unknown value returns Lobby`() = assertEquals(Status.Lobby, Status.fromWire("UNKNOWN"))
    @Test fun `Status fromWire trims whitespace`() = assertEquals(Status.Playing, Status.fromWire("  RUNNING  "))

    // endregion

    // region Player

    @Test
    fun `Player isAlive returns true when aliveFlag is true even with zero lives`() {
        val player = Player.fromJson(JSONObject().apply {
            put("id", "p1")
            put("lives", 0)
            put("alive", true)
        })
        assertTrue(player.isAlive())
    }

    @Test
    fun `Player isAlive returns false when aliveFlag is false even with positive lives`() {
        val player = Player.fromJson(JSONObject().apply {
            put("id", "p1")
            put("lives", 3)
            put("alive", false)
        })
        assertFalse(player.isAlive())
    }

    @Test
    fun `Player isAlive uses lives when alive field absent`() {
        val player = Player("p1", lives = 2)
        assertTrue(player.isAlive())
        player.lives = 0
        assertFalse(player.isAlive())
    }

    @Test
    fun `Player visibleHandSize returns handSizeHint when larger than actual hand`() {
        val json = JSONObject().apply {
            put("id", "p1")
            put("lives", 3)
            put("handSize", 5)
        }
        val player = Player.fromJson(json)
        assertEquals(5, player.visibleHandSize())
    }

    @Test
    fun `Player visibleHandSize returns actual hand size when larger than hint`() {
        val player = Player("p1", lives = 3)
        repeat(4) { player.hand.add(Card(CardType.Normal)) }
        assertEquals(4, player.visibleHandSize())
    }

    @Test
    fun `Player toJson includes all expected fields`() {
        val player = Player("p1", lives = 3, name = "Alice", avatar = "hamster")
        player.hand.add(Card(CardType.Normal))
        val json = player.toJson()
        assertEquals("p1", json.getString("id"))
        assertEquals("Alice", json.getString("playerName"))
        assertEquals("hamster", json.getString("avatar"))
        assertEquals(3, json.getInt("lives"))
        assertTrue(json.getBoolean("alive"))
        assertEquals(1, json.getJSONArray("hand").length())
    }

    @Test
    fun `Player fromJson uses playerId key when id key is absent`() {
        val json = JSONObject().apply {
            put("playerId", "abc")
            put("lives", 2)
        }
        val player = Player.fromJson(json)
        assertEquals("abc", player.id)
    }

    @Test
    fun `Player fromJson falls back to name field when playerName is blank`() {
        val json = JSONObject().apply {
            put("id", "p1")
            put("lives", 2)
            put("playerName", "")
            put("name", "Bob")
        }
        val player = Player.fromJson(json)
        assertEquals("Bob", player.name)
    }

    @Test
    fun `Player displayName uses short id without hyphen as-is`() {
        val json = JSONObject().apply {
            put("id", "fat")
            put("lives", 2)
        }
        val player = Player.fromJson(json)
        assertEquals("fat", player.name)
    }

    @Test
    fun `Player displayName abbreviates long id`() {
        val json = JSONObject().apply {
            put("id", "averylongplayerid")
            put("lives", 2)
        }
        val player = Player.fromJson(json)
        assertEquals("Player erid", player.name)
    }

    @Test
    fun `Player displayName abbreviates id containing hyphen`() {
        val json = JSONObject().apply {
            put("id", "abc-123")
            put("lives", 2)
        }
        val player = Player.fromJson(json)
        assertEquals("Player -123", player.name)
    }

    @Test
    fun `Player fromJson deserializes hand cards`() {
        val handArray = JSONArray().apply {
            put(JSONObject().apply { put("type", "Normal") })
        }
        val json = JSONObject().apply {
            put("id", "p1")
            put("lives", 3)
            put("hand", handArray)
        }
        val player = Player.fromJson(json)
        assertEquals(1, player.hand.size)
        assertEquals(CardType.Normal, player.hand[0].type)
    }

    // endregion

    // region GameState

    @Test
    fun `GameState currentTurnPlayerId uses currentPlayerId when set`() {
        val state = makeMinimalGameState(currentPlayerId = "zombie")
        assertEquals("zombie", state.currentTurnPlayerId)
    }

    @Test
    fun `GameState currentTurnPlayerId falls back to currentPlayerIndex when currentPlayerId is null`() {
        val state = makeMinimalGameState(currentPlayerId = null, currentPlayerIndex = 0)
        assertEquals("fat", state.currentTurnPlayerId)
    }

    @Test
    fun `GameState deckSize uses remainingDeckSize when non-negative`() {
        val state = makeMinimalGameState(remainingDeckSize = 42)
        assertEquals(42, state.deckSize)
    }

    @Test
    fun `GameState deckSize uses deck size when remainingDeckSize is negative`() {
        val deck = Deck().also { it.insertAt(Card(CardType.Normal), 0) }
        val state = makeMinimalGameState(remainingDeckSize = -1, deck = deck)
        assertEquals(1, state.deckSize)
    }

    @Test
    fun `GameState toJson round-trips basic fields`() {
        val state = makeMinimalGameState()
        val json = state.toJson()
        assertEquals("test-id", json.getString("id"))
        assertEquals("Playing", json.getString("status"))
        assertEquals(0, json.getInt("currentPlayerIndex"))
    }

    @Test
    fun `GameState toJson includes currentPlayerId only when set`() {
        val withId = makeMinimalGameState(currentPlayerId = "fat")
        val withoutId = makeMinimalGameState(currentPlayerId = null)
        assertTrue(withId.toJson().has("currentPlayerId"))
        assertFalse(withoutId.toJson().has("currentPlayerId"))
    }

    @Test
    fun `GameState fromJson deserializes app format`() {
        val json = minimalGameStateJson()
        val state = GameState.fromJson(json)
        assertEquals("test-id", state.id)
        assertEquals(Status.Playing, state.status)
        assertEquals(1, state.players.size)
        assertEquals("fat", state.players[0].id)
    }

    @Test
    fun `GameState fromJson with currentPlayerId sets currentTurnPlayerId`() {
        val json = minimalGameStateJson().apply { put("currentPlayerId", "fat") }
        val state = GameState.fromJson(json)
        assertEquals("fat", state.currentTurnPlayerId)
    }

    @Test
    fun `GameState fromBackendJson deserializes backend format`() {
        val json = JSONObject().apply {
            put("gameId", "backend-id")
            put("gameState", "RUNNING")
            put("currentPlayerId", "fat")
            put("players", JSONArray().apply {
                put(JSONObject().apply {
                    put("playerId", "fat")
                    put("lives", 3)
                })
            })
        }
        val state = GameState.fromBackendJson(json)
        assertEquals("backend-id", state.id)
        assertEquals(Status.Playing, state.status)
        assertEquals("fat", state.currentTurnPlayerId)
        assertEquals(0, state.currentPlayerIndex)
    }

    @Test
    fun `GameState fromBackendJson with unknown currentPlayerId defaults index to 0`() {
        val json = JSONObject().apply {
            put("gameId", "id")
            put("gameState", "RUNNING")
            put("currentPlayerId", "ghost")
            put("players", JSONArray().apply {
                put(JSONObject().apply {
                    put("playerId", "fat")
                    put("lives", 3)
                })
            })
        }
        val state = GameState.fromBackendJson(json)
        assertEquals(0, state.currentPlayerIndex)
    }

    // endregion

    // region helpers

    private fun makeMinimalGameState(
        currentPlayerId: String? = null,
        currentPlayerIndex: Int = 0,
        remainingDeckSize: Int = 0,
        deck: Deck = Deck()
    ): GameState {
        val players = arrayListOf(
            Player("fat", lives = 3),
            Player("zombie", lives = 3)
        )
        return GameState(
            id = "test-id",
            players = players,
            deck = deck,
            discard = Discard(),
            currentPlayerIndex = currentPlayerIndex,
            status = Status.Playing,
            currentPlayerId = currentPlayerId,
            remainingDeckSize = remainingDeckSize
        )
    }

    private fun minimalGameStateJson(): JSONObject {
        val playerJson = JSONObject().apply {
            put("id", "fat")
            put("lives", 3)
        }
        return JSONObject().apply {
            put("id", "test-id")
            put("status", "PLAYING")
            put("currentPlayerIndex", 0)
            put("players", JSONArray().apply { put(playerJson) })
            put("deck", JSONArray())
            put("discard", JSONArray())
        }
    }

    // endregion
}
