package com.doomhamsters.viewmodel
import com.doomhamsters.GameRepository
import com.doomhamsters.model.GameState
import com.doomhamsters.model.Player
import com.doomhamsters.model.Status
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.just
import io.mockk.mockk
import io.mockk.Runs
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
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
import java.io.IOException
@OptIn(ExperimentalCoroutinesApi::class)
class GameConnectionManagerTest {
    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var repository: GameRepository
    private val createdViewModels = mutableListOf<GameBoardViewModel>()

    @BeforeEach
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        repository = mockk(relaxed = true)
        coEvery { repository.connect() } just Runs
        coEvery { repository.disconnect() } just Runs
        coEvery { repository.fetchGameState(any(), any()) } returns defaultGameState()
        coEvery { repository.subscribeToGameState(any()) } returns activeGameStateFlow()
        coEvery { repository.subscribeToGame(any()) } returns activeStringFlow()
        coEvery { repository.subscribeToPrivateEvents(any(), any()) } returns activeJsonFlow()
    }

    @AfterEach
    fun tearDown() {
        createdViewModels.forEach(::clearViewModel)
        createdViewModels.clear()
        Dispatchers.resetMain()
    }

    @Test
    fun `connection status is Connected after successful initial setup`() = runTest {
        val vm = createViewModel()
        advanceUntilIdle()

        assertEquals(ConnectionStatus.Connected, vm.connectionStatus.value)
    }
        @Test
        fun `status is Reconnecting after a subscription failure`() = runTest {
            coEvery { repository.subscribeToGameState(any()) } returns flow {
                throw IOException("dropped")
            }
            val connectBarrier = CompletableDeferred<Unit>()
            var connectCount = 0
            coEvery { repository.connect() } coAnswers {
                connectCount++
                if (connectCount > 1) connectBarrier.await()
            }

            val manager = GameConnectionManager("game-1", repository)

            val job = launch {
                manager.connectAndMaintain(
                    localPlayerId = "player-1",
                    localPlayerName = "Astrobot",
                    callbacks = GameConnectionCallbacks(
                        onInitialConnect = {},
                        onReconnect = {},
                        onGameStateReceived = {},
                        onPublicEventReceived = {},
                        onPrivateEventReceived = {},
                        onServerError = {},
                        onFatalError = {},
                        onLog = {}
                    )
                )
            }

            advanceUntilIdle()

            assertEquals(ConnectionStatus.Reconnecting(1, 3), manager.connectionStatus.value)
            connectBarrier.complete(Unit)
            job.cancel()
        }


    @Test
    fun `status returns to Connected after a successful reconnect`() = runTest {
        var subscribeCount = 0
        coEvery { repository.subscribeToGameState(any()) } answers {
            subscribeCount++
            if (subscribeCount == 1) flow { throw IOException("dropped") } else activeGameStateFlow()
        }

        val vm = createViewModel()
        advanceUntilIdle()

        assertEquals(ConnectionStatus.Connected, vm.connectionStatus.value)
    }

    @Test
    fun `status returns to Connected after subscriptions complete and reconnect`() = runTest {
        var gameStateSubscribeCount = 0
        var publicSubscribeCount = 0
        var privateSubscribeCount = 0
        coEvery { repository.subscribeToGameState(any()) } answers {
            gameStateSubscribeCount++
            if (gameStateSubscribeCount == 1) flow { } else activeGameStateFlow()
        }
        coEvery { repository.subscribeToGame(any()) } answers {
            publicSubscribeCount++
            if (publicSubscribeCount == 1) flow { } else activeStringFlow()
        }
        coEvery { repository.subscribeToPrivateEvents(any(), any()) } answers {
            privateSubscribeCount++
            if (privateSubscribeCount == 1) flow { } else activeJsonFlow()
        }

        val vm = createViewModel()
        advanceUntilIdle()

        assertEquals(ConnectionStatus.Connected, vm.connectionStatus.value)
        coVerify(atLeast = 2) { repository.connect() }
    }

    @Test
    fun `attempt number increments when reconnect itself fails`() = runTest {
        coEvery { repository.subscribeToGameState(any()) } returns flow {
            throw IOException("dropped")
        }

        val secondReconnectBarrier = CompletableDeferred<Unit>()
        var connectCount = 0
        coEvery { repository.connect() } coAnswers {
            connectCount++
            when (connectCount) {
                1 -> Unit
                2 -> throw IOException("still down")
                else -> secondReconnectBarrier.await()
            }
        }

        val manager = GameConnectionManager("game-1", repository)

        val job = launch {
            manager.connectAndMaintain(
                localPlayerId = "player-1",
                localPlayerName = "Astrobot",
                callbacks = GameConnectionCallbacks(
                    onInitialConnect = {},
                    onReconnect = {},
                    onGameStateReceived = {},
                    onPublicEventReceived = {},
                    onPrivateEventReceived = {},
                    onServerError = {},
                    onFatalError = {},
                    onLog = {}
                )
            )
        }

        advanceUntilIdle()
        assertEquals(ConnectionStatus.Reconnecting(2, 3), manager.connectionStatus.value)
        secondReconnectBarrier.complete(Unit)
        job.cancel()
    }

    @Test
    fun `status becomes Failed after all reconnect attempts are exhausted`() = runTest {
        coEvery { repository.subscribeToGameState(any()) } returns flow {
            throw IOException("dropped")
        }
        var connectCount = 0
        coEvery { repository.connect() } coAnswers {
            connectCount++
            if (connectCount > 1) throw IOException("server down")
        }

        val vm = createViewModel()
        advanceUntilIdle()

        assertEquals(ConnectionStatus.Failed, vm.connectionStatus.value)
    }

    @Test
    fun `status becomes Failed when initial connection cannot be established`() = runTest {
        coEvery { repository.connect() } throws IOException("no route to host")

        val vm = createViewModel()
        advanceUntilIdle()

        assertEquals(ConnectionStatus.Failed, vm.connectionStatus.value)
    }

    @Test
    fun `game state is fetched from REST after reconnect to restore state`() = runTest {
        var subscribeCount = 0
        coEvery { repository.subscribeToGameState(any()) } answers {
            subscribeCount++
            if (subscribeCount == 1) flow { throw IOException("dropped") } else activeGameStateFlow()
        }

        createViewModel()
        advanceUntilIdle()

        coVerify(atLeast = 2) { repository.fetchGameState(any(), any()) }
    }

    @Test
    fun `disconnect is called on the repository before each reconnect attempt`() = runTest {
        var subscribeCount = 0
        coEvery { repository.subscribeToGameState(any()) } answers {
            subscribeCount++
            if (subscribeCount == 1) flow { throw IOException("dropped") } else activeGameStateFlow()
        }

        createViewModel()
        advanceUntilIdle()

        coVerify(atLeast = 1) { repository.disconnect() }
    }

    @Test
    fun `reconnected log message appears after recovery`() = runTest {
        var subscribeCount = 0
        coEvery { repository.subscribeToGameState(any()) } answers {
            subscribeCount++
            if (subscribeCount == 1) flow { throw IOException("dropped") } else activeGameStateFlow()
        }

        val vm = createViewModel()
        advanceUntilIdle()

        assertTrue(vm.log.value.contains("Reconnected."))
    }
    @Test
    fun `retries initial connection before surfacing failure`() = runTest {

        coEvery { repository.connect() } throws RuntimeException("timeout") andThen Unit
        coEvery { repository.disconnect() } just Runs


        val manager = GameConnectionManager("game-1", repository)


        val job = launch {
            manager.connectAndMaintain(
                localPlayerId = "player-1",
                localPlayerName = "Alex",
                callbacks = GameConnectionCallbacks(
                    onInitialConnect = {},
                    onReconnect = {},
                    onGameStateReceived = {},
                    onPublicEventReceived = {},
                    onPrivateEventReceived = {},
                    onServerError = {},
                    onFatalError = {},
                    onLog = {}
                )
            )
        }

        advanceUntilIdle()
        coVerify(exactly = 2) { repository.connect() }
        coVerify(exactly = 1) { repository.disconnect() }
        job.cancel()
    }

    private fun defaultGameState() = GameState(
        id = "game-1",
        players = arrayListOf(
            Player(id = "player-1", lives = 3, name = "Astrobot"),
            Player(id = "player-2", lives = 3, name = "JustABot")
        ),
        currentPlayerIndex = 0,
        status = Status.Playing,
        currentPlayerId = "player-1"
    )

    private fun activeGameStateFlow() = flow<GameState> { awaitCancellation() }

    private fun activeStringFlow() = flow<String> { awaitCancellation() }

    private fun activeJsonFlow() = flow<org.json.JSONObject> { awaitCancellation() }

    private fun createViewModel() = GameBoardViewModel(
        gameId = "game-1",
        initialLocalPlayerId = "player-1",
        localPlayerName = "Astrobot",
        repository = repository
    ).also(createdViewModels::add)

    private fun clearViewModel(viewModel: GameBoardViewModel) {
        GameBoardViewModel::class.java.getDeclaredMethod("onCleared")
            .apply { isAccessible = true }
            .invoke(viewModel)
    }

}