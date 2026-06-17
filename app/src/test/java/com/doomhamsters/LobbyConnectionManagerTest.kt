package com.doomhamsters

import com.doomhamsters.data.Lobby
import io.mockk.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.*
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class LobbyConnectionManagerTest {

    private val testDispatcher = UnconfinedTestDispatcher()

    private lateinit var mockRepo: LobbyRepository
    private lateinit var connectionFlow: MutableStateFlow<Boolean>
    private lateinit var infoMessageFlow: MutableStateFlow<String?>

    private var currentLobby: Lobby? = null
    private val capturedSnapshots = mutableListOf<Lobby>()

    @BeforeEach
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        mockRepo = mockk(relaxed = true)
        connectionFlow = MutableStateFlow(false)
        infoMessageFlow = MutableStateFlow(null)
        capturedSnapshots.clear()
        currentLobby = null
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createManager(scope: CoroutineScope, enableRefresh: Boolean = false): LobbyConnectionManager {
        return LobbyConnectionManager(
            repository = mockRepo,
            connectionFlow = connectionFlow,
            infoMessageFlow = infoMessageFlow,
            enableLobbyRefresh = enableRefresh,
            scope = scope,
            getCurrentLobby = { currentLobby },
            onLobbySnapshot = { capturedSnapshots.add(it) }
        )
    }

    @Test
    fun `observeLobby sets observedLobbyId and triggers repository subscriptions`() = runTest(testDispatcher) {
        val manager = createManager(backgroundScope)
        coEvery { mockRepo.connect() } just Runs
        coEvery { mockRepo.subscribeLobbyUpdates(any()) } returns emptyFlow()
        coEvery { mockRepo.subscribeGameStart(any()) } returns emptyFlow()

        manager.observeLobby("ROOM123")
        runCurrent()

        assertEquals("ROOM123", manager.observedLobbyId)
        coVerify { mockRepo.subscribeLobbyUpdates("ROOM123") }
        coVerify { mockRepo.subscribeGameStart("ROOM123") }
    }

    @Test
    fun `observeLobby returns early if same lobby and listeners are already active`() = runTest(testDispatcher) {
        val manager = createManager(backgroundScope)
        coEvery { mockRepo.connect() } just Runs
        coEvery { mockRepo.isConnected() } returns false andThen true
        coEvery { mockRepo.subscribeLobbyUpdates(any()) } returns MutableSharedFlow()
        coEvery { mockRepo.subscribeGameStart(any()) } returns MutableSharedFlow()

        manager.observeLobby("ROOM123")
        runCurrent()

        manager.lobbyRefreshJob = Job()

        manager.observeLobby("ROOM123")
        runCurrent()

        coVerify(exactly = 1) { mockRepo.connect() }

        manager.lobbyRefreshJob?.cancel()
    }

    @Test
    fun `ensureRealtimeLobbyObservers connects repository if it is disconnected`() = runTest(testDispatcher) {
        val manager = createManager(backgroundScope)
        coEvery { mockRepo.isConnected() } returns false andThen true
        coEvery { mockRepo.connect() } just Runs
        coEvery { mockRepo.subscribeLobbyUpdates(any()) } returns emptyFlow()
        coEvery { mockRepo.subscribeGameStart(any()) } returns emptyFlow()

        manager.ensureRealtimeLobbyObservers("ROOM123")
        runCurrent()

        coVerify(exactly = 1) { mockRepo.connect() }
        assertTrue(connectionFlow.value)
    }

    @Test
    fun `ensureRealtimeLobbyObservers catches connection exception smoothly`() = runTest(testDispatcher) {
        val manager = createManager(backgroundScope)
        coEvery { mockRepo.isConnected() } returns false
        coEvery { mockRepo.connect() } throws RuntimeException("Network dead")

        manager.ensureRealtimeLobbyObservers("ROOM123")
        runCurrent()

        assertFalse(connectionFlow.value)
        coVerify { mockRepo.connect() }
    }

    @Test
    fun `cancelLobbyObservers successfully cancels running coroutine jobs`() = runTest(testDispatcher) {
        val manager = createManager(backgroundScope)
        coEvery { mockRepo.connect() } just Runs
        coEvery { mockRepo.subscribeLobbyUpdates(any()) } returns MutableSharedFlow()
        coEvery { mockRepo.subscribeGameStart(any()) } returns MutableSharedFlow()

        manager.observeLobby("ROOM123")
        runCurrent()

        val updatesJob = manager.lobbyUpdatesJob
        val startJob = manager.gameStartJob

        assertNotNull(updatesJob)
        assertNotNull(startJob)

        manager.cancelLobbyObservers()
        runCurrent()

        assertTrue(updatesJob?.isCancelled == true)
        assertTrue(startJob?.isCancelled == true)
        assertNull(manager.lobbyUpdatesJob)
        assertNull(manager.gameStartJob)
    }

    @Test
    fun `incoming lobby updates invoke onLobbySnapshot callback`() = runTest(testDispatcher) {
        val manager = createManager(backgroundScope)
        val lobbyUpdateFlow = MutableSharedFlow<Lobby>()
        coEvery { mockRepo.connect() } just Runs
        coEvery { mockRepo.subscribeLobbyUpdates(any()) } returns lobbyUpdateFlow
        coEvery { mockRepo.subscribeGameStart(any()) } returns emptyFlow()

        manager.observeLobby("ROOM123")
        runCurrent()

        val testLobby = Lobby(lobbyId = "ROOM123", members = emptyList(), qrCodeBase64 = "")
        lobbyUpdateFlow.emit(testLobby)
        runCurrent()

        assertEquals(1, capturedSnapshots.size)
        assertEquals("ROOM123", capturedSnapshots.first().lobbyId)
    }

    @Test
    fun `launchLobbyUpdates sets info message and nulls observedLobbyId on flow error`() = runTest(testDispatcher) {
        val manager = createManager(backgroundScope)
        val errorFlow = flow<Lobby> { throw RuntimeException("Flow broken") }
        coEvery { mockRepo.subscribeLobbyUpdates(any()) } returns errorFlow

        manager.observedLobbyId = "ROOM123"
        manager.launchLobbyUpdates("ROOM123")
        runCurrent()

        assertEquals("Verbindung wird im Hintergrund aktualisiert...", infoMessageFlow.value)
        assertNull(manager.observedLobbyId)
    }

    @Test
    fun `launchLobbyUpdates catches structural setup exceptions`() = runTest(testDispatcher) {
        val manager = createManager(backgroundScope)
        coEvery { mockRepo.subscribeLobbyUpdates(any()) } throws RuntimeException("Fatal repository crash")

        manager.observedLobbyId = "ROOM123"
        manager.launchLobbyUpdates("ROOM123")
        runCurrent()

        assertEquals("Verbindung wird im Hintergrund aktualisiert...", infoMessageFlow.value)
        assertNull(manager.observedLobbyId)
    }

    @Test
    fun `launchGameStartListener sets info message on flow error`() = runTest(testDispatcher) {
        val manager = createManager(backgroundScope)
        val errorFlow = flow<String> { throw RuntimeException("Game start channel failed") }
        coEvery { mockRepo.subscribeGameStart(any()) } returns errorFlow

        manager.observedLobbyId = "ROOM123"
        manager.launchGameStartListener("ROOM123")
        runCurrent()

        assertEquals("Spielstart-Synchronisierung eingeschränkt.", infoMessageFlow.value)
        assertNull(manager.observedLobbyId)
    }

    @Test
    fun `clearObservedLobby resets state and connection status`() = runTest(testDispatcher) {
        val manager = createManager(backgroundScope)
        manager.observedLobbyId = "ROOM123"
        connectionFlow.value = true

        manager.clearObservedLobby()
        runCurrent()

        assertNull(manager.observedLobbyId)
        assertFalse(connectionFlow.value)
    }
}