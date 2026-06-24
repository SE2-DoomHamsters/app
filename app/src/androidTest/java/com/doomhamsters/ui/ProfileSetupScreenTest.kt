package com.doomhamsters.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.doomhamsters.LobbyViewModel
import com.doomhamsters.ui.lobby.MainLobbyNavigation
import com.doomhamsters.ui.lobby.ProfileSetupScreen
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ProfileSetupScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun createGroupButton_isDisabledInitially_andBecomesEnabledWhenInputsAreValid() {
        var mockUsername by mutableStateOf("")
        var mockGroupName by mutableStateOf("")

        val mockViewModel = mockk<LobbyViewModel>(relaxed = true) {
            every { username } answers { mockUsername }
            every { groupName } answers { mockGroupName }
            every { isProfileActionInProgress } returns false
            every { error } returns MutableStateFlow(null)
            every { isLoading } returns MutableStateFlow(false)
        }

        composeTestRule.setContent {
            ProfileSetupScreen(viewModel = mockViewModel)
        }

        composeTestRule.onNodeWithText("Gruppe erstellen").assertIsNotEnabled()

        mockUsername = "DoomSlayer"
        mockGroupName = "Die epische Lobby"

        composeTestRule.waitForIdle()

        val createButton = composeTestRule.onNodeWithText("Gruppe erstellen")
        createButton.assertIsEnabled()

        createButton.performClick()
        verify(exactly = 1) { mockViewModel.createGroup() }
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
}