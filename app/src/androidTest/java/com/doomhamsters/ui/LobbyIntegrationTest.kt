package com.doomhamsters.ui

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.lifecycle.viewmodel.compose.viewModel
import com.doomhamsters.LobbyViewModel
import com.doomhamsters.ui.lobby.MainLobbyNavigation
import org.junit.Rule
import org.junit.Test

class LobbyIntegrationTest {

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
}