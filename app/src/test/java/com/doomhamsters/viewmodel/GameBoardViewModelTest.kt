package com.doomhamsters.viewmodel

import com.doomhamsters.logic.GameEngine
import com.doomhamsters.logic.InvalidActionException
import com.doomhamsters.logic.InvalidDrawException
import com.doomhamsters.model.Card
import com.doomhamsters.model.CardType
import com.doomhamsters.model.GameState
import com.doomhamsters.model.Player
import com.doomhamsters.model.Status
import io.mockk.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.*
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class GameBoardViewModelTest {

    private lateinit var viewModel: GameBoardViewModel
    private lateinit var mockGameState: GameState
    private val testDispatcher = UnconfinedTestDispatcher()

    @BeforeEach
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        mockkConstructor(GameEngine::class)
        viewModel = GameBoardViewModel()

        mockGameState = mockk(relaxed = true)
        every { anyConstructed<GameEngine>().getState() } returns mockGameState
    }

    @AfterEach
    fun teardown() {
        Dispatchers.resetMain()
        unmockkAll()
    }

    private fun setEngineToNullArtificially() {
        val field = GameBoardViewModel::class.java.getDeclaredField("gameEngine")
        field.isAccessible = true
        field.set(viewModel, null)
    }

    @Test
    fun test_startGame_initializesGameEngineAndEmitsGameState() {
        val playerIds = arrayListOf("fat", "zombie", "sleepy")
        viewModel.startGame(playerIds)
        assertEquals(mockGameState, viewModel.gameState.value)
    }

    @Test
    fun test_draw_withoutStartingGame_handlesNullEngineGracefully() {
        assertDoesNotThrow { viewModel.draw("fat") }
        assertNull(viewModel.gameState.value)
        assertEquals("fat drew a card", viewModel.log.value.last())
    }

    @Test
    fun test_draw_normalCard_updatesStateAndLogsAction() {
        viewModel.startGame(arrayListOf("fat", "zombie", "sleepy"))
        every { anyConstructed<GameEngine>().draw("fat") } returns null

        viewModel.draw("fat")

        assertEquals(mockGameState, viewModel.gameState.value)
        assertNull(viewModel.pendingDoom.value)
        assertEquals("fat drew a card", viewModel.log.value.last())
    }

    @Test
    fun test_draw_doomCard_updatesPendingDoomAndLogsAction() {
        viewModel.startGame(arrayListOf("fat", "zombie", "sleepy"))
        val doomCard = Card(CardType.Doom)
        every { anyConstructed<GameEngine>().draw("fat") } returns doomCard

        viewModel.draw("fat")

        assertEquals(doomCard, viewModel.pendingDoom.value)
        assertEquals("fat defused a Doom card!", viewModel.log.value.last())
    }

    @Test
    fun test_draw_throwsInvalidActionException_emitsError() = runTest {
        val errors = mutableListOf<String>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.error.collect { errors.add(it) }
        }

        viewModel.startGame(arrayListOf("fat", "zombie", "sleepy"))
        every { anyConstructed<GameEngine>().draw("fat") } throws InvalidActionException("Player dead")

        viewModel.draw("fat")

        assertEquals("Player dead", errors.first())
    }

    @Test
    fun test_draw_throwsInvalidActionExceptionWithNullMessage_emitsDefaultError() = runTest {
        val errors = mutableListOf<String>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.error.collect { errors.add(it) }
        }

        viewModel.startGame(arrayListOf("fat", "zombie", "sleepy"))
        val mockException = mockk<InvalidActionException>()
        every { mockException.message } returns null
        every { anyConstructed<GameEngine>().draw("fat") } throws mockException

        viewModel.draw("fat")

        assertEquals("Invalid action", errors.first())
    }

    @Test
    fun test_draw_throwsInvalidDrawException_emitsError() = runTest {
        val errors = mutableListOf<String>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.error.collect { errors.add(it) }
        }

        viewModel.startGame(arrayListOf("fat", "zombie", "sleepy"))
        every { anyConstructed<GameEngine>().draw("fat") } throws InvalidDrawException("Deck empty")

        viewModel.draw("fat")

        assertEquals("Deck empty", errors.first())
    }

    @Test
    fun test_draw_throwsInvalidDrawExceptionWithNullMessage_emitsDefaultError() = runTest {
        val errors = mutableListOf<String>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.error.collect { errors.add(it) }
        }

        viewModel.startGame(arrayListOf("fat", "zombie", "sleepy"))
        val mockException = mockk<InvalidDrawException>()
        every { mockException.message } returns null
        every { anyConstructed<GameEngine>().draw("fat") } throws mockException

        viewModel.draw("fat")

        assertEquals("Invalid draw", errors.first())
    }

    @Test
    fun test_insertDoom_withNoPendingDoom_doesNothing() {
        viewModel.startGame(arrayListOf("fat", "zombie", "sleepy"))
        viewModel.insertDoom(2)
        verify(exactly = 0) { anyConstructed<GameEngine>().insertCard(any(), any()) }
    }

    @Test
    fun test_insertDoom_withPendingDoom_insertsCardAndClearsPending() {
        viewModel.startGame(arrayListOf("fat", "zombie", "sleepy"))
        val doomCard = Card(CardType.Doom)
        every { anyConstructed<GameEngine>().draw("fat") } returns doomCard
        every { anyConstructed<GameEngine>().insertCard(any(), any()) } just Runs

        viewModel.draw("fat")
        viewModel.insertDoom(2)

        verify { anyConstructed<GameEngine>().insertCard(doomCard, 2) }
        assertNull(viewModel.pendingDoom.value)
        assertEquals("Doom inserted at position 2", viewModel.log.value.last())
    }

    @Test
    fun test_insertDoom_withPendingDoom_butEngineIsForcedNull_coversUnreachableBranch() {
        viewModel.startGame(arrayListOf("fat"))
        every { anyConstructed<GameEngine>().draw("fat") } returns Card(CardType.Doom)
        viewModel.draw("fat")

        setEngineToNullArtificially()

        assertDoesNotThrow { viewModel.insertDoom(0) }
        assertNull(viewModel.pendingDoom.value)
    }

    @Test
    fun test_advanceTurn_withoutStartingGame_handlesNullEngineGracefully() {
        assertDoesNotThrow { viewModel.advanceTurn() }
        assertNull(viewModel.gameState.value)
    }

    @Test
    fun test_advanceTurn_withPendingDoom_doesNotAdvanceEngine() {
        viewModel.startGame(arrayListOf("fat", "zombie", "sleepy"))
        every { anyConstructed<GameEngine>().draw("fat") } returns Card(CardType.Doom)
        viewModel.draw("fat")

        viewModel.advanceTurn()

        verify(exactly = 0) { anyConstructed<GameEngine>().advanceTurn() }
    }

    @Test
    fun test_advanceTurn_statusPlaying_advancesEngineButDoesNotEmitGameOver() = runTest {
        val gameOvers = mutableListOf<String>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.gameOver.collect { gameOvers.add(it) }
        }

        viewModel.startGame(arrayListOf("fat", "zombie", "sleepy"))
        every { mockGameState.status } returns Status.Playing

        viewModel.advanceTurn()

        verify(exactly = 1) { anyConstructed<GameEngine>().advanceTurn() }
        assertTrue(gameOvers.isEmpty())
    }

    @Test
    fun test_advanceTurn_statusFinished_emitsGameOverWithWinnerId() = runTest {
        val gameOvers = mutableListOf<String>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.gameOver.collect { gameOvers.add(it) }
        }

        viewModel.startGame(arrayListOf("fat", "zombie", "sleepy"))

        val survivingPlayer = Player("zombie", 1)
        every { mockGameState.status } returns Status.Finished
        every { mockGameState.players } returns arrayListOf(
            Player("fat", 0),
            survivingPlayer,
            Player("sleepy", 0)
        )

        viewModel.advanceTurn()

        assertEquals("zombie", gameOvers.first())
    }

    @Test
    fun test_advanceTurn_statusFinished_noAlivePlayer_emitsUnknown() = runTest {
        val gameOvers = mutableListOf<String>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.gameOver.collect { gameOvers.add(it) }
        }

        viewModel.startGame(arrayListOf("fat", "zombie"))
        every { mockGameState.status } returns Status.Finished
        every { mockGameState.players } returns arrayListOf(
            Player("fat", 0),
            Player("zombie", 0)
        )

        viewModel.advanceTurn()

        assertEquals("Unknown", gameOvers.first())
    }

    @Test
    fun test_advanceTurn_statusFinished_emptyPlayerList_bypassesLambda_emitsUnknown() = runTest {
        val gameOvers = mutableListOf<String>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.gameOver.collect { gameOvers.add(it) }
        }

        viewModel.startGame(arrayListOf("fat"))
        every { mockGameState.status } returns Status.Finished
        every { mockGameState.players } returns arrayListOf()

        viewModel.advanceTurn()

        assertEquals("Unknown", gameOvers.first())
    }
}