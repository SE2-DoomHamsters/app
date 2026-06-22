package com.doomhamsters.mascot.presentation

import com.doomhamsters.viewmodel.ConnectionStatus
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class MascotPresentationTest {

    private fun resolve(
        turn: Boolean = false,
        doom: Boolean = false,
        connection: ConnectionStatus = ConnectionStatus.Connected,
        cardPlayed: Boolean = false,
    ) = MascotPresentation.resolve(
        isLocalPlayersTurn = turn,
        hasPendingDoom = doom,
        connectionStatus = connection,
        cardPlayedActive = cardPlayed,
    )

    @Test
    fun `defaults to idle`() {
        assertEquals(MascotAnimation.IDLE, resolve())
    }

    @Test
    fun `local turn shows your turn`() {
        assertEquals(MascotAnimation.YOUR_TURN, resolve(turn = true))
    }

    @Test
    fun `card played outranks your turn`() {
        assertEquals(MascotAnimation.CARD_PLAYED, resolve(turn = true, cardPlayed = true))
    }

    @Test
    fun `doom outranks card played and your turn`() {
        assertEquals(MascotAnimation.DOOM, resolve(turn = true, cardPlayed = true, doom = true))
    }

    @Test
    fun `disconnected outranks everything`() {
        assertEquals(
            MascotAnimation.DISCONNECTED,
            resolve(
                turn = true,
                cardPlayed = true,
                doom = true,
                connection = ConnectionStatus.Disconnected,
            ),
        )
    }

    @Test
    fun `reconnecting counts as disconnected`() {
        assertEquals(
            MascotAnimation.DISCONNECTED,
            resolve(connection = ConnectionStatus.Reconnecting(attempt = 1, maxAttempts = 3)),
        )
    }

    @Test
    fun `idle to your turn is a turn transition`() {
        assertEquals(
            MascotAnimation.TURNING,
            MascotPresentation.transitionFor(MascotAnimation.IDLE, MascotAnimation.YOUR_TURN),
        )
    }

    @Test
    fun `your turn to idle is the reverse transition`() {
        assertEquals(
            MascotAnimation.TURNING_BACK,
            MascotPresentation.transitionFor(MascotAnimation.YOUR_TURN, MascotAnimation.IDLE),
        )
    }

    @Test
    fun `non turn swaps have no transition`() {
        assertNull(MascotPresentation.transitionFor(MascotAnimation.IDLE, MascotAnimation.DOOM))
        assertNull(MascotPresentation.transitionFor(MascotAnimation.DOOM, MascotAnimation.IDLE))
        assertNull(MascotPresentation.transitionFor(MascotAnimation.IDLE, MascotAnimation.IDLE))
    }

    @Test
    fun `swap squashes when incoming wants it on entry`() {
        // DOOM has squashOnSwap = true.
        assertEquals(true, MascotPresentation.shouldSquash(MascotAnimation.IDLE, MascotAnimation.DOOM))
    }

    @Test
    fun `swap squashes when outgoing wants it on exit`() {
        // CARD_PLAYED has squashOnExit = true; IDLE does not squash on entry.
        assertEquals(true, MascotPresentation.shouldSquash(MascotAnimation.CARD_PLAYED, MascotAnimation.IDLE))
    }

    @Test
    fun `swap does not squash when neither side opts in`() {
        assertEquals(false, MascotPresentation.shouldSquash(MascotAnimation.IDLE, MascotAnimation.YOUR_TURN))
    }

    @Test
    fun `asset uri points into the mascot assets folder`() {
        assertEquals("file:///android_asset/mascot/idle.gif", MascotAnimation.IDLE.assetUri)
    }
}
