package com.doomhamsters.viewmodel



import com.doomhamsters.GameRepository
import com.doomhamsters.model.Card
import com.doomhamsters.model.CardType
import com.doomhamsters.model.GameState
import com.doomhamsters.model.Player
import com.doomhamsters.model.Status
import io.mockk.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.json.JSONObject
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class GameBoardViewModelStateSyncTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var repository: GameRepository
    private lateinit var gameStateFlow: MutableSharedFlow<GameState>

    @BeforeEach
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        repository = mockk(relaxed = true)
        gameStateFlow = MutableSharedFlow()
        coEvery { repository.subscribeToGame("game-1") } returns emptyFlow()
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `keeps local hand when shared sync omits private cards`() = runTest {
        val localPlayerId = "player-1"
        val fullState = gameState(
            currentPlayerId = localPlayerId,
            localHand = listOf(
                Card(CardType.Normal, id = "n1"),
                Card(CardType.SnackStash, id = "s1")
            ),
            localHandSizeHint = 2
        )
        val broadcastState = gameState(
            currentPlayerId = localPlayerId,
            localHand = emptyList(),
            localHandSizeHint = 2
        )

        coEvery { repository.connect() } just Runs
        coEvery { repository.fetchGameState("game-1", localPlayerId) } returns fullState
        coEvery { repository.subscribeToGameState("game-1") } returns gameStateFlow
        coEvery { repository.subscribeToPrivateEvents("game-1", localPlayerId) } returns emptyFlow()

        val viewModel = GameBoardViewModel(
            gameId = "game-1",
            initialLocalPlayerId = localPlayerId,
            localPlayerName = "Local",
            repository = repository
        )
        advanceUntilIdle()

        gameStateFlow.emit(broadcastState)
        advanceUntilIdle()

        val localPlayer = viewModel.gameState.value!!.players.first { it.id == localPlayerId }
        assertEquals(2, localPlayer.hand.size)
        assertEquals(listOf("n1", "s1"), localPlayer.hand.map { it.id })
        assertEquals(true, viewModel.isLocalPlayersTurn.value)
    }

    @Test
    fun `doom draw event is informational because backend resolves doom automatically`() = runTest {
        val localPlayerId = "player-1"
        val initialState = gameState(
            currentPlayerId = localPlayerId,
            localHand = listOf(Card(CardType.SnackStash, id = "s1")),
            localHandSizeHint = 1
        )
        val afterDefuseState = gameState(
            currentPlayerId = localPlayerId,
            localHand = emptyList(),
            localHandSizeHint = 0,
            resolvingDoomPlayerId = localPlayerId,
            pendingDoomRequiresInsertion = true,
            pendingDoomCardId = "doom-1"
        )
        val doomEvent = org.json.JSONObject()
            .put("type", "DOOM_DRAWN")
            .put("card", Card(CardType.Doom, id = "doom-1").toJson())

        coEvery { repository.connect() } just Runs
        coEvery { repository.fetchGameState("game-1", localPlayerId) } returnsMany listOf(initialState, afterDefuseState)
        coEvery { repository.subscribeToGameState("game-1") } returns gameStateFlow
        coEvery { repository.subscribeToPrivateEvents("game-1", localPlayerId) } returns flowOf(doomEvent)
        coEvery { repository.sendAction(any(), any(), any()) } just Runs
        val viewModel = GameBoardViewModel(
            gameId = "game-1",
            initialLocalPlayerId = localPlayerId,
            localPlayerName = "Local",
            repository = repository
        )
        advanceUntilIdle()

        assertEquals(CardType.Doom, viewModel.pendingDoom.value?.type)
        assertEquals("Scroll to your Snack Stash and select it.", viewModel.pendingDoomMessage.value)
        assertEquals(true, viewModel.pendingDoomRequiresSelection.value)
        assertEquals(false, viewModel.pendingDoomRequiresInsertionUi.value)
        assertEquals(1, viewModel.gameState.value!!.players.first { it.id == localPlayerId }.hand.size)
        assertEquals(CardType.SnackStash, viewModel.gameState.value!!.players.first { it.id == localPlayerId }.hand.first().type)
        assertEquals("You drew a Doom card.", viewModel.log.value.last())

        viewModel.dismissDoomNotice(0)
        advanceUntilIdle()

        assertEquals(false, viewModel.pendingDoomRequiresSelection.value)
        assertEquals(true, viewModel.pendingDoomRequiresInsertionUi.value)
        assertEquals(0, viewModel.gameState.value!!.players.first { it.id == localPlayerId }.hand.size)
        coVerify(exactly = 0) { repository.sendAction("game-1", "doom/ack", any()) }
    }

    @Test
    fun `preserves snack stash during doom resolution when shared state arrives before private event`() = runTest {
        val localPlayerId = "player-1"
        val initialState = gameState(
            currentPlayerId = localPlayerId,
            localHand = listOf(Card(CardType.SnackStash, id = "s1")),
            localHandSizeHint = 1
        )
        val resolvingState = gameState(
            currentPlayerId = localPlayerId,
            localHand = emptyList(),
            localHandSizeHint = 0,
            resolvingDoomPlayerId = localPlayerId,
            pendingDoomRequiresInsertion = true,
            pendingDoomCardId = "doom-1"
        )
        val doomEvent = org.json.JSONObject()
            .put("type", "DOOM_DRAWN")
            .put("card", Card(CardType.Doom, id = "doom-1").toJson())

        coEvery { repository.connect() } just Runs
        coEvery { repository.fetchGameState("game-1", localPlayerId) } returns initialState
        coEvery { repository.subscribeToGameState("game-1") } returns gameStateFlow
        coEvery { repository.subscribeToPrivateEvents("game-1", localPlayerId) } returns flowOf(doomEvent)
        coEvery { repository.sendAction(any(), any(), any()) } just Runs
        val viewModel = GameBoardViewModel(
            gameId = "game-1",
            initialLocalPlayerId = localPlayerId,
            localPlayerName = "Local",
            repository = repository
        )
        advanceUntilIdle()

        gameStateFlow.emit(resolvingState)
        advanceUntilIdle()

        val localPlayer = viewModel.gameState.value!!.players.first { it.id == localPlayerId }
        assertEquals(1, localPlayer.hand.size)
        assertEquals(CardType.SnackStash, localPlayer.hand.first().type)
        assertEquals(localPlayerId, viewModel.gameState.value!!.resolvingDoomPlayerId)
    }

    @Test
    fun `doom draw without snack stash logs life loss and does not create pending state`() = runTest {
        val localPlayerId = "player-1"
        val initialState = gameState(
            currentPlayerId = localPlayerId,
            localHand = emptyList(),
            localHandSizeHint = 0,
            localLives = 3
        )
        val afterDoomState = gameState(
            currentPlayerId = localPlayerId,
            localHand = emptyList(),
            localHandSizeHint = 0,
            localLives = 2,
            resolvingDoomPlayerId = localPlayerId
        )

        coEvery { repository.connect() } just Runs
        coEvery { repository.fetchGameState("game-1", localPlayerId) } returnsMany listOf(initialState, afterDoomState)
        coEvery { repository.subscribeToGameState("game-1") } returns gameStateFlow
        coEvery { repository.subscribeToPrivateEvents("game-1", localPlayerId) } returns flowOf(
            org.json.JSONObject()
                .put("type", "DOOM_DRAWN")
                .put("card", Card(CardType.Doom, id = "doom-1").toJson())
        )
        coEvery { repository.sendAction(any(), any(), any()) } just Runs
        val viewModel = GameBoardViewModel(
            gameId = "game-1",
            initialLocalPlayerId = localPlayerId,
            localPlayerName = "Local",
            repository = repository
        )
        advanceUntilIdle()

        viewModel.draw(localPlayerId)
        advanceUntilIdle()

        assertEquals(CardType.Doom, viewModel.pendingDoom.value?.type)
        assertEquals("You lost 1 life.", viewModel.pendingDoomMessage.value)
        assertEquals(false, viewModel.pendingDoomRequiresSelection.value)
        assertEquals(false, viewModel.pendingDoomRequiresInsertionUi.value)
        assertEquals(2, viewModel.gameState.value!!.players.first { it.id == localPlayerId }.lives)
        assertEquals("You drew a Doom card and lost 1 life.", viewModel.log.value.last())
        coVerify(exactly = 0) { repository.sendAction("game-1", "nextTurn", any()) }
        coVerify(exactly = 0) { repository.sendAction("game-1", "doom/ack", any()) }

        viewModel.dismissDoomNotice()
        advanceUntilIdle()

        coVerify(exactly = 1) { repository.sendAction("game-1", "doom/ack", any()) }
        coVerify(exactly = 1) { repository.sendAction("game-1", "nextTurn", any()) }
    }

    @Test
    fun `doom insert sends insert before next turn after snack stash selection`() = runTest {
        val localPlayerId = "player-1"
        val initialState = gameState(
            currentPlayerId = localPlayerId,
            localHand = listOf(Card(CardType.SnackStash, id = "s1")),
            localHandSizeHint = 1
        )
        val resolvingState = gameState(
            currentPlayerId = localPlayerId,
            localHand = emptyList(),
            localHandSizeHint = 0,
            resolvingDoomPlayerId = localPlayerId,
            pendingDoomRequiresInsertion = true,
            pendingDoomCardId = "doom-insert"
        )
        val doomEvent = org.json.JSONObject()
            .put("type", "DOOM_DRAWN")
            .put("card", Card(CardType.Doom, id = "doom-insert").toJson())

        coEvery { repository.connect() } just Runs
        coEvery { repository.fetchGameState("game-1", localPlayerId) } returnsMany listOf(initialState, resolvingState, resolvingState)
        coEvery { repository.subscribeToGameState("game-1") } returns gameStateFlow
        coEvery { repository.subscribeToPrivateEvents("game-1", localPlayerId) } returns flowOf(doomEvent)
        coEvery { repository.sendAction(any(), any(), any()) } just Runs
        val viewModel = GameBoardViewModel(
            gameId = "game-1",
            initialLocalPlayerId = localPlayerId,
            localPlayerName = "Local",
            repository = repository
        )
        advanceUntilIdle()

        viewModel.dismissDoomNotice(0)
        advanceUntilIdle()
        viewModel.insertDoom(3)
        advanceUntilIdle()

        coVerify {
            repository.sendAction(
                "game-1",
                "doom/insert",
                match {
                    it.getString("playerId") == localPlayerId &&
                        it.getInt("position") == 3
                }
            )
        }
        coVerify(exactly = 1) { repository.sendAction("game-1", "nextTurn", any()) }
        coVerify(exactly = 0) { repository.sendAction("game-1", "doom/ack", any()) }
    }

    @Test
    fun `remote doom pause follows resolving doom player from shared state`() = runTest {
        val initialState = gameState(
            currentPlayerId = "player-2",
            localHand = emptyList(),
            localHandSizeHint = 0
        )
        val remoteResolvingState = gameState(
            currentPlayerId = "player-2",
            localHand = emptyList(),
            localHandSizeHint = 0,
            remoteHandSizeHint = 4,
            resolvingDoomPlayerId = "player-2",
            pendingDoomRequiresInsertion = true
        )
        val remoteLifeLossState = gameState(
            currentPlayerId = "player-2",
            localHand = emptyList(),
            localHandSizeHint = 0,
            remoteHandSizeHint = 4,
            resolvingDoomPlayerId = "player-2",
            pendingDoomRequiresInsertion = false
        )

        coEvery { repository.connect() } just Runs
        coEvery { repository.fetchGameState("game-1", "player-1") } returns initialState
        coEvery { repository.subscribeToGameState("game-1") } returns gameStateFlow
        coEvery { repository.subscribeToPrivateEvents("game-1", "player-1") } returns emptyFlow()

        val viewModel = GameBoardViewModel(
            gameId = "game-1",
            initialLocalPlayerId = "player-1",
            localPlayerName = "Local",
            repository = repository
        )
        advanceUntilIdle()

        gameStateFlow.emit(remoteResolvingState)
        advanceUntilIdle()
        assertEquals("Remote", viewModel.pausedForDoomPlayerName.value)
        assertEquals("Remote disabled Doom with Snack Stash.", viewModel.pausedForDoomMessage.value)
        assertEquals(
            "They are choosing where to reinsert the Doom card.",
            viewModel.pausedForDoomDetail.value
        )

        gameStateFlow.emit(remoteResolvingState)
        advanceUntilIdle()
        assertEquals("Remote", viewModel.pausedForDoomPlayerName.value)

        gameStateFlow.emit(remoteLifeLossState)
        advanceUntilIdle()
        assertEquals("Remote was hit by Doom.", viewModel.pausedForDoomMessage.value)
        assertEquals(
            "They are acknowledging the life loss before play continues.",
            viewModel.pausedForDoomDetail.value
        )

        gameStateFlow.emit(
            gameState(
                currentPlayerId = "player-1",
                localHand = emptyList(),
                localHandSizeHint = 0,
                remoteHandSizeHint = 4,
                resolvingDoomPlayerId = null
            )
        )
        advanceUntilIdle()
        assertEquals(null, viewModel.pausedForDoomPlayerName.value)
        assertEquals(null, viewModel.pausedForDoomMessage.value)
        assertEquals(null, viewModel.pausedForDoomDetail.value)
    }

    @Test
    fun `initial remote resolving doom state is shown from authoritative backend state`() = runTest {
        val initialState = gameState(
            currentPlayerId = "player-2",
            localHand = emptyList(),
            localHandSizeHint = 0,
            remoteHandSizeHint = 4,
            resolvingDoomPlayerId = "player-2"
        )

        coEvery { repository.connect() } just Runs
        coEvery { repository.fetchGameState("game-1", "player-1") } returns initialState
        coEvery { repository.subscribeToGameState("game-1") } returns gameStateFlow
        coEvery { repository.subscribeToPrivateEvents("game-1", "player-1") } returns emptyFlow()

        val viewModel = GameBoardViewModel(
            gameId = "game-1",
            initialLocalPlayerId = "player-1",
            localPlayerName = "Local",
            repository = repository
        )
        advanceUntilIdle()

        assertEquals("Remote", viewModel.pausedForDoomPlayerName.value)
        assertEquals("Remote was hit by Doom.", viewModel.pausedForDoomMessage.value)
        assertEquals(
            "They are acknowledging the life loss before play continues.",
            viewModel.pausedForDoomDetail.value
        )
    }

    @Test
    fun `backend json null doom fields do not trigger remote doom pause overlay`() = runTest {
        val initialState = GameState.fromBackendJson(
            JSONObject()
                .put("gameId", "game-1")
                .put("players", org.json.JSONArray().apply {
                    put(
                        JSONObject()
                            .put("id", "player-1")
                            .put("name", "Local")
                            .put("lives", 3)
                            .put("handSizeHint", 0)
                            .put("hand", org.json.JSONArray())
                    )
                    put(
                        JSONObject()
                            .put("id", "player-2")
                            .put("name", "Remote")
                            .put("lives", 3)
                            .put("handSizeHint", 4)
                            .put("hand", org.json.JSONArray())
                    )
                })
                .put("gameState", "PLAYING")
                .put("currentPlayerId", JSONObject.NULL)
                .put("turnCount", 1)
                .put("remainingDeckSize", 30)
                .put("resolvingDoomPlayerId", JSONObject.NULL)
                .put("pendingDoomRequiresInsertion", false)
                .put("pendingDoomCardId", JSONObject.NULL)
        )

        coEvery { repository.connect() } just Runs
        coEvery { repository.fetchGameState("game-1", "player-1") } returns initialState
        coEvery { repository.subscribeToGameState("game-1") } returns gameStateFlow
        coEvery { repository.subscribeToPrivateEvents("game-1", "player-1") } returns emptyFlow()

        val viewModel = GameBoardViewModel(
            gameId = "game-1",
            initialLocalPlayerId = "player-1",
            localPlayerName = "Local",
            repository = repository
        )
        advanceUntilIdle()

        assertEquals(null, viewModel.gameState.value!!.currentTurnPlayerId)
        assertEquals(null, viewModel.gameState.value!!.resolvingDoomPlayerId)
        assertEquals(null, viewModel.pausedForDoomPlayerName.value)
        assertEquals(null, viewModel.pausedForDoomMessage.value)
        assertEquals(null, viewModel.pausedForDoomDetail.value)
    }

    @Test
    fun `backend game state uses exact remaining deck size field`() {
        val state = gameState(
            currentPlayerId = "player-1",
            localHand = List(6) { index ->
                if (index == 0) Card(CardType.SnackStash, id = "s$index") else Card(CardType.Normal, id = "n$index")
            },
            localHandSizeHint = 6,
            remoteHandSizeHint = 6,
            remainingDeckSize = 35
        )

        assertEquals(35, state.deckSize)
    }

    @Test
    fun `activate card sends generic card activation payload`() = runTest {
        val localPlayerId = "player-1"
        val quickPeek = Card(CardType.QuickPeek, id = "qp-1", name = "Quick Peek", effectId = "QUICK_PEEK")
        val initialState = gameState(
            currentPlayerId = localPlayerId,
            localHand = listOf(quickPeek),
            localHandSizeHint = 1
        )

        coEvery { repository.connect() } just Runs
        coEvery { repository.fetchGameState("game-1", localPlayerId) } returnsMany listOf(initialState, initialState)
        coEvery { repository.subscribeToGameState("game-1") } returns gameStateFlow
        coEvery { repository.subscribeToPrivateEvents("game-1", localPlayerId) } returns emptyFlow()
        coEvery { repository.sendAction(any(), any(), any()) } just Runs

        val viewModel = GameBoardViewModel(
            gameId = "game-1",
            initialLocalPlayerId = localPlayerId,
            localPlayerName = "Local",
            repository = repository
        )
        advanceUntilIdle()

        viewModel.activateCard(quickPeek)
        advanceUntilIdle()

        coVerify {
            repository.sendAction(
                "game-1",
                "card/activate",
                match {
                    it.getString("playerId") == localPlayerId &&
                        it.getString("cardId") == "qp-1" &&
                        it.getString("cardType") == CardType.QuickPeek.name &&
                        it.getString("commandId") == "QUICK_PEEK"
                }
            )
        }
    }

    @Test
    fun `quick peek private result displays private notice`() = runTest {
        val localPlayerId = "player-1"
        val quickPeek = Card(CardType.QuickPeek, id = "qp-1", name = "Quick Peek", effectId = "QUICK_PEEK")
        val initialState = gameState(
            currentPlayerId = localPlayerId,
            localHand = listOf(quickPeek),
            localHandSizeHint = 1
        )
        val resultEvent = JSONObject()
            .put("type", "CARD_COMMAND_RESULT")
            .put("commandId", "QUICK_PEEK")
            .put("card", quickPeek.toJson())
            .put("message", "Top card: Doom.")
            .put("revealedCard", Card(CardType.Doom, id = "doom-1", name = "Doom").toJson())

        coEvery { repository.connect() } just Runs
        coEvery { repository.fetchGameState("game-1", localPlayerId) } returns initialState
        coEvery { repository.subscribeToGameState("game-1") } returns gameStateFlow
        coEvery { repository.subscribeToPrivateEvents("game-1", localPlayerId) } returns flowOf(resultEvent)

        val viewModel = GameBoardViewModel(
            gameId = "game-1",
            initialLocalPlayerId = localPlayerId,
            localPlayerName = "Local",
            repository = repository
        )
        advanceUntilIdle()

        assertEquals("Quick Peek", viewModel.cardCommandNotice.value?.title)
        assertEquals("Top card: Doom.", viewModel.cardCommandNotice.value?.message)
        assertEquals(CardType.Doom, viewModel.cardCommandNotice.value?.revealedCard?.type)
    }

    private fun gameState(
        currentPlayerId: String,
        localHand: List<Card>,
        localHandSizeHint: Int,
        localLives: Int = 3,
        remoteHandSizeHint: Int = 4,
        resolvingDoomPlayerId: String? = null,
        pendingDoomRequiresInsertion: Boolean = false,
        pendingDoomCardId: String? = null,
        remainingDeckSize: Int = 30
    ): GameState {
        val localPlayer = Player(
            id = "player-1",
            lives = localLives,
            name = "Local",
            handSizeHint = localHandSizeHint
        ).also { player ->
            player.hand.addAll(localHand)
        }
        val remotePlayer = Player(
            id = "player-2",
            lives = 3,
            name = "Remote",
            handSizeHint = remoteHandSizeHint
        )

        return GameState(
            id = "game-1",
            players = arrayListOf(localPlayer, remotePlayer),
            currentPlayerIndex = 0,
            status = Status.Playing,
            currentPlayerId = currentPlayerId,
            turnCount = 1,
            remainingDeckSize = remainingDeckSize,
            resolvingDoomPlayerId = resolvingDoomPlayerId,
            pendingDoomRequiresInsertion = pendingDoomRequiresInsertion,
            pendingDoomCardId = pendingDoomCardId
        )
    }
}
