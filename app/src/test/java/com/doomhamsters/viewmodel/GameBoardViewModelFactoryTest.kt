package com.doomhamsters.viewmodel

import androidx.lifecycle.ViewModel
import com.doomhamsters.GameRepository
import com.doomhamsters.model.GameState
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.just
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

@OptIn(ExperimentalCoroutinesApi::class)
class GameBoardViewModelFactoryTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var repository: GameRepository
    private val createdViewModels = mutableListOf<GameBoardViewModel>()

    @BeforeEach
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        repository = mockk(relaxed = true)
        coEvery { repository.connect() } just Runs
        coEvery { repository.subscribeToGame(any()) } returns activeStringFlow()
        coEvery { repository.subscribeToGameState(any()) } returns activeGameStateFlow()
        coEvery { repository.subscribeToPrivateEvents(any(), any()) } returns activeJsonFlow()
        coEvery { repository.subscribeToErrors(any(), any()) } returns activeStringFlow()
    }

    @AfterEach
    fun tearDown() {
        createdViewModels.forEach(::clearViewModel)
        createdViewModels.clear()
        Dispatchers.resetMain()
    }

    @Test
    fun `create returns a configured GameBoardViewModel`() = runTest {
        val factory = GameBoardViewModelFactory(
            gameId = "game-1",
            playerId = "player-1",
            playerName = "Alex",
            repository = repository
        )
        val vm = factory.create(GameBoardViewModel::class.java)
        advanceUntilIdle()

        assertNotNull(vm)
        assertEquals("game-1", vm.gameId)
        createdViewModels.add(vm)
    }

    @Test
    fun `create throws IllegalArgumentException for an incompatible ViewModel class`() {
        val factory = GameBoardViewModelFactory(
            gameId = "game-1",
            playerId = "player-1",
            playerName = "Alex",
            repository = repository
        )
        class AnotherViewModel : ViewModel()
        assertThrows<IllegalArgumentException> {
            factory.create(AnotherViewModel::class.java)
        }
    }

    private fun activeGameStateFlow() = flow<GameState> { awaitCancellation() }
    private fun activeStringFlow() = flow<String> { awaitCancellation() }
    private fun activeJsonFlow() = flow<org.json.JSONObject> { awaitCancellation() }

    private fun clearViewModel(viewModel: GameBoardViewModel) {
        GameBoardViewModel::class.java.getDeclaredMethod("onCleared")
            .apply { isAccessible = true }
            .invoke(viewModel)
    }
}
