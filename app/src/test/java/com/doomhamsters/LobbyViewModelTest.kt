package com.doomhamsters
import com.doomhamsters.data.Lobby
import com.doomhamsters.data.User
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.Runs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class LobbyViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var mockRepo: LobbyRepository
    private lateinit var viewModel: LobbyViewModel
    private lateinit var gameStartFlow: MutableSharedFlow<String>

    private val fakeLobby = Lobby(
        lobbyId = "DOOMUNIT",
        members = listOf(User("fixed-id", "Christian", "🐱")),
        qrCodeBase64 = "base64qr=="
    )

    @BeforeEach
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        mockRepo = mockk(relaxed = true)
        gameStartFlow = MutableSharedFlow<String>()
        coEvery { mockRepo.subscribeGameStart(any()) } returns gameStartFlow
        viewModel = LobbyViewModel(repository = mockRepo, userId = "fixed-id")
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // ── initial state ─────────────────────────────────────────────────────────

    @Test
    fun `initialer Status ist korrekt`() {
        assertEquals(1, viewModel.currentStep)
        assertEquals("", viewModel.username)
        assertEquals("", viewModel.groupName)
        assertEquals("dog", viewModel.selectedAvatar)
        assertNull(viewModel.lobby.value)
        assertNull(viewModel.error.value)
    }

    // ── createGroup ───────────────────────────────────────────────────────────

    @Test
    fun `createGroup macht nichts wenn username leer ist`() = runTest {
        viewModel.groupName = "DoomUnit"
        viewModel.username = ""

        viewModel.createGroup()

        assertEquals(1, viewModel.currentStep)
        assertNull(viewModel.lobby.value)
    }

    @Test
    fun `createGroup macht nichts wenn groupName leer ist`() = runTest {
        viewModel.username = "Christian"
        viewModel.groupName = ""

        viewModel.createGroup()

        assertEquals(1, viewModel.currentStep)
    }

    @Test
    fun `createGroup wechselt zu Step 3 bei erfolgreicher Netzwerkantwort`() = runTest {
        coEvery { mockRepo.connect() } just Runs
        coEvery { mockRepo.createLobby(any(), any()) } returns fakeLobby
        coEvery { mockRepo.subscribeLobbyUpdates(any()) } returns emptyFlow()

        viewModel.username = "Christian"
        viewModel.groupName = "DoomUnit"
        viewModel.selectedAvatar = "🐱"

        viewModel.createGroup()

        assertEquals(3, viewModel.currentStep)
        assertEquals(fakeLobby, viewModel.lobby.value)
        assertNull(viewModel.error.value)
    }

    @Test
    fun `createGroup sendet korrekten User an Repository`() = runTest {
        coEvery { mockRepo.connect() } just Runs
        coEvery { mockRepo.createLobby(any(), any()) } returns fakeLobby
        coEvery { mockRepo.subscribeLobbyUpdates(any()) } returns emptyFlow()

        viewModel.username = "Christian"
        viewModel.groupName = "DoomUnit"
        viewModel.selectedAvatar = "🐱"

        viewModel.createGroup()

        coVerify {
            mockRepo.createLobby("DoomUnit", User("fixed-id", "Christian", "🐱"))
        }
    }

    @Test
    fun `createGroup setzt Fehlermeldung bei Netzwerkfehler`() = runTest {
        coEvery { mockRepo.connect() } throws RuntimeException("No network")

        viewModel.username = "Christian"
        viewModel.groupName = "DoomUnit"

        viewModel.createGroup()

        assertEquals("No network", viewModel.error.value)
        assertEquals(1, viewModel.currentStep)
    }

    @Test
    fun `lobby wird aktualisiert wenn Subscription neue Daten sendet`() = runTest {
        val lobbyFlow = MutableSharedFlow<Lobby>()
        coEvery { mockRepo.connect() } just Runs
        coEvery { mockRepo.createLobby(any(), any()) } returns fakeLobby
        coEvery { mockRepo.subscribeLobbyUpdates(any()) } returns lobbyFlow

        viewModel.username = "Christian"
        viewModel.groupName = "DoomUnit"
        viewModel.createGroup()

        val updatedLobby = fakeLobby.copy(
            members = listOf(
                User("fixed-id", "Christian", "🐱"),
                User("u2", "Gast", "🐶")
            )
        )
        lobbyFlow.emit(updatedLobby)

        assertEquals(2, viewModel.lobby.value?.members?.size)
    }
    // ── joinLobby ─────────────────────────────────────────────────────────────

    @Test
    fun `joinLobby zeigt Fehler wenn username leer ist`() = runTest {
        viewModel.username = "" // Name fehlt

        viewModel.joinLobby("DOOMUNIT")

        assertEquals("Bitte gib zuerst deinen Spielernamen ein!", viewModel.error.value)
        assertEquals(1, viewModel.currentStep)
        assertNull(viewModel.lobby.value)
    }

    @Test
    fun `joinLobby wechselt zu Step 3 bei erfolgreichem Beitritt`() = runTest {
        coEvery { mockRepo.connect() } just Runs
        coEvery { mockRepo.joinLobby(any(), any()) } returns fakeLobby
        coEvery { mockRepo.subscribeLobbyUpdates(any()) } returns emptyFlow()

        viewModel.username = "Anna"
        viewModel.selectedAvatar = "🐱"

        viewModel.joinLobby("DOOMUNIT")

        assertEquals(3, viewModel.currentStep)
        assertEquals(fakeLobby, viewModel.lobby.value)
        assertNull(viewModel.error.value)

        // Prüfen, ob die richtigen Daten ans Repository geschickt wurden
        coVerify {
            mockRepo.joinLobby("DOOMUNIT", User("fixed-id", "Anna", "🐱"))
        }
    }

    @Test
    fun `joinLobby zeigt Fehler wenn Lobby nicht existiert`() = runTest {
        coEvery { mockRepo.connect() } just Runs
        // Simulieren: Backend findet die Lobby nicht und gibt null zurück
        coEvery { mockRepo.joinLobby(any(), any()) } returns null

        viewModel.username = "Anna"

        viewModel.joinLobby("FALSCHE_LOBBY")

        assertEquals("Lobby 'FALSCHE_LOBBY' wurde nicht gefunden!", viewModel.error.value)
        assertEquals(1, viewModel.currentStep) // Bleibt im Startbildschirm
    }

    @Test
    fun `joinLobby fängt Netzwerkfehler ab`() = runTest {
        // Simulieren: Server ist offline
        coEvery { mockRepo.connect() } throws RuntimeException("Server down")

        viewModel.username = "Anna"

        viewModel.joinLobby("DOOMUNIT")

        assertEquals("Fehler beim Beitreten: Server down", viewModel.error.value)
        assertEquals(1, viewModel.currentStep)
    }

    // ── startGame ─────────────────────────────────────────────────────────────

    @Test
    fun `startGame wechselt zu Step 4`() = runTest {
        viewModel.username = "Hamster1"
        viewModel.groupName = "TestGruppe"
        viewModel.createGroup()
        advanceUntilIdle()
        gameStartFlow.emit("neue-game-uuid")
        advanceUntilIdle()
        assertEquals(4, viewModel.currentStep)
    }
    @Test
    fun `startGame ruft triggerGameStart im Repository auf`() = runTest {
        //  Setup
        coEvery { mockRepo.connect() } just Runs
        coEvery { mockRepo.createLobby(any(), any()) } returns fakeLobby
        coEvery { mockRepo.subscribeLobbyUpdates(any()) } returns emptyFlow()
        coEvery { mockRepo.subscribeGameStart(any()) } returns emptyFlow()

        // Daten setzen
        viewModel.username = "Christian"
        viewModel.groupName = "DoomUnit"

        // Lobby erstellen
        viewModel.createGroup()

        //Start-Knopf drücken
        viewModel.startGame()

        //Verifizieren
        coVerify { mockRepo.triggerGameStart("DOOMUNIT") }
    }
    @Test
    fun `Navigation wird getriggert wenn Server das Start-Signal sendet`() = runTest {
        // Setup
        val gameStartFlow = MutableSharedFlow<String>()
        coEvery { mockRepo.connect() } just Runs
        coEvery { mockRepo.createLobby(any(), any()) } returns fakeLobby
        coEvery { mockRepo.subscribeLobbyUpdates(any()) } returns emptyFlow()
        coEvery { mockRepo.subscribeGameStart(any()) } returns gameStartFlow

        // Beobachten des Navigationsflows in einer Liste
        val navEvents = mutableListOf<Pair<String, String>>()
        val collectJob = launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.navigateToGame.collect { navEvents.add(it) }
        }

        // Lobby erstellen
        viewModel.username = "Christian"
        viewModel.groupName = "DoomUnit"
        viewModel.createGroup()

        // Der Server schickt die Game-ID über den Kanal
        gameStartFlow.emit("MEINE_TEST_UUID")

        // Event bei der Ui angekommen?
        assertEquals(1, navEvents.size)
        assertEquals("MEINE_TEST_UUID", navEvents[0].first) // gameId
        assertEquals("fixed-id", navEvents[0].second)        // userId

        collectJob.cancel()
    }

    // ── onCleared ─────────────────────────────────────────────────────────────

    @Test
    fun `onCleared ruft disconnect auf`() = runTest {
        coEvery { mockRepo.disconnect() } just Runs

        // onCleared() is protected in ViewModel — invoke via reflection
        LobbyViewModel::class.java.getDeclaredMethod("onCleared")
            .apply { isAccessible = true }
            .invoke(viewModel)

        coVerify { mockRepo.disconnect() }
    }
}
