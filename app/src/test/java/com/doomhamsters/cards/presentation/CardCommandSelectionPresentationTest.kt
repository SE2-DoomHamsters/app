package com.doomhamsters.cards.presentation

import com.doomhamsters.model.Card
import com.doomhamsters.model.CardType
import com.doomhamsters.model.GameState
import com.doomhamsters.model.Player
import com.doomhamsters.model.Status
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class CardCommandSelectionPresentationTest {

    @Test
    fun `beg for snacks state requires target and requested card type`() {
        val card = Card(CardType.BegForSnacks, id = "beg-1")

        val state = CardCommandSelectionPresentation.display(
            card = card,
            gameState = gameState(),
            localPlayerId = "p1",
            selectedTargetPlayerId = null,
            selectedCardType = CardCommandSelectionPresentation.initialRequestedCardType(card),
            hamsterType = ""
        )

        assertNotNull(state)
        requireNotNull(state)
        assertEquals("Beg for Snacks", state.title)
        assertTrue(state.requiresTargetPlayer)
        assertTrue(state.requiresRequestedCardType)
        assertFalse(state.requiresHamsterType)
        assertEquals(listOf(CardCommandSelectionOption("p2", "Bob")), state.targetOptions)
        assertFalse(state.canConfirm)
    }

    @Test
    fun `beg for snacks can confirm after target and card type are selected`() {
        val card = Card(CardType.BegForSnacks, id = "beg-1")

        val state = CardCommandSelectionPresentation.display(
            card = card,
            gameState = gameState(),
            localPlayerId = "p1",
            selectedTargetPlayerId = "p2",
            selectedCardType = "PowerNap",
            hamsterType = ""
        )

        assertTrue(requireNotNull(state).canConfirm)
    }

    @Test
    fun `requestable card type options exclude doom and keep backend wire values`() {
        val options = CardCommandSelectionPresentation.requestableCardTypeOptions()

        assertFalse(options.any { it.type == CardType.Doom })
        assertEquals("snack_stash", options.first { it.type == CardType.SnackStash }.wireValue)
        assertEquals("HamsterTwo", options.first { it.type == CardType.TwoHamsters }.wireValue)
        assertEquals("normal", options.first { it.type == CardType.Normal }.wireValue)
    }

    @Test
    fun `initial requested card type is only set for card type commands`() {
        assertEquals(
            "snack_stash",
            CardCommandSelectionPresentation.initialRequestedCardType(
                Card(CardType.BegForSnacks, id = "beg-1")
            )
        )
        assertNull(
            CardCommandSelectionPresentation.initialRequestedCardType(
                Card(CardType.StealCard, id = "steal-1")
            )
        )
    }

    @Test
    fun `activation parameters omit blank optional selections`() {
        val parameters = CardCommandSelectionPresentation.activationParameters(
            targetPlayerId = "p2",
            requestedCardType = " ",
            hamsterType = ""
        )

        assertEquals(mapOf("targetPlayerId" to "p2"), parameters)
    }@Test
    fun `display returns null when card is null or has no command`() {
        assertNull(
            CardCommandSelectionPresentation.display(null, gameState(), "p1", null, null, "")
        )
        val normalCard = Card(CardType.Normal, id = "normal-1")
        assertNull(
            CardCommandSelectionPresentation.display(normalCard, gameState(), "p1", null, null, "")
        )
    }

    @Test
    fun `canConfirm handles cards that require target but nothing else`() {
        val card = Card(CardType.StealCard, id = "steal-1")
        var state = CardCommandSelectionPresentation.display(
            card, gameState(), "p1", selectedTargetPlayerId = null, selectedCardType = null, hamsterType = ""
        )
        assertFalse(requireNotNull(state).canConfirm)

        state = CardCommandSelectionPresentation.display(
            card, gameState(), "p1", selectedTargetPlayerId = "p2", selectedCardType = null, hamsterType = ""
        )
        assertTrue(requireNotNull(state).canConfirm)
    }

    @Test
    fun `canConfirm handles cards that require target and hamster type`() {
        val card = Card(CardType.FourHamsters, id = "four-1")
        var state = CardCommandSelectionPresentation.display(
            card, gameState(), "p1", selectedTargetPlayerId = "p2", selectedCardType = null, hamsterType = ""
        )
        assertFalse(requireNotNull(state).canConfirm)
        state = CardCommandSelectionPresentation.display(
            card, gameState(), "p1", selectedTargetPlayerId = "p2", selectedCardType = null, hamsterType = "Sniper"
        )
        assertTrue(requireNotNull(state).canConfirm)
    }

    @Test
    fun `canConfirm handles cards that require nothing`() {
        val card = Card(CardType.QuickPeek, id = "peek-1")

        val state = CardCommandSelectionPresentation.display(
            card, gameState(), "p1", selectedTargetPlayerId = null, selectedCardType = null, hamsterType = ""
        )
        assertTrue(requireNotNull(state).canConfirm)
    }

    @Test
    fun `initialRequestedCardType handles nulls gracefully`() {
        assertNull(CardCommandSelectionPresentation.initialRequestedCardType(null))
        assertNull(CardCommandSelectionPresentation.initialRequestedCardType(Card(CardType.Normal, id = "n-1")))
    }

    @Test
    fun `activation parameters includes provided values correctly`() {
        val parameters = CardCommandSelectionPresentation.activationParameters(
            targetPlayerId = "p2",
            requestedCardType = "PowerNap",
            hamsterType = "Sniper"
        )

        assertEquals("p2", parameters["targetPlayerId"])
        assertEquals("PowerNap", parameters["cardType"])
        assertEquals("Sniper", parameters["hamsterType"])
    }

    @Test
    fun `requestable card type options use name as fallback wire value`() {
        val options = CardCommandSelectionPresentation.requestableCardTypeOptions()
        val powerNapOption = options.first { it.type == CardType.PowerNap }
        assertEquals("PowerNap", powerNapOption.wireValue)
    }

    private fun gameState(): GameState {
        return GameState(
            id = "game-1",
            players = arrayListOf(
                Player(id = "p1", name = "Alice", lives = 3),
                Player(id = "p2", name = "Bob", lives = 3),
                Player(id = "p3", name = "Dead", lives = 0)
            ),
            status = Status.Playing
        )
    }
}
