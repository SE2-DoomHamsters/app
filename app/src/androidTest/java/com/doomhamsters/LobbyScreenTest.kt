package com.doomhamsters
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.lifecycle.viewmodel.compose.viewModel
import com.doomhamsters.ui.lobby.MainLobbyNavigation
import org.junit.Rule
import org.junit.Test

class LobbyScreenTest {

    @get:Rule
    // Verwendung von ComponentActivity als Container für den Test
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun createGroup_shows_lobbyId_after_click() {
        composeTestRule.setContent {
            val testViewModel: LobbyViewModel = viewModel()

            testViewModel.currentStep = 2
            testViewModel.username = ""
            testViewModel.groupName = ""
            MainLobbyNavigation(testViewModel)
        }

        // Tippe Namen ein
        composeTestRule.onNodeWithText("Dein Spielername").performTextInput("Hamster1")
        composeTestRule.onNodeWithText("Name der Gruppe / Lobby").performTextInput("TestGruppe")

        // Klicke auf den Button
        composeTestRule.onNodeWithText("Gruppe erstellen").performClick()

        //  Warten, bis das Backend geantwortet hat (max. 5 Sekunden)
        composeTestRule.waitUntil(timeoutMillis = 5000) {
            composeTestRule
                .onAllNodesWithText("Lobby ID", substring = true)
                .fetchSemanticsNodes()
                .isNotEmpty()
        }

        // Prüfe Ergebnis
        composeTestRule.onNodeWithText("Lobby ID", substring = true).assertIsDisplayed()
    }
    @Test
    fun button_is_disabled_when_fields_are_empty() {
            composeTestRule.setContent {
                //  ViewModel...
                val testViewModel: LobbyViewModel = viewModel()

                // Werkszustand!
                testViewModel.currentStep = 2 // Sicherstellen, dass wir im Profil-Screen sind
                testViewModel.username = ""   // Geister-Namen löschen
                testViewModel.groupName = ""  // Geister-Gruppen löschen

                MainLobbyNavigation(testViewModel)
            }

            // Am Anfang MUSS er deaktiviert sein
            composeTestRule.onNodeWithText("Gruppe erstellen").assertIsNotEnabled()

            // Nur einen Namen eingeben
            composeTestRule.onNodeWithText("Dein Spielername").performTextInput("Hamster1")

            // Immer noch deaktiviert, weil Gruppenname zwingend fehlt
            composeTestRule.onNodeWithText("Gruppe erstellen").assertIsNotEnabled()
        }
    @Test
    fun selecting_avatar_updates_ui() {
        composeTestRule.setContent {
            val testViewModel: LobbyViewModel = viewModel()
            // Reset
            testViewModel.currentStep = 2
            testViewModel.username = ""
            testViewModel.groupName = ""
            MainLobbyNavigation(testViewModel)
        }

        // Klicke auf das Alien-Emoji
        composeTestRule.onNodeWithText("👽").performClick()

    }
    @Test
    fun full_flow_to_gameboard() {
        composeTestRule.setContent {
            val testViewModel: LobbyViewModel = viewModel()
            // Reset auf Start
            testViewModel.currentStep = 2
            testViewModel.username = ""
            testViewModel.groupName = ""
            MainLobbyNavigation(testViewModel)
        }

        // 1. Setup & Erstellen
        composeTestRule.onNodeWithText("Dein Spielername").performTextInput("Hamster1")
        composeTestRule.onNodeWithText("Name der Gruppe / Lobby").performTextInput("Test")
        composeTestRule.onNodeWithText("Gruppe erstellen").performClick()

        // Warten auf Lobby-Screen
        composeTestRule.waitUntil(timeoutMillis = 5000) {
            composeTestRule.onAllNodesWithText("Lobby ID", substring = true)
                .fetchSemanticsNodes().isNotEmpty()
        }

        // In der Lobby: Spiel starten
        composeTestRule.onNodeWithText("SPIEL STARTEN").performClick()

        // Ziel erreicht?
        composeTestRule.onNodeWithText("GAMEBOARD").assertIsDisplayed()
    }
    @Test
    fun scanner_button_state_depends_on_username_only() {
        composeTestRule.setContent {
            val testViewModel: LobbyViewModel = viewModel()
            // Step manuell auf 2 gesetzt, damit wir sicher auf dem ProfileSetupScreen landen
            testViewModel.currentStep = 2
            MainLobbyNavigation(testViewModel)
        }

        // Am Anfang ist der Username leer daher muss der Scanner-Button deaktiviert sein
        composeTestRule.onNodeWithText("QR-Code scannen & Beitreten").assertIsNotEnabled()

        // Nur den Spielernamen eingeben (Gruppenname bleibt leer)
        composeTestRule.onNodeWithText("Dein Spielername").performTextInput("Anna")

        // Jetzt mUSS der Scanner-Button aktiviert sein!
        composeTestRule.onNodeWithText("QR-Code scannen & Beitreten").assertIsEnabled()

        // Zur Sicherheit getestet, ob Der "Gruppe erstellen"-Button weiterhin deaktiviert ist
        //, weil dafür der Gruppenname zwingend nötig ist.
        composeTestRule.onNodeWithText("Gruppe erstellen").assertIsNotEnabled()
    }

    @Test
    fun leave_lobby_returns_to_start_screen() {
        composeTestRule.setContent {
            val testViewModel: LobbyViewModel = viewModel()
            testViewModel.username = "Hamster1"
            testViewModel.currentStep = 3
            MainLobbyNavigation(testViewModel)
        }

        // prüft, ob Button da ist
        composeTestRule.onNodeWithText("LOBBY VERLASSEN").assertIsDisplayed()

        // klickt den Button
        composeTestRule.onNodeWithText("LOBBY VERLASSEN").performClick()

        // prüft, ob wir wieder am Startbildschirm sind
        composeTestRule.onNodeWithText("Willkommen bei", substring = true).assertIsDisplayed()
    }
}

