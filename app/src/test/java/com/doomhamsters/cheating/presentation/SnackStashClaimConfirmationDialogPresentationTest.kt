package com.doomhamsters.cheating.presentation

import com.doomhamsters.model.Card
import com.doomhamsters.model.CardType
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Test

class SnackStashClaimConfirmationDialogPresentationTest {

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
}
