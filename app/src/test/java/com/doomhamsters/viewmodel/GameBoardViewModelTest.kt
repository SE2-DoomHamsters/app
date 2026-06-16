package com.doomhamsters.viewmodel

import com.doomhamsters.GameRepository
import com.doomhamsters.model.GameState
import com.doomhamsters.model.Player
import com.doomhamsters.model.Status
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.just
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class GameBoardViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var repository: GameRepository
    private val createdViewModels = mutableListOf<GameBoardViewModel>()

    @BeforeEach
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        repository = mockk(relaxed = true)
        coEvery { repository.connect() } just Runs
        coEvery { repository.subscribeToGame("game-1") } returns activeStringFlow()
        coEvery { repository.subscribeToGameState("game-1") } returns activeGameStateFlow()
        coEvery { repository.subscribeToPrivateEvents("game-1", any()) } returns activeJsonFlow()
        coEvery { repository.subscribeToErrors("game-1", any()) } returns activeStringFlow()
    }

    @AfterEach
    fun tearDown() {
        createdViewModels.forEach(::clearViewModel)
        createdViewModels.clear()
        Dispatchers.resetMain()
    }

    @Test
    fun `keeps original player id when it already exists in backend state`() = runTest {
        val state = gameState(
            players = arrayListOf(
                Player(id = "player-1", lives = 3, name = "Alex"),
                Player(id = "player-2", lives = 3, name = "Remote")
            )
        )
        coEvery { repository.fetchGameState("game-1", "player-1") } returns state
        coEvery { repository.subscribeToPrivateEvents("game-1", "player-1") } returns activeJsonFlow()

        val viewModel = createViewModel(
            gameId = "game-1",
            initialLocalPlayerId = "player-1",
            localPlayerName = "Alex",
            repository = repository
        )
        advanceUntilIdle()

        assertEquals("player-1", viewModel.localPlayerId)
    }

    @Test
    fun `resolves player id by name only when backend state has a unique match`() = runTest {
        val state = gameState(
            players = arrayListOf(
                Player(id = "server-42", lives = 3, name = "Alex"),
                Player(id = "player-2", lives = 3, name = "Remote")
            )
        )
        coEvery { repository.fetchGameState("game-1", "client-temp") } returns state
        coEvery { repository.fetchGameState("game-1", "server-42") } returns state
        coEvery { repository.subscribeToPrivateEvents("game-1", "server-42") } returns activeJsonFlow()

        val viewModel = createViewModel(
            gameId = "game-1",
            initialLocalPlayerId = "client-temp",
            localPlayerName = "Alex",
            repository = repository
        )
        advanceUntilIdle()

        assertEquals("server-42", viewModel.localPlayerId)
    }

    @Test
    fun `does not remap player id when name match is ambiguous`() = runTest {
        val state = gameState(
            players = arrayListOf(
                Player(id = "server-1", lives = 3, name = "Alex"),
                Player(id = "server-2", lives = 3, name = "Alex"),
                Player(id = "player-3", lives = 3, name = "Remote")
            )
        )
        coEvery { repository.fetchGameState("game-1", "client-temp") } returns state
        coEvery { repository.subscribeToPrivateEvents("game-1", "client-temp") } returns activeJsonFlow()

        val viewModel = createViewModel(
            gameId = "game-1",
            initialLocalPlayerId = "client-temp",
            localPlayerName = "Alex",
            repository = repository
        )
        advanceUntilIdle()

        assertEquals("client-temp", viewModel.localPlayerId)
    }

    @Test
    fun `retries initial connection before surfacing failure`() = runTest {
        val state = gameState(
            players = arrayListOf(
                Player(id = "player-1", lives = 3, name = "Alex"),
                Player(id = "player-2", lives = 3, name = "Remote")
            )
        )
        coEvery { repository.connect() } throws RuntimeException("timeout") andThen Unit
        coEvery { repository.disconnect() } just Runs
        coEvery { repository.fetchGameState("game-1", "player-1") } returns state
        coEvery { repository.subscribeToPrivateEvents("game-1", "player-1") } returns activeJsonFlow()

        val viewModel = createViewModel(
            gameId = "game-1",
            initialLocalPlayerId = "player-1",
            localPlayerName = "Alex",
            repository = repository
        )
        advanceUntilIdle()

        assertEquals("player-1", viewModel.localPlayerId)
        coVerify(exactly = 2) { repository.connect() }
        coVerify(exactly = 1) { repository.disconnect() }
    }

    @Test
    fun `surfaces server error event in the in-game log`() = runTest {
        val state = gameState(
            players = arrayListOf(
                Player(id = "player-1", lives = 3, name = "Alex"),
                Player(id = "player-2", lives = 3, name = "Remote")
            )
        )
        coEvery { repository.fetchGameState("game-1", "player-1") } returns state
        coEvery { repository.subscribeToErrors("game-1", "player-1") } returns flow {
            emit("It is not your turn.")
            awaitCancellation()
        }

        val viewModel = createViewModel(
            gameId = "game-1",
            initialLocalPlayerId = "player-1",
            localPlayerName = "Alex",
            repository = repository
        )
        advanceUntilIdle()

        assertTrue(viewModel.log.value.any { it.contains("It is not your turn.") })
    }

    private fun gameState(players: ArrayList<Player>): GameState =
        GameState(
            id = "game-1",
            players = players,
            currentPlayerIndex = 0,
            status = Status.Playing,
            currentPlayerId = players.firstOrNull()?.id
        )

    private fun createViewModel(
        gameId: String,
        initialLocalPlayerId: String,
        localPlayerName: String,
        repository: GameRepository
    ): GameBoardViewModel =
        GameBoardViewModel(
            gameId = gameId,
            initialLocalPlayerId = initialLocalPlayerId,
            localPlayerName = localPlayerName,
            repository = repository
        ).also(createdViewModels::add)

    private fun clearViewModel(viewModel: GameBoardViewModel) {
        GameBoardViewModel::class.java.getDeclaredMethod("onCleared")
            .apply { isAccessible = true }
            .invoke(viewModel)
    }

    private fun activeGameStateFlow() = flow<GameState> { awaitCancellation() }

    private fun activeStringFlow() = flow<String> { awaitCancellation() }

    private fun activeJsonFlow() = flow<org.json.JSONObject> { awaitCancellation() }
}
