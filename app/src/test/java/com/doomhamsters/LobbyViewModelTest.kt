package com.doomhamsters

import com.doomhamsters.data.Lobby
import com.doomhamsters.data.User
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerifyOrder
import io.mockk.coVerify
import io.mockk.just
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class LobbyViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var mockRepo: LobbyRepository
    private lateinit var viewModel: LobbyViewModel
    private lateinit var gameStartFlow: MutableSharedFlow<String>

    private val fakeLobby = Lobby(
        lobbyId = "DOOMUNIT",
        members = listOf(User("fixed-id", "Christian", "hamster")),
        qrCodeBase64 = "base64qr=="
    )
    private val startableLobby = Lobby(
        lobbyId = "DOOMUNIT",
        members = listOf(
            User("fixed-id", "Christian", "hamster"),
            User("guest-id", "Anna", "dog")
        ),
        qrCodeBase64 = "base64qr=="
    )

    @BeforeEach
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        mockRepo = mockk(relaxed = true)
        gameStartFlow = MutableSharedFlow()
        coEvery { mockRepo.subscribeGameStart(any()) } returns gameStartFlow
        viewModel = LobbyViewModel(
            repository = mockRepo,
            userId = "fixed-id",
            enableLobbyRefresh = false
        )
    }

    @AfterEach
    fun tearDown() {
        LobbyViewModel::class.java.getDeclaredMethod("onCleared")
            .apply { isAccessible = true }
            .invoke(viewModel)
        Dispatchers.resetMain()
    }

    @Test
    fun `initial status is correct`() {
        assertEquals(1, viewModel.currentStep)
        assertEquals("dog", viewModel.selectedAvatar)
        assertNull(viewModel.lobby.value)
        assertNull(viewModel.error.value)
        assertNull(viewModel.activeGameSession.value)
    }

    @Test
    fun `createGroup does nothing when username is blank`() = runTest {
        viewModel.groupName = "DoomUnit"
        viewModel.username = ""

        viewModel.createGroup()

        assertEquals(1, viewModel.currentStep)
        assertNull(viewModel.lobby.value)
    }

    @Test
    fun `createGroup does nothing when groupName is blank`() = runTest {
        viewModel.username = "Christian"
        viewModel.groupName = ""

        viewModel.createGroup()

        assertEquals(1, viewModel.currentStep)
    }

    @Test
    fun `createGroup switches to step 3 on success`() = runTest {
        coEvery { mockRepo.connect() } just Runs
        coEvery { mockRepo.createLobby(any(), any()) } returns fakeLobby
        coEvery { mockRepo.subscribeLobbyUpdates(any()) } returns emptyFlow()

        viewModel.username = "Christian"
        viewModel.groupName = "DoomUnit"
        viewModel.selectedAvatar = "hamster"

        viewModel.createGroup()
        advanceUntilIdle()

        assertEquals(3, viewModel.currentStep)
        assertEquals(fakeLobby, viewModel.lobby.value)
        assertNull(viewModel.error.value)
        assertFalse(viewModel.isProfileActionInProgress)
    }

    @Test
    fun `createGroup sends correct user to repository`() = runTest {
        coEvery { mockRepo.connect() } just Runs
        coEvery { mockRepo.createLobby(any(), any()) } returns fakeLobby
        coEvery { mockRepo.subscribeLobbyUpdates(any()) } returns emptyFlow()

        viewModel.username = "Christian"
        viewModel.groupName = "DoomUnit"
        viewModel.selectedAvatar = "hamster"

        viewModel.createGroup()
        advanceUntilIdle()

        coVerify {
            mockRepo.createLobby("DoomUnit", User("fixed-id", "Christian", "hamster"))
        }
    }

    @Test
    fun `createGroup sets error on network failure`() = runTest {
        coEvery { mockRepo.createLobby(any(), any()) } throws RuntimeException("No network")

        viewModel.username = "Christian"
        viewModel.groupName = "DoomUnit"

        viewModel.createGroup()
        advanceUntilIdle()

        assertEquals("Etwas ist schiefgelaufen: No network", viewModel.error.value)
        assertEquals(1, viewModel.currentStep)
    }

    @Test
    fun `lobby updates refresh member list`() = runTest {
        val lobbyFlow = MutableSharedFlow<Lobby>()
        coEvery { mockRepo.connect() } just Runs
        coEvery { mockRepo.createLobby(any(), any()) } returns fakeLobby
        coEvery { mockRepo.subscribeLobbyUpdates(any()) } returns lobbyFlow

        viewModel.username = "Christian"
        viewModel.groupName = "DoomUnit"
        viewModel.createGroup()
        advanceUntilIdle()

        val updatedLobby = fakeLobby.copy(
            members = listOf(
                User("fixed-id", "Christian", "hamster"),
                User("u2", "Gast", "dog")
            )
        )
        lobbyFlow.emit(updatedLobby)
        advanceUntilIdle()

        assertEquals(2, viewModel.lobby.value?.members?.size)
    }

    @Test
    fun `lobby update with game id moves waiting clients into game`() = runTest {
        val lobbyFlow = MutableSharedFlow<Lobby>()
        coEvery { mockRepo.connect() } just Runs
        coEvery { mockRepo.joinLobby(any(), any()) } returns fakeLobby
        coEvery { mockRepo.subscribeLobbyUpdates(any()) } returns lobbyFlow

        viewModel.username = "Anna"
        viewModel.selectedAvatar = "hamster"

        viewModel.joinLobby("DOOMUNIT")
        advanceUntilIdle()

        lobbyFlow.emit(fakeLobby.copy(gameId = "shared-game-1", gameStarted = true))
        advanceUntilIdle()

        assertEquals(4, viewModel.currentStep)
        assertEquals("shared-game-1", viewModel.activeGameSession.value?.gameId)
    }

    @Test
    fun `joinLobby shows error when username is blank`() = runTest {
        viewModel.username = ""

        viewModel.joinLobby("DOOMUNIT")

        assertEquals("Bitte gib zuerst deinen Spielernamen ein!", viewModel.error.value)
        assertEquals(1, viewModel.currentStep)
        assertNull(viewModel.lobby.value)
    }

    @Test
    fun `joinLobby switches to step 3 on success`() = runTest {
        coEvery { mockRepo.connect() } just Runs
        coEvery { mockRepo.joinLobby(any(), any()) } returns fakeLobby
        coEvery { mockRepo.subscribeLobbyUpdates(any()) } returns emptyFlow()

        viewModel.username = "Anna"
        viewModel.selectedAvatar = "hamster"

        viewModel.joinLobby("DOOMUNIT")
        advanceUntilIdle()

        assertEquals(3, viewModel.currentStep)
        assertEquals(fakeLobby, viewModel.lobby.value)
        assertNull(viewModel.error.value)

        coVerify {
            mockRepo.joinLobby("DOOMUNIT", User("fixed-id", "Anna", "hamster"))
        }
    }

    @Test
    fun `joinLobby keeps step 4 when joined lobby is already started`() = runTest {
        coEvery { mockRepo.connect() } just Runs
        coEvery { mockRepo.joinLobby(any(), any()) } returns fakeLobby.copy(
            gameId = "shared-game-1",
            gameStarted = true
        )
        coEvery { mockRepo.subscribeLobbyUpdates(any()) } returns emptyFlow()

        viewModel.username = "Anna"
        viewModel.selectedAvatar = "hamster"

        viewModel.joinLobby("DOOMUNIT")
        advanceUntilIdle()

        assertEquals(4, viewModel.currentStep)
        assertEquals("shared-game-1", viewModel.activeGameSession.value?.gameId)
    }

    @Test
    fun `joinLobby connects before sending join request and starts subscriptions`() = runTest {
        coEvery { mockRepo.connect() } just Runs
        coEvery { mockRepo.joinLobby(any(), any()) } returns fakeLobby
        coEvery { mockRepo.subscribeLobbyUpdates(any()) } returns emptyFlow()
        coEvery { mockRepo.subscribeGameStart(any()) } returns emptyFlow()

        viewModel.username = "Anna"
        viewModel.selectedAvatar = "hamster"

        viewModel.joinLobby("DOOMUNIT")
        advanceUntilIdle()

        coVerifyOrder {
            mockRepo.connect()
            mockRepo.joinLobby("DOOMUNIT", User("fixed-id", "Anna", "hamster"))
        }
        coVerify(atLeast = 1) { mockRepo.subscribeLobbyUpdates("DOOMUNIT") }
        coVerify(atLeast = 1) { mockRepo.subscribeGameStart("DOOMUNIT") }
    }

    @Test
    fun `joinLobby shows error when lobby does not exist`() = runTest {
        coEvery { mockRepo.connect() } just Runs
        coEvery { mockRepo.joinLobby(any(), any()) } returns null

        viewModel.username = "Anna"

        viewModel.joinLobby("FALSCHE_LOBBY")
        advanceUntilIdle()

        assertEquals("Lobby 'FALSCHE_LOBBY' wurde nicht gefunden!", viewModel.error.value)
        assertEquals(1, viewModel.currentStep)
    }

    @Test
    fun `joinLobby catches network errors`() = runTest {
        coEvery { mockRepo.connect() } just Runs
        coEvery { mockRepo.joinLobby(any(), any()) } throws RuntimeException("Server down")

        viewModel.username = "Anna"

        viewModel.joinLobby("DOOMUNIT")
        advanceUntilIdle()

        assertEquals("Etwas ist schiefgelaufen: Server down", viewModel.error.value)
        assertEquals(1, viewModel.currentStep)
    }

    @Test
    fun `startGame moves to step 4 when start signal arrives`() = runTest {
        coEvery { mockRepo.connect() } just Runs
        coEvery { mockRepo.disconnect() } just Runs
        coEvery { mockRepo.createLobby(any(), any()) } returns startableLobby
        coEvery { mockRepo.subscribeLobbyUpdates(any()) } returns emptyFlow()

        viewModel.username = "Hamster1"
        viewModel.groupName = "TestGruppe"
        viewModel.createGroup()
        advanceUntilIdle()

        gameStartFlow.emit("neue-game-uuid")
        advanceUntilIdle()

        assertEquals(4, viewModel.currentStep)
        assertEquals("neue-game-uuid", viewModel.activeGameSession.value?.gameId)
    }

    @Test
    fun `entering game disconnects lobby observers`() = runTest {
        coEvery { mockRepo.connect() } just Runs
        coEvery { mockRepo.disconnect() } just Runs
        coEvery { mockRepo.createLobby(any(), any()) } returns startableLobby
        coEvery { mockRepo.subscribeLobbyUpdates(any()) } returns emptyFlow()

        viewModel.username = "Hamster1"
        viewModel.groupName = "TestGruppe"
        viewModel.createGroup()
        advanceUntilIdle()

        gameStartFlow.emit("neue-game-uuid")
        advanceUntilIdle()

        coVerify(atLeast = 1) { mockRepo.disconnect() }
        assertEquals(4, viewModel.currentStep)
    }

    @Test
    fun `startGame calls triggerGameStart in repository`() = runTest {
        coEvery { mockRepo.connect() } just Runs
        coEvery { mockRepo.disconnect() } just Runs
        coEvery { mockRepo.createLobby(any(), any()) } returns startableLobby
        coEvery { mockRepo.subscribeLobbyUpdates(any()) } returns emptyFlow()
        coEvery { mockRepo.subscribeGameStart(any()) } returns emptyFlow()
        coEvery { mockRepo.getLobby(any()) } returns
            startableLobby.copy(
                gameId = "game-1",
                gameStarted = true
        )

        viewModel.username = "Christian"
        viewModel.groupName = "DoomUnit"
        viewModel.createGroup()
        advanceUntilIdle()

        viewModel.startGame()
        advanceUntilIdle()

        coVerify { mockRepo.triggerGameStart("DOOMUNIT", "fixed-id") }
        assertFalse(viewModel.isStartingGame)
    }

    @Test
    fun `active game session is set when server sends start signal`() = runTest {
        val startFlow = MutableSharedFlow<String>()
        coEvery { mockRepo.connect() } just Runs
        coEvery { mockRepo.createLobby(any(), any()) } returns startableLobby
        coEvery { mockRepo.subscribeLobbyUpdates(any()) } returns emptyFlow()
        coEvery { mockRepo.subscribeGameStart(any()) } returns startFlow

        viewModel.username = "Christian"
        viewModel.groupName = "DoomUnit"
        viewModel.createGroup()
        advanceUntilIdle()

        startFlow.emit("MEINE_TEST_UUID")
        advanceUntilIdle()

        assertEquals(4, viewModel.currentStep)
        assertEquals("MEINE_TEST_UUID", viewModel.activeGameSession.value?.gameId)
        assertEquals("fixed-id", viewModel.activeGameSession.value?.playerId)
    }

    @Test
    fun `returnToLobbyAfterGame clears active session and keeps lobby screen`() = runTest {
        coEvery { mockRepo.connect() } just Runs
        coEvery { mockRepo.disconnect() } just Runs
        coEvery { mockRepo.createLobby(any(), any()) } returns startableLobby
        coEvery { mockRepo.subscribeLobbyUpdates(any()) } returns emptyFlow()

        viewModel.username = "Christian"
        viewModel.groupName = "DoomUnit"
        viewModel.createGroup()
        advanceUntilIdle()

        gameStartFlow.emit("MEINE_TEST_UUID")
        advanceUntilIdle()

        viewModel.returnToLobbyAfterGame()
        advanceUntilIdle()

        assertEquals(3, viewModel.currentStep)
        assertNull(viewModel.activeGameSession.value)
        coVerify(atLeast = 2) { mockRepo.connect() }
    }

    @Test
    fun `startGame is blocked until at least two players are present`() = runTest {
        coEvery { mockRepo.connect() } just Runs
        coEvery { mockRepo.createLobby(any(), any()) } returns fakeLobby
        coEvery { mockRepo.subscribeLobbyUpdates(any()) } returns emptyFlow()

        viewModel.username = "Christian"
        viewModel.groupName = "DoomUnit"
        viewModel.createGroup()
        advanceUntilIdle()

        viewModel.startGame()

        coVerify(exactly = 0) { mockRepo.triggerGameStart(any(), any()) }
        assertEquals("Mindestens 2 Spieler werden zum Starten benotigt.", viewModel.error.value)
    }

    @Test
    fun `startGame stays blocked until current player is part of lobby members`() = runTest {
        val otherPlayersLobby = Lobby(
            lobbyId = "DOOMUNIT",
            members = listOf(
                User("guest-id", "Anna", "dog"),
                User("guest-2", "Chris", "fox")
            ),
            qrCodeBase64 = "base64qr=="
        )
        coEvery { mockRepo.connect() } just Runs
        coEvery { mockRepo.createLobby(any(), any()) } returns otherPlayersLobby
        coEvery { mockRepo.subscribeLobbyUpdates(any()) } returns emptyFlow()

        viewModel.username = "Christian"
        viewModel.groupName = "DoomUnit"
        viewModel.createGroup()
        advanceUntilIdle()

        viewModel.startGame()

        coVerify(exactly = 0) { mockRepo.triggerGameStart(any(), any()) }
        assertEquals("Warte auf die Lobby-Synchronisierung.", viewModel.error.value)
    }

    @Test
    fun `onCleared calls disconnect`() = runTest {
        coEvery { mockRepo.disconnect() } just Runs

        LobbyViewModel::class.java.getDeclaredMethod("onCleared")
            .apply { isAccessible = true }
            .invoke(viewModel)

        coVerify { mockRepo.disconnect() }
    }

    @Test
    fun `leaveLobby calls repository and resets state`() = runTest {
        coEvery { mockRepo.connect() } just Runs
        coEvery { mockRepo.createLobby(any(), any()) } returns fakeLobby
        coEvery { mockRepo.subscribeLobbyUpdates(any()) } returns emptyFlow()

        viewModel.username = "Michelle"
        viewModel.groupName = "DoomUnit"
        viewModel.createGroup()
        advanceUntilIdle()
        assertEquals(3, viewModel.currentStep)

        viewModel.leaveLobby()
        advanceUntilIdle()

        coVerify { mockRepo.leaveLobby("DOOMUNIT", "fixed-id") }
        coVerify { mockRepo.disconnect() }
        assertNull(viewModel.lobby.value)
        assertEquals(1, viewModel.currentStep)
    }

    @Test
    fun `leaveLobby resets state even on network error`() = runTest {
        coEvery { mockRepo.createLobby(any(), any()) } returns fakeLobby
        viewModel.username = "Michelle"
        viewModel.groupName = "DoomUnit"
        viewModel.createGroup()
        advanceUntilIdle()

        coEvery { mockRepo.leaveLobby(any(), any()) } throws RuntimeException("Network error")

        viewModel.leaveLobby()
        advanceUntilIdle()

        assertEquals(1, viewModel.currentStep)
        assertNull(viewModel.lobby.value)
    }

    @Test
    fun `retryLastAction retries the failed createGroup action`() = runTest {
        coEvery { mockRepo.createLobby(any(), any()) } throws RuntimeException("First attempt failed") andThen fakeLobby
        coEvery { mockRepo.connect() } just Runs
        coEvery { mockRepo.subscribeLobbyUpdates(any()) } returns emptyFlow()

        viewModel.username = "Michelle"
        viewModel.groupName = "DoomUnit"

        // First attempt
        viewModel.createGroup()
        advanceUntilIdle()
        assertEquals("Etwas ist schiefgelaufen: First attempt failed", viewModel.error.value)

        // Retry
        viewModel.retryLastAction()
        advanceUntilIdle()

        assertNull(viewModel.error.value)
        assertEquals(3, viewModel.currentStep)
        assertEquals(fakeLobby, viewModel.lobby.value)
    }

    @Test
    fun `clearError resets error flow`() {
        viewModel.joinLobby("") // Triggers error
        assertEquals("Bitte gib eine gultige Lobby-ID ein!", viewModel.error.value)

        viewModel.clearError()
        assertNull(viewModel.error.value)
    }

    @Test
    fun `clearInfoMessage resets infoMessage flow`() = runTest {
        val lobbyFlow = MutableSharedFlow<Lobby>()
        coEvery { mockRepo.connect() } just Runs
        coEvery { mockRepo.createLobby(any(), any()) } returns fakeLobby
        coEvery { mockRepo.subscribeLobbyUpdates(any()) } throws RuntimeException("Update failed")

        viewModel.username = "Michelle"
        viewModel.groupName = "DoomUnit"
        viewModel.createGroup()
        advanceUntilIdle()

        assertEquals("Verbindung wird im Hintergrund aktualisiert...", viewModel.infoMessage.value)

        viewModel.clearInfoMessage()
        assertNull(viewModel.infoMessage.value)
    }

    @Test
    fun `isLoading is true during createGroup and false after`() = runTest {
        coEvery { mockRepo.createLobby(any(), any()) } coAnswers {
            delay(1000)
            fakeLobby
        }
        coEvery { mockRepo.connect() } just Runs

        viewModel.username = "Michelle"
        viewModel.groupName = "DoomUnit"

        val job = launch { viewModel.createGroup() }
        runCurrent()
        assertEquals(true, viewModel.isLoading.value)

        advanceUntilIdle()
        assertEquals(false, viewModel.isLoading.value)
        job.join()
    }
}
