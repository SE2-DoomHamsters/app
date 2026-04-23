package com.doomhamsters
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
class LobbyViewModelTest {
    private lateinit var viewModel: LobbyViewModel

    @BeforeEach
    fun setup() {
        viewModel = LobbyViewModel()
    }

    @Test
    fun `initialer Status ist korrekt`() {
        assertEquals(1, viewModel.currentStep)
        assertEquals("", viewModel.username)
        assertEquals("", viewModel.groupName)
        assertNull(viewModel.lobby.value)
    }

    @Test
    fun `createGroup wechselt zu Step 2 wenn Eingaben gültig sind`() {
        // 1. Eingaben simulieren
        viewModel.username = "Christian"
        viewModel.groupName = "DoomUnit"
        viewModel.selectedAvatar = "🐱"

        // 2. Funktion ausführen
        viewModel.createGroup()

        // 3. Überprüfen
        assertEquals(2, viewModel.currentStep, "App sollte zu Step 2 wechseln")

        val aktuelleLobby = viewModel.lobby.value
        assertNotNull(aktuelleLobby, "Lobby-Objekt sollte erstellt worden sein")
        assertEquals("DOOMUNIT", aktuelleLobby?.lobbyId) // Prüft die .uppercase() Umwandlung
        assertEquals("Christian", aktuelleLobby?.members?.first()?.username)
    }

    @Test
    fun `createGroup macht nichts wenn Felder leer sind`() {
        // Nur ein Feld ausfüllen
        viewModel.username = "Christian"
        viewModel.groupName = ""

        viewModel.createGroup()

        // Muss in Step 1 bleiben
        assertEquals(1, viewModel.currentStep)
        assertNull(viewModel.lobby.value)
    }
}