package com.doomhamsters
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.lifecycle.viewmodel.compose.viewModel
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
            MainLobbyNavigation(testViewModel)
        }
        composeTestRule.waitForIdle()

        // 2. Tippe Namen ein
        composeTestRule.onNodeWithText("Dein Spielername").performTextInput("Hamster1")
        composeTestRule.onNodeWithText("Name der Gruppe / Lobby").performTextInput("TestGruppe")

        // 3. Klicke auf den Button
        composeTestRule.onNodeWithText("Gruppe erstellen").performClick()

        // 4. Prüfe Ergebnis
        composeTestRule.onNodeWithText("Lobby ID: TESTGRUPPE", substring = true).assertIsDisplayed()
    }
    @Test
    fun button_is_disabled_when_fields_are_empty() {
        composeTestRule.setContent {
            MainLobbyNavigation(viewModel())
        }

        // Button sollte am Anfang deaktiviert sein
        composeTestRule.onNodeWithText("Gruppe erstellen").assertIsNotEnabled()

        // Nur einen Namen eingeben
        composeTestRule.onNodeWithText("Dein Spielername").performTextInput("Hamster1")

        // Immer noch deaktiviert, weil Gruppenname fehlt
        composeTestRule.onNodeWithText("Gruppe erstellen").assertIsNotEnabled()
    }
    @Test
    fun selecting_avatar_updates_ui() {
        composeTestRule.setContent {
            MainLobbyNavigation(viewModel())
        }

        // Klicke auf das Alien-Emoji
        composeTestRule.onNodeWithText("👽").performClick()
    }
    @Test
    fun full_flow_to_gameboard() {
        composeTestRule.setContent {
            MainLobbyNavigation(viewModel())
        }

        // 1. Setup
        composeTestRule.onNodeWithText("Dein Spielername").performTextInput("Hamster1")
        composeTestRule.onNodeWithText("Name der Gruppe / Lobby").performTextInput("Test")
        composeTestRule.onNodeWithText("Gruppe erstellen").performClick()

        // 2. In der Lobby
        composeTestRule.onNodeWithText("SPIEL STARTEN").performClick()

        // 3. Ziel erreicht?
        composeTestRule.onNodeWithText("GAMEBOARD").assertIsDisplayed()
    }
}

