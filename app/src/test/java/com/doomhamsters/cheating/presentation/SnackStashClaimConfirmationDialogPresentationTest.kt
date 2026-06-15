package com.doomhamsters.cheating.presentation

import com.doomhamsters.cheating.SnackStashClaimEvent
import com.doomhamsters.model.Card
import com.doomhamsters.model.CardType
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class SnackStashClaimConfirmationDialogPresentationTest {

    @Test
    fun `state returns selected card copy when doom selection is open`() {
        val selectedCard = Card(CardType.SnackStash, id = "card-1")
        val state = SnackStashClaimConfirmationDialogPresentation.state(
            hand = listOf(Card(CardType.Normal), selectedCard),
            selectedCardIndex = 1,
            pendingDoomRequiresSelection = true,
            pendingClaim = null
        )

        assertEquals(selectedCard, state?.selectedCard)
        assertEquals("Use actual Snack Stash?", state?.copy?.title)
    }

    @Test
    fun `state is hidden when a claim is already pending`() {
        val state = SnackStashClaimConfirmationDialogPresentation.state(
            hand = listOf(Card(CardType.SnackStash)),
            selectedCardIndex = 0,
            pendingDoomRequiresSelection = true,
            pendingClaim = pendingClaim()
        )

        assertNull(state)
    }

    @Test
    fun `hand selection opens and toggles claim confirmation index`() {
        val selected = SnackStashClaimConfirmationDialogPresentation.handSelectionAfterToggle(
            currentSelectedCardIndex = -1,
            toggledCardIndex = 2,
            pendingDoomRequiresSelection = true,
            pendingClaim = null
        )
        val cleared = SnackStashClaimConfirmationDialogPresentation.handSelectionAfterToggle(
            currentSelectedCardIndex = 2,
            toggledCardIndex = 2,
            pendingDoomRequiresSelection = true,
            pendingClaim = null
        )

        assertEquals(SnackStashHandSelectionState(2, 2), selected)
        assertEquals(SnackStashHandSelectionState(-1, -1), cleared)
    }

    @Test
    fun `actual snack stash uses honest copy`() {
        val copy = SnackStashClaimConfirmationDialogPresentation.copyFor(Card(CardType.SnackStash))

        assertEquals("Use actual Snack Stash?", copy.title)
        assertEquals("Use Snack Stash", copy.confirmLabel)
        assertFalse(copy.message.contains("Claim"))
    }

    @Test
    fun `non snack stash uses bluff claim copy`() {
        val copy = SnackStashClaimConfirmationDialogPresentation.copyFor(Card(CardType.Normal, name = "Normal"))

        assertEquals("Claim Snack Stash?", copy.title)
        assertEquals("Confirm Claim", copy.confirmLabel)
        assertEquals(
            "Claim Normal as Snack Stash. Other players will vote on whether they believe you.",
            copy.message
        )
    }

    private fun pendingClaim(): SnackStashClaimEvent {
        return SnackStashClaimEvent(
            claimId = "claim-1",
            playerId = "player-1",
            playerName = "Local",
            votesRequired = 1,
            votesReceived = 0,
            votedPlayerIds = emptySet(),
            message = null
        )
    }
}
