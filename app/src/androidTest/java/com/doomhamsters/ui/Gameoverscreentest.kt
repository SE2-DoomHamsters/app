package com.doomhamsters.ui

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.doomhamsters.ui.gameboard.GameOverScreen
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class GameOverScreenTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun test_displaysGameOverText() {
        composeTestRule.setContent {
            GameOverScreen(winnerId = "fat", onRestart = {})
        }
        composeTestRule.onNodeWithText("Game Over!").assertIsDisplayed()
    }

    @Test
    fun test_displaysWinnerId() {
        composeTestRule.setContent {
            GameOverScreen(winnerId = "fat", onRestart = {})
        }
        composeTestRule.onNodeWithText("Winner: fat").assertIsDisplayed()
    }

    @Test
    fun test_displaysWinnerId_differentPlayer() {
        composeTestRule.setContent {
            GameOverScreen(winnerId = "zombie", onRestart = {})
        }
        composeTestRule.onNodeWithText("Winner: zombie").assertIsDisplayed()
    }

    @Test
    fun test_playAgainButton_isDisplayed() {
        composeTestRule.setContent {
            GameOverScreen(winnerId = "fat", onRestart = {})
        }
        composeTestRule.onNodeWithText("Play Again").assertIsDisplayed()
    }

    @Test
    fun test_playAgainButton_invokesOnRestart() {
        var restarted = false
        composeTestRule.setContent {
            GameOverScreen(winnerId = "fat", onRestart = { restarted = true })
        }
        composeTestRule.onNodeWithText("Play Again").performClick()
        assertTrue(restarted)
    }
}