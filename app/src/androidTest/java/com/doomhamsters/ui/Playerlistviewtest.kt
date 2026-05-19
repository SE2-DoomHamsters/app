package com.doomhamsters.ui

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.doomhamsters.model.Player
import com.doomhamsters.ui.gameboard.PlayerListView
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PlayerListViewTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun test_currentPlayer_hasArrowPrefix() {
        val players = listOf(Player("fat", 3), Player("zombie", 1))
        composeTestRule.setContent {
            PlayerListView(players = players, currentPlayerIndex = 0)
        }
        composeTestRule.onNodeWithText("▶ fat").assertIsDisplayed()
    }

    @Test
    fun test_nonCurrentPlayer_hasNoArrowPrefix() {
        val players = listOf(Player("fat", 3), Player("zombie", 1))
        composeTestRule.setContent {
            PlayerListView(players = players, currentPlayerIndex = 0)
        }
        composeTestRule.onNodeWithText("zombie").assertIsDisplayed()
        composeTestRule.onNodeWithText("▶ zombie").assertDoesNotExist()
    }

    @Test
    fun test_currentPlayerIndex_pointsToSecondPlayer() {
        val players = listOf(Player("fat", 3), Player("zombie", 1))
        composeTestRule.setContent {
            PlayerListView(players = players, currentPlayerIndex = 1)
        }
        composeTestRule.onNodeWithText("fat").assertIsDisplayed()
        composeTestRule.onNodeWithText("▶ zombie").assertIsDisplayed()
        composeTestRule.onNodeWithText("▶ fat").assertDoesNotExist()
    }

    @Test
    fun test_displaysLivesForEachPlayer() {
        val players = listOf(Player("fat", 3), Player("zombie", 1))
        composeTestRule.setContent {
            PlayerListView(players = players, currentPlayerIndex = 0)
        }
        composeTestRule.onNodeWithText("Lives: 3").assertIsDisplayed()
        composeTestRule.onNodeWithText("Lives: 1").assertIsDisplayed()
    }

    @Test
    fun test_displaysCardCountForEachPlayer() {
        val player = Player("fat", 3)
        composeTestRule.setContent {
            PlayerListView(players = listOf(player), currentPlayerIndex = 0)
        }
        composeTestRule.onNodeWithText("Cards: ${player.hand.size}").assertIsDisplayed()
    }

    @Test
    fun test_deadPlayer_isStillDisplayed() {
        val players = listOf(Player("fat", 3), Player("zombie", 0))
        composeTestRule.setContent {
            PlayerListView(players = players, currentPlayerIndex = 0)
        }
        composeTestRule.onNodeWithText("zombie").assertIsDisplayed()
        composeTestRule.onNodeWithText("Lives: 0").assertIsDisplayed()
    }

    @Test
    fun test_emptyPlayerList_rendersWithoutCrash() {
        composeTestRule.setContent {
            PlayerListView(players = emptyList(), currentPlayerIndex = 0)
        }
        composeTestRule.onNodeWithText("▶").assertDoesNotExist()
    }

    @Test
    fun test_singlePlayer_isDisplayedAsCurrentPlayer() {
        val players = listOf(Player("sleepy", 2))
        composeTestRule.setContent {
            PlayerListView(players = players, currentPlayerIndex = 0)
        }
        composeTestRule.onNodeWithText("▶ sleepy").assertIsDisplayed()
        composeTestRule.onNodeWithText("Lives: 2").assertIsDisplayed()
    }
}