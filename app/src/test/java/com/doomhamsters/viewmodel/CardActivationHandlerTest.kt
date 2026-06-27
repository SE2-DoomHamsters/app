package com.doomhamsters.viewmodel

import com.doomhamsters.GameRepository
import com.doomhamsters.cards.CardCommandId
import com.doomhamsters.cards.CardRegistry
import com.doomhamsters.cards.definitions.CardCommandDefinition
import com.doomhamsters.model.Card
import com.doomhamsters.model.CardType
import com.doomhamsters.model.GameState
import com.doomhamsters.model.Player
import com.doomhamsters.model.Status
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkAll
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class CardActivationHandlerTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var repository: GameRepository
    private lateinit var errorFlow: MutableSharedFlow<String>
    private lateinit var handlerScope: CoroutineScope

    private val localPlayerId = "player-1"
    private val card = Card(CardType.PowerNap, id = "pn-1")

    private var currentGameState: GameState? = null
    private var isPendingDoom = false
    private var isLocalPlayersTurn = true
    private var onStateChangedCalled = false

    @BeforeEach
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        repository = mockk(relaxed = true)
        errorFlow = MutableSharedFlow()
        handlerScope = CoroutineScope(testDispatcher)
        mockkObject(CardRegistry)
        every { CardRegistry.commandFor(any()) } returns CardCommandDefinition(id = CardCommandId.POWER_NAP)
    }

    @AfterEach
    fun tearDown() {
        unmockkAll()
        Dispatchers.resetMain()
    }

    private fun buildHandler() = CardActivationHandler(
        gameId = "game-1",
        repository = repository,
        scope = handlerScope,
        error = errorFlow,
        getLocalPlayerId = { localPlayerId },
        getGameState = { currentGameState },
        isPendingDoom = { isPendingDoom },
        isLocalPlayersTurn = { isLocalPlayersTurn },
        onStateChanged = { onStateChangedCalled = true }
    )

    private fun gameStateWithLocalPlayer(vararg handCards: Card) = GameState(
        id = "game-1",
        players = arrayListOf(
            Player(id = localPlayerId, lives = 3, name = "Local").also { p ->
                p.hand.addAll(handCards.toList())
            }
        ),
        currentPlayerIndex = 0,
        status = Status.Playing,
        currentPlayerId = localPlayerId
    )

    // region canActivate

    @Test
    fun `canActivate returns false when game state is null`() {
        currentGameState = null
        assertFalse(buildHandler().canActivate(card))
    }

    @Test
    fun `canActivate returns false when local player is not in game state`() {
        currentGameState = GameState(
            id = "game-1",
            players = arrayListOf(Player(id = "other", lives = 3, name = "Other")),
            currentPlayerIndex = 0,
            status = Status.Playing
        )
        assertFalse(buildHandler().canActivate(card))
    }

    @Test
    fun `canActivate returns false when pending doom is active`() {
        currentGameState = gameStateWithLocalPlayer(card)
        isPendingDoom = true
        assertFalse(buildHandler().canActivate(card))
    }

    @Test
    fun `canActivate returns false when local player is resolving doom`() {
        currentGameState = GameState(
            id = "game-1",
            players = arrayListOf(
                Player(id = localPlayerId, lives = 3, name = "Local").also { p -> p.hand.add(card) }
            ),
            currentPlayerIndex = 0,
            status = Status.Playing,
            currentPlayerId = localPlayerId,
            resolvingDoomPlayerId = localPlayerId
        )
        assertFalse(buildHandler().canActivate(card))
    }

    @Test
    fun `canActivate returns false when it is not local player turn`() {
        currentGameState = gameStateWithLocalPlayer(card)
        isLocalPlayersTurn = false
        assertFalse(buildHandler().canActivate(card))
    }

    @Test
    fun `canActivate returns false when card is not in player hand`() {
        currentGameState = gameStateWithLocalPlayer()
        assertFalse(buildHandler().canActivate(card))
    }

    @Test
    fun `canActivate returns false when command action path is blank`() {
        currentGameState = gameStateWithLocalPlayer(card)
        every { CardRegistry.commandFor(any()) } returns CardCommandDefinition(
            id = CardCommandId.POWER_NAP,
            actionPath = ""
        )
        assertFalse(buildHandler().canActivate(card))
    }

    @Test
    fun `canActivate returns true when all conditions are satisfied`() {
        currentGameState = gameStateWithLocalPlayer(card)
        assertTrue(buildHandler().canActivate(card))
    }

    // endregion

    // region activate

    @Test
    fun `activate is a no-op when canActivate returns false`() = runTest {
        currentGameState = null
        buildHandler().activate(card)
        advanceUntilIdle()
        coVerify(exactly = 0) { repository.sendAction(any(), any(), any()) }
    }

    @Test
    fun `activate parks card in pendingTargetedCard when command requires target player`() {
        currentGameState = gameStateWithLocalPlayer(card)
        every { CardRegistry.commandFor(any()) } returns CardCommandDefinition(
            id = CardCommandId.POWER_NAP,
            requiresTargetPlayer = true
        )
        val handler = buildHandler()
        handler.activate(card)
        assertEquals(card, handler.pendingTargetedCard.value)
    }

    @Test
    fun `activate sends action to repository when no target selection is required`() = runTest {
        currentGameState = gameStateWithLocalPlayer(card)
        coEvery { repository.sendAction(any(), any(), any()) } just Runs
        buildHandler().activate(card)
        advanceUntilIdle()
        coVerify { repository.sendAction("game-1", "card/activate", any()) }
    }

    // endregion

    // region activateWithTargets

    @Test
    fun `activateWithTargets sends action with target parameters`() = runTest {
        currentGameState = gameStateWithLocalPlayer(card)
        coEvery { repository.sendAction(any(), any(), any()) } just Runs
        val handler = buildHandler()
        handler.activateWithTargets(card, "player-2", "Normal")
        advanceUntilIdle()
        coVerify { repository.sendAction("game-1", "card/activate", any()) }
    }

    @Test
    fun `activateWithTargets clears pendingTargetedCard before sending`() = runTest {
        currentGameState = gameStateWithLocalPlayer(card)
        coEvery { repository.sendAction(any(), any(), any()) } just Runs
        val handler = buildHandler()
        every { CardRegistry.commandFor(any()) } returns CardCommandDefinition(
            id = CardCommandId.POWER_NAP,
            requiresTargetPlayer = true
        )
        handler.activate(card)
        assertEquals(card, handler.pendingTargetedCard.value)

        every { CardRegistry.commandFor(any()) } returns CardCommandDefinition(id = CardCommandId.POWER_NAP)
        handler.activateWithTargets(card, "player-2", "Normal")
        advanceUntilIdle()
        assertNull(handler.pendingTargetedCard.value)
    }

    @Test
    fun `activateWithTargets does nothing when canActivate returns false`() = runTest {
        currentGameState = null
        buildHandler().activateWithTargets(card, "player-2", "Normal")
        advanceUntilIdle()
        coVerify(exactly = 0) { repository.sendAction(any(), any(), any()) }
    }

    // endregion

    // region cancelSelection

    @Test
    fun `cancelSelection clears pendingTargetedCard`() {
        currentGameState = gameStateWithLocalPlayer(card)
        every { CardRegistry.commandFor(any()) } returns CardCommandDefinition(
            id = CardCommandId.POWER_NAP,
            requiresTargetPlayer = true
        )
        val handler = buildHandler()
        handler.activate(card)
        assertEquals(card, handler.pendingTargetedCard.value)

        handler.cancelSelection()
        assertNull(handler.pendingTargetedCard.value)
    }

    // endregion
}
