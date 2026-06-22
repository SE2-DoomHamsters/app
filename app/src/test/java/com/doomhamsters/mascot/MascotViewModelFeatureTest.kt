package com.doomhamsters.mascot

import com.doomhamsters.mascot.presentation.MascotAnimation
import com.doomhamsters.model.Card
import com.doomhamsters.model.CardType
import com.doomhamsters.viewmodel.ConnectionStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class MascotViewModelFeatureTest {

    private class Inputs {
        val turn = MutableStateFlow(false)
        val doom = MutableStateFlow<Card?>(null)
        val cardPlayed = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
        val connection = MutableStateFlow<ConnectionStatus>(ConnectionStatus.Connected)
    }

    // The feature runs in an independent scope bound to the test scheduler so
    // advanceUntilIdle()/runCurrent() drive its collectors and delays. (Coroutines
    // in this scope are inert once the test scheduler stops, so no cleanup needed.)
    private fun TestScope.startFeature(i: Inputs): MascotViewModelFeature {
        val scope = CoroutineScope(StandardTestDispatcher(testScheduler))
        val feature = MascotViewModelFeature(
            scope = scope,
            isLocalPlayersTurn = i.turn,
            pendingDoom = i.doom,
            cardPlayed = i.cardPlayed,
            connectionStatus = i.connection,
        )
        advanceUntilIdle()
        return feature
    }

    @Test
    fun `starts idle`() = runTest {
        val i = Inputs()
        val feature = startFeature(i)
        assertEquals(MascotAnimation.IDLE, feature.animation.value)
    }

    @Test
    fun `idle to your turn plays turning then settles on your turn`() = runTest {
        val i = Inputs()
        val feature = startFeature(i)

        i.turn.value = true
        runCurrent()
        assertEquals(MascotAnimation.TURNING, feature.animation.value)

        advanceUntilIdle()
        assertEquals(MascotAnimation.YOUR_TURN, feature.animation.value)
    }

    @Test
    fun `your turn to idle plays the reverse transition`() = runTest {
        val i = Inputs()
        val feature = startFeature(i)
        i.turn.value = true
        advanceUntilIdle()
        assertEquals(MascotAnimation.YOUR_TURN, feature.animation.value)

        i.turn.value = false
        runCurrent()
        assertEquals(MascotAnimation.TURNING_BACK, feature.animation.value)

        advanceUntilIdle()
        assertEquals(MascotAnimation.IDLE, feature.animation.value)
    }

    @Test
    fun `card played flashes then reverts`() = runTest {
        val i = Inputs()
        val feature = startFeature(i)

        i.cardPlayed.tryEmit(Unit)
        runCurrent()
        assertEquals(MascotAnimation.CARD_PLAYED, feature.animation.value)

        advanceUntilIdle()
        assertEquals(MascotAnimation.IDLE, feature.animation.value)
    }

    @Test
    fun `pending doom overrides idle`() = runTest {
        val i = Inputs()
        val feature = startFeature(i)

        i.doom.value = Card(CardType.Doom)
        runCurrent()
        assertEquals(MascotAnimation.DOOM, feature.animation.value)
    }

    @Test
    fun `disconnect overrides your turn`() = runTest {
        val i = Inputs()
        val feature = startFeature(i)
        i.turn.value = true
        advanceUntilIdle()

        i.connection.value = ConnectionStatus.Disconnected
        runCurrent()
        assertEquals(MascotAnimation.DISCONNECTED, feature.animation.value)
    }
}
