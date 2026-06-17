/*
package com.doomhamsters.ui

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.lifecycle.viewModelScope
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.doomhamsters.logic.GameEngine
import com.doomhamsters.model.Card
import com.doomhamsters.model.CardType
import com.doomhamsters.model.GameState
import com.doomhamsters.viewmodel.GameBoardViewModel
import kotlinx.coroutines.launch
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class GameBoardTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    private lateinit var viewModel: FakeGameBoardViewModel

    @Before
    fun setup() {
        viewModel = FakeGameBoardViewModel()
    }

    @Test
    fun test_displaysHyphen_whenNoGameState() {
        viewModel.forceCustomState(null)
        composeTestRule.setContent {
            GameBoard(viewModel = viewModel, onGameOver = {})
        }
        composeTestRule.onNodeWithText("Current Player: -").assertIsDisplayed()
        composeTestRule.onNodeWithText("Lives: -").assertIsDisplayed()
    }

    @Test
    fun test_displaysCurrentPlayerIdAndLives() {
        viewModel.setupFakeStateWithPlayers("fat", "zombie")

        val state = viewModel.gameState.value
        val activePlayer = state?.players?.getOrNull(state.currentPlayerIndex ?: 0)
        val expectedLabel = "Current Player: ${activePlayer?.id ?: "fat"}"

        composeTestRule.setContent {
            GameBoard(viewModel = viewModel, onGameOver = {})
        }

        composeTestRule.onNodeWithText(expectedLabel).assertIsDisplayed()
        composeTestRule.onAllNodesWithText("Lives: 3").onFirst().assertIsDisplayed()
    }

    @Test
    fun test_showsDrawButton_whenNoPendingDoom() {
        viewModel.setupFakeStateWithPlayers("fat")
        composeTestRule.setContent {
            GameBoard(viewModel = viewModel, onGameOver = {})
        }
        composeTestRule.onNodeWithText("Draw").assertIsDisplayed()
        composeTestRule.onNodeWithText("Insert Doom").assertDoesNotExist()
    }

    @Test
    fun test_showsDoomUi_whenPendingDoom() {
        viewModel.setupFakeStateWithPlayers("fat")
        viewModel.triggerPendingDoom()

        composeTestRule.setContent {
            GameBoard(viewModel = viewModel, onGameOver = {})
        }

        composeTestRule.onNodeWithText("Insert Doom").assertIsDisplayed()
        composeTestRule.onNodeWithText("Where to insert Doom? (0 = top)").assertIsDisplayed()
        composeTestRule.onNodeWithText("Draw").assertDoesNotExist()
    }

    @Test
    fun test_drawButton_doesNothing_whenCurrentPlayerIsNull() {
        viewModel.forceCustomState(null)
        composeTestRule.setContent {
            GameBoard(viewModel = viewModel, onGameOver = {})
        }
        composeTestRule.onNodeWithText("Draw").performClick()
        assertNull(viewModel.drawCalledWith)
    }

    @Test
    fun test_drawButton_callsDrawAndAdvanceTurn() {
        viewModel.setupFakeStateWithPlayers("fat")

        composeTestRule.setContent {
            GameBoard(viewModel = viewModel, onGameOver = {})
        }
        composeTestRule.onNodeWithText("Draw").performClick()

        assertEquals("fat", viewModel.drawCalledWith)
        assertTrue(viewModel.advanceTurnCalled)
    }

    @Test
    fun test_insertDoom_callsInsertDoomWithPosition() {
        viewModel.setupFakeStateWithPlayers("fat")
        viewModel.triggerPendingDoom()

        composeTestRule.setContent {
            GameBoard(viewModel = viewModel, onGameOver = {})
        }

        composeTestRule.onAllNodes(hasSetTextAction())[0].performTextInput("3")
        composeTestRule.onNodeWithText("Insert Doom").performClick()

        assertEquals(3, viewModel.insertDoomCalledWith)
        assertTrue(viewModel.advanceTurnCalled)
    }

    @Test
    fun test_insertDoom_withEmptyPosition_defaultsToZero() {
        viewModel.setupFakeStateWithPlayers("fat")
        viewModel.triggerPendingDoom()

        composeTestRule.setContent {
            GameBoard(viewModel = viewModel, onGameOver = {})
        }

        composeTestRule.onNodeWithText("Insert Doom").performClick()

        assertEquals(0, viewModel.insertDoomCalledWith)
    }

    @Test
    fun test_insertDoom_clearsUiAndShowsDrawButton() {
        viewModel.setupFakeStateWithPlayers("fat")
        viewModel.triggerPendingDoom()

        composeTestRule.setContent {
            GameBoard(viewModel = viewModel, onGameOver = {})
        }

        composeTestRule.onNodeWithText("Insert Doom").performClick()

        composeTestRule.onNodeWithText("Draw").assertIsDisplayed()
        composeTestRule.onNodeWithText("Insert Doom").assertDoesNotExist()
    }

    @Test
    fun test_logSection_isDisplayed() {
        viewModel.setupFakeStateWithPlayers("fat")
        composeTestRule.setContent {
            GameBoard(viewModel = viewModel, onGameOver = {})
        }
        composeTestRule.onNodeWithText("Log:").assertIsDisplayed()
    }

    @Test
    fun test_logEntries_areDisplayed() {
        viewModel.setupFakeStateWithPlayers("fat")

        viewModel.pushLog("fat drew a card")

        composeTestRule.setContent {
            GameBoard(viewModel = viewModel, onGameOver = {})
        }

        composeTestRule.onNodeWithText("• fat drew a card").assertIsDisplayed()
    }

    @Test
    fun test_logEntries_showOnlyLastFive() {
        viewModel.setupFakeStateWithPlayers("fat")

        repeat(6) { viewModel.pushLog("fat drew a card") }

        composeTestRule.setContent {
            GameBoard(viewModel = viewModel, onGameOver = {})
        }


        composeTestRule.onAllNodesWithText("• fat drew a card").assertCountEquals(5)
    }

    @Test
    fun test_playerList_isRendered() {
        viewModel.setupFakeStateWithPlayers("fat", "zombie")

        val state = viewModel.gameState.value
        val currentIndex = state?.currentPlayerIndex ?: 0
        val p1 = state?.players?.getOrNull(0)?.id ?: "fat"
        val p2 = state?.players?.getOrNull(1)?.id ?: "zombie"

        val p1Label = if (currentIndex == 0) "▶ $p1" else p1
        val p2Label = if (currentIndex == 1) "▶ $p2" else p2

        composeTestRule.setContent {
            GameBoard(viewModel = viewModel, onGameOver = {})
        }

        composeTestRule.onNodeWithText(p1Label).assertIsDisplayed()
        composeTestRule.onNodeWithText(p2Label).assertIsDisplayed()
    }

    @Test
    fun test_gameOver_triggersOnGameOverCallback() {
        var receivedWinnerId: String? = null
        composeTestRule.setContent {
            GameBoard(viewModel = viewModel, onGameOver = { receivedWinnerId = it })
        }

        viewModel.manuallyTriggerGameOver("zombie")

        composeTestRule.waitUntil(timeoutMillis = 2000) { receivedWinnerId != null }
        assertEquals("zombie", receivedWinnerId)
    }

    @Test
    fun test_recomposition_coversCompilerBranches() {

        val initialViewModel = FakeGameBoardViewModel()

        val activeViewModel = androidx.compose.runtime.mutableStateOf<GameBoardViewModel>(initialViewModel)
        val activeCallback = androidx.compose.runtime.mutableStateOf<(String) -> Unit>({})

        composeTestRule.setContent {
            GameBoard(
                viewModel = activeViewModel.value,
                onGameOver = activeCallback.value
            )
        }

        composeTestRule.waitForIdle()


        initialViewModel.setupFakeStateWithPlayers("fat", "zombie")
        composeTestRule.waitForIdle()


        val newViewModel = FakeGameBoardViewModel().apply { setupFakeStateWithPlayers("alien") }
        activeViewModel.value = newViewModel
        activeCallback.value = { _ -> }

        composeTestRule.waitForIdle()
    }

    @Test
    fun test_insertDoom_withInvalidCharacters_defaultsToZero() {
        viewModel.setupFakeStateWithPlayers("fat")
        viewModel.triggerPendingDoom()

        composeTestRule.setContent {
            GameBoard(viewModel = viewModel, onGameOver = {})
        }


        composeTestRule.onAllNodes(hasSetTextAction())[0].performTextInput("invalid_text")
        composeTestRule.onNodeWithText("Insert Doom").performClick()


        assertEquals(0, viewModel.insertDoomCalledWith)
    }

    @Test
    fun test_composeCompilerBranches_byChangingParameters() {

        val vm1 = FakeGameBoardViewModel().apply { forceCustomState(null) }
        val vm2 = FakeGameBoardViewModel().apply { setupFakeStateWithPlayers("fat") }

        val activeViewModel = androidx.compose.runtime.mutableStateOf<GameBoardViewModel>(vm1)
        val activeCallback = androidx.compose.runtime.mutableStateOf<(String)->Unit>({ })

        composeTestRule.setContent {
            GameBoard(
                viewModel = activeViewModel.value,
                onGameOver = activeCallback.value
            )
        }

        composeTestRule.waitForIdle()


        activeViewModel.value = vm2
        activeCallback.value = { _ -> println("Game Over Triggered") }

        composeTestRule.waitForIdle()
    }




}

//class FakeGameBoardViewModel : GameBoardViewModel(gameId = "fake", playerId = "fake") {
   // var drawCalledWith: String? = null
   // var insertDoomCalledWith: Int? = null
  //  var advanceTurnCalled = false

   // fun setupFakeStateWithPlayers(vararg ids: String) {
     //   val generator = GameEngine(ArrayList(ids.toList()))
       // _gameState.value = generator.getState()
    //}

    //fun forceCustomState(state: GameState?) {
      //  _gameState.value = state
   // }

   // fun triggerPendingDoom() {
      //  _pendingDoom.value = Card(CardType.Doom)
   // }

   // fun pushLog(message: String) {
     //   _log.value = _log.value + message
    //}

   // fun manuallyTriggerGameOver(winnerId: String) {
     //   viewModelScope.launch { _gameOver.emit(winnerId) }
   // }

   // override fun draw(playerId: String) {
      //  drawCalledWith = playerId
   // }

   // override fun insertDoom(position: Int) {
      //  insertDoomCalledWith = position
       // _pendingDoom.value = null
   // }

   // override fun advanceTurn() {
     //   advanceTurnCalled = true
   // }

   // override fun startGame(playerIds: ArrayList<String>) {}
//}

 */