package com.doomhamsters
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.*
// WICHTIG: Den Import ändern!
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.lifecycle.viewmodel.compose.viewModel
import org.junit.Rule
import org.junit.Test

class LobbyScreenTest {

    @get:Rule
    // Wir nutzen ComponentActivity als Container für den Test
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun createGroup_shows_lobbyId_after_click() {
        // Der Rest bleibt fast gleich, aber die Rule ist jetzt "geerdet"
        composeTestRule.setContent {
            val testViewModel: LobbyViewModel = viewModel()
            MainLobbyNavigation(testViewModel)
        }

        // Falls es immer noch hakt, hilft oft ein kurzes Warten,
        // bis der neue Laptop die Activity geladen hat:
        composeTestRule.waitForIdle()

        // 2. Tippe Namen ein
        composeTestRule.onNodeWithText("Dein Spielername").performTextInput("Hamster1")
        composeTestRule.onNodeWithText("Name der Gruppe / Lobby").performTextInput("TestGruppe")

        // 3. Klicke auf den Button
        composeTestRule.onNodeWithText("Gruppe erstellen").performClick()

        // 4. Prüfe Ergebnis
        composeTestRule.onNodeWithText("Lobby ID: TESTGRUPPE", substring = true).assertIsDisplayed()
    }
}

