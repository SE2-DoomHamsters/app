package com.doomhamsters.ui

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.lifecycle.viewmodel.compose.viewModel
import com.doomhamsters.LobbyViewModel
import com.doomhamsters.ui.lobby.MainLobbyNavigation
import org.junit.Rule
import org.junit.Test

class ActiveLobbyScreenTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

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