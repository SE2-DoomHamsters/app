package com.doomhamsters.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.doomhamsters.LobbyViewModel
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
}