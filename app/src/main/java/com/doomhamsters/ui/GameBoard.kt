package com.doomhamsters.ui


import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import com.doomhamsters.ui.gameboard.*
import com.doomhamsters.ui.theme.*
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.doomhamsters.cards.CardCommandNotice
import com.doomhamsters.model.Card
import com.doomhamsters.model.GameState
import com.doomhamsters.model.CardType
import com.doomhamsters.model.Player
import com.doomhamsters.viewmodel.GameBoardViewModel
import kotlinx.coroutines.launch

private sealed interface GameBoardResolution {
    data class Ready(
        val state: GameState,
        val currentTurnPlayer: Player,
        val localPlayer: Player
    ) : GameBoardResolution

    data class Status(val message: String) : GameBoardResolution
}

private data class BoardOverlayState(
    val pendingDoom: Card?,
    val pendingDoomMessage: String?,
    val pendingDoomRequiresSelection: Boolean,
    val pendingDoomRequiresInsertionUi: Boolean,
    val pausedRemotePlayerName: String?,
    val pausedForDoomPlayerName: String?,
    val pausedForDoomMessage: String?,
    val pausedForDoomDetail: String?,
    val cardCommandNotice: CardCommandNotice?
)

private data class BoardOverlayContext(
    val currentPlayer: Player,
    val deckSize: Int,
    val selectedPlayerCardIndex: Int,
    val doomSliderPosition: Float
)

private data class BoardOverlayCallbacks(
    val onDoomSliderChange: (Float) -> Unit,
    val onDoomConfirmed: () -> Unit,
    val onDoomDismiss: () -> Unit,
    val onCardCommandDismiss: () -> Unit
)

private data class HandDrawState(
    val localAnimatingCardIndex: Int,
    val opponentAnimatingCardIndex: Int,
    val isAnimatingLocalDraw: Boolean,
    val progress: Float
)

private data class PlayerAreaContent(
    val player: Player,
    val playerName: String,
    val handUiConfig: FannedHandUiConfig,
    val handAnimationConfig: FannedHandAnimationConfig,
    val handCallbacks: FannedHandCallbacks
)

private data class LocalPlayerAreaInputs(
    val player: Player,
    val playerName: String,
    val selectedPlayerCardIndex: Int,
    val pendingDoomRequiresSelection: Boolean,
    val isResolvingLocalDoom: Boolean,
    val isLifeLossDoomOverlay: Boolean,
    val localAnimatingCardIndex: Int,
    val localDrawStartOffset: Offset?,
    val drawProgress: Float
)

private data class LocalPlayerAreaCallbacks(
    val onHandCenterMeasured: (Offset) -> Unit,
    val canActivateCard: (Card) -> Boolean,
    val onCardSelectionToggle: (Int) -> Unit,
    val onCardActivated: (Card) -> Unit
)

/** Renders the live game board for the local player. */
@Composable
fun GameBoard(
    viewModel: GameBoardViewModel,
    playerAvatars: Map<String, String> = emptyMap(),
    playerNames: Map<String, String> = emptyMap(),
    onGameOver: (String) -> Unit
) {
    val gameState by viewModel.gameState.collectAsState()
    val isLocalPlayersTurn by viewModel.isLocalPlayersTurn.collectAsState()
    val pendingDoom by viewModel.pendingDoom.collectAsState()
    val pendingDoomMessage by viewModel.pendingDoomMessage.collectAsState()
    val pendingDoomRequiresSelection by viewModel.pendingDoomRequiresSelection.collectAsState()
    val pendingDoomRequiresInsertionUi by viewModel.pendingDoomRequiresInsertionUi.collectAsState()
    val pausedForDoomPlayerName by viewModel.pausedForDoomPlayerName.collectAsState()
    val pausedForDoomMessage by viewModel.pausedForDoomMessage.collectAsState()
    val pausedForDoomDetail by viewModel.pausedForDoomDetail.collectAsState()
    val cardCommandNotice by viewModel.cardCommandNotice.collectAsState()
    var latestError by remember { mutableStateOf<String?>(null) }

    var selectedPlayerCardIndex by remember { mutableIntStateOf(-1) }
    var doomSliderPosition by remember { mutableFloatStateOf(0f) }
    var deckCenter by remember { mutableStateOf<Offset?>(null) }
    var localHandCenter by remember { mutableStateOf<Offset?>(null) }
    var opponentHandCenter by remember { mutableStateOf<Offset?>(null) }

    LaunchedEffect(viewModel) {
        launch {
            viewModel.error.collect { latestError = it }
        }
        launch {
            viewModel.gameOver.collect(onGameOver)
        }
    }

    when (
        val resolution = resolveGameBoardResolution(
            gameState = gameState,
            latestError = latestError,
            localPlayerId = viewModel.localPlayerId
        )
    ) {
        is GameBoardResolution.Status -> {
            GameBoardStatus(resolution.message)
            return
        }

        is GameBoardResolution.Ready -> {
            val state = resolution.state
            val currentTurnPlayer = resolution.currentTurnPlayer
            val localPlayer = resolution.localPlayer

            val topOpponent = rememberTopOpponent(
                players = state.players,
                localPlayerId = localPlayer.id,
                currentTurnPlayerId = currentTurnPlayer.id
            )
            val currentTurnPlayerName = resolvePlayerDisplayName(
                playerId = currentTurnPlayer.id,
                fallbackName = currentTurnPlayer.name,
                playerNames = playerNames
            )
            val localPlayerName = resolvePlayerDisplayName(
                playerId = localPlayer.id,
                fallbackName = localPlayer.name,
                playerNames = playerNames
            )
            val topOpponentName = topOpponent?.let { opponent ->
                resolvePlayerDisplayName(
                    playerId = opponent.id,
                    fallbackName = opponent.name,
                    playerNames = playerNames
                )
            }
            val pausedRemotePlayerName = resolvePausedRemotePlayerName(
                rawName = pausedForDoomPlayerName,
                players = state.players,
                playerNames = playerNames
            )
            val opponentVisibleHandSize = topOpponent?.visibleHandSize() ?: 0
            val isResolvingLocalDoom =
                pendingDoom != null ||
                    state.resolvingDoomPlayerId == viewModel.localPlayerId
            val isLifeLossDoomOverlay =
                pendingDoom != null &&
                    !pendingDoomRequiresSelection &&
                    !pendingDoomRequiresInsertionUi

            ResetDoomInteractionState(
                isResolvingLocalDoom = isResolvingLocalDoom,
                pendingDoomRequiresSelection = pendingDoomRequiresSelection,
                onResetSelection = { selectedPlayerCardIndex = -1 },
                onResetSlider = { doomSliderPosition = 0f }
            )

            val handDrawState = rememberHandDrawState(
                localPlayerId = localPlayer.id,
                localHandSize = localPlayer.hand.size,
                opponentId = topOpponent?.id,
                opponentVisibleHandSize = opponentVisibleHandSize
            )

            val visibleTurns = buildVisibleTurnOrder(
                players = state.players,
                currentTurnPlayerId = state.currentTurnPlayerId,
                currentPlayerIndex = state.currentPlayerIndex
            )
            val localDrawStartOffset = remember(deckCenter, localHandCenter) {
                offsetBetween(deckCenter, localHandCenter)
            }
            val opponentDrawStartOffset = remember(deckCenter, opponentHandCenter) {
                offsetBetween(opponentHandCenter, deckCenter)
            }
            val handleDraw = createDrawHandler(
                isLocalPlayersTurn = isLocalPlayersTurn,
                isAnimatingLocalDraw = handDrawState.isAnimatingLocalDraw,
                deckSize = state.deckSize,
                isResolvingLocalDoom = isResolvingLocalDoom,
                localPlayerId = localPlayer.id,
                onResetSelection = { selectedPlayerCardIndex = -1 },
                onDraw = viewModel::draw
            )
            val overlayState = BoardOverlayState(
                pendingDoom = pendingDoom,
                pendingDoomMessage = pendingDoomMessage,
                pendingDoomRequiresSelection = pendingDoomRequiresSelection,
                pendingDoomRequiresInsertionUi = pendingDoomRequiresInsertionUi,
                pausedRemotePlayerName = pausedRemotePlayerName,
                pausedForDoomPlayerName = pausedForDoomPlayerName,
                pausedForDoomMessage = pausedForDoomMessage,
                pausedForDoomDetail = pausedForDoomDetail,
                cardCommandNotice = cardCommandNotice
            )
            val localPlayerAreaContent = buildLocalPlayerAreaContent(
                inputs = LocalPlayerAreaInputs(
                    player = localPlayer,
                    playerName = localPlayerName,
                    selectedPlayerCardIndex = selectedPlayerCardIndex,
                    pendingDoomRequiresSelection = pendingDoomRequiresSelection,
                    isResolvingLocalDoom = isResolvingLocalDoom,
                    isLifeLossDoomOverlay = isLifeLossDoomOverlay,
                    localAnimatingCardIndex = handDrawState.localAnimatingCardIndex,
                    localDrawStartOffset = localDrawStartOffset,
                    drawProgress = handDrawState.progress
                ),
                callbacks = LocalPlayerAreaCallbacks(
                    onHandCenterMeasured = { localHandCenter = it },
                    canActivateCard = viewModel::canActivateCard,
                    onCardSelectionToggle = { index ->
                        selectedPlayerCardIndex = if (selectedPlayerCardIndex == index) -1 else index
                    },
                    onCardActivated = { card ->
                        selectedPlayerCardIndex = -1
                        viewModel.activateCard(card)
                    }
                )
            )

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(BackgroundCream)
            ) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Spacer(modifier = Modifier.weight(0.8f))

                    Box(
                        modifier = Modifier.weight(1.7f).fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        CenterPlayArea(
                            deckSize = state.deckSize,
                            isLocalPlayersTurn = isLocalPlayersTurn,
                            currentTurnPlayerName = currentTurnPlayerName,
                            visibleTurns = visibleTurns,
                            playerAvatars = playerAvatars,
                            onDrawClick = handleDraw,
                            onDeckCenterMeasured = { deckCenter = it }
                        )
                    }
                    Spacer(modifier = Modifier.weight(1.5f))
                }

                DecorativeSideBorders()

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .zIndex(3f),
                    contentAlignment = Alignment.TopCenter
                ) {
                    OpponentArea(
                        opponent = topOpponent,
                        opponentName = topOpponentName,
                        drawAnimation = handDrawState.opponentAnimatingCardIndex.takeIf { it >= 0 }?.let { index ->
                            index to (opponentDrawStartOffset ?: Offset.Zero)
                        },
                        drawProgress = { handDrawState.progress },
                        onHandCenterMeasured = { opponentHandCenter = it }
                    )
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .windowInsetsTopHeight(WindowInsets.statusBars)
                        .background(BackgroundCream)
                        .zIndex(4f)
                        .align(Alignment.TopCenter)
                )

                BoardOverlays(
                    overlayState = overlayState,
                    context = BoardOverlayContext(
                        currentPlayer = localPlayer,
                        deckSize = state.deckSize,
                        selectedPlayerCardIndex = selectedPlayerCardIndex,
                        doomSliderPosition = doomSliderPosition
                    ),
                    callbacks = BoardOverlayCallbacks(
                        onDoomSliderChange = { doomSliderPosition = it },
                        onDoomConfirmed = {
                            viewModel.insertDoom(doomSliderPosition.toInt())
                            doomSliderPosition = 0f
                            selectedPlayerCardIndex = -1
                        },
                        onDoomDismiss = { viewModel.dismissDoomNotice(selectedPlayerCardIndex) },
                        onCardCommandDismiss = viewModel::dismissCardCommandNotice
                    )
                )

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .zIndex(if (pendingDoomRequiresSelection) 7f else 0f)
                ) {
                    PlayerArea(content = localPlayerAreaContent)
                }
            }
        }
    }
}

private fun resolveGameBoardResolution(
    gameState: GameState?,
    latestError: String?,
    localPlayerId: String
): GameBoardResolution {
    if (gameState == null) {
        return GameBoardResolution.Status(latestError ?: "Waiting for game state from server...")
    }

    val currentTurnPlayer = gameState.players.firstOrNull { it.id == gameState.currentTurnPlayerId }
        ?: gameState.players.getOrNull(gameState.currentPlayerIndex)
        ?: return GameBoardResolution.Status("Game state is invalid: current player is missing.")

    val localPlayer = gameState.players.firstOrNull { it.id == localPlayerId }
        ?: return GameBoardResolution.Status(
            latestError ?: "Joined game, but player '$localPlayerId' is missing from game state."
        )

    return GameBoardResolution.Ready(
        state = gameState,
        currentTurnPlayer = currentTurnPlayer,
        localPlayer = localPlayer
    )
}

private fun offsetBetween(from: Offset?, to: Offset?): Offset? {
    return if (from != null && to != null) from - to else null
}

private fun createDrawHandler(
    isLocalPlayersTurn: Boolean,
    isAnimatingLocalDraw: Boolean,
    deckSize: Int,
    isResolvingLocalDoom: Boolean,
    localPlayerId: String,
    onResetSelection: () -> Unit,
    onDraw: (String) -> Unit
): () -> Unit {
    return {
        if (isLocalPlayersTurn && !isAnimatingLocalDraw && deckSize > 0 && !isResolvingLocalDoom) {
            onResetSelection()
            onDraw(localPlayerId)
        }
    }
}

private fun buildLocalPlayerAreaContent(
    inputs: LocalPlayerAreaInputs,
    callbacks: LocalPlayerAreaCallbacks
): PlayerAreaContent {
    return PlayerAreaContent(
        player = inputs.player,
        playerName = inputs.playerName,
        handUiConfig = FannedHandUiConfig(
            isOpponent = false,
            selectedIndex = inputs.selectedPlayerCardIndex,
            disableDefocus = inputs.pendingDoomRequiresSelection,
            suppressTooltip = inputs.isResolvingLocalDoom,
            interactionEnabled = !inputs.isLifeLossDoomOverlay,
            onCenterMeasured = callbacks.onHandCenterMeasured
        ),
        handAnimationConfig = FannedHandAnimationConfig(
            drawAnimation = inputs.localAnimatingCardIndex.takeIf { it >= 0 }?.let { index ->
                index to (inputs.localDrawStartOffset ?: Offset.Zero)
            },
            drawProgress = { inputs.drawProgress }
        ),
        handCallbacks = FannedHandCallbacks(
            canActivateCard = callbacks.canActivateCard,
            onCardSelected = callbacks.onCardSelectionToggle,
            onCardActivated = callbacks.onCardActivated
        )
    )
}

@Composable
private fun ResetDoomInteractionState(
    isResolvingLocalDoom: Boolean,
    pendingDoomRequiresSelection: Boolean,
    onResetSelection: () -> Unit,
    onResetSlider: () -> Unit
) {
    LaunchedEffect(isResolvingLocalDoom) {
        if (isResolvingLocalDoom) {
            onResetSelection()
            onResetSlider()
        }
    }

    LaunchedEffect(pendingDoomRequiresSelection) {
        if (!pendingDoomRequiresSelection) {
            onResetSelection()
        }
    }
}

@Composable
private fun rememberTopOpponent(
    players: List<Player>,
    localPlayerId: String,
    currentTurnPlayerId: String
): Player? {
    val fallbackOpponent = players.firstOrNull { it.id != localPlayerId }
    val activeOpponent = players.firstOrNull { it.id == currentTurnPlayerId && it.id != localPlayerId }
    var lastActiveOpponentId by remember(localPlayerId) { mutableStateOf<String?>(null) }

    LaunchedEffect(activeOpponent?.id, fallbackOpponent?.id, players.size) {
        lastActiveOpponentId = when {
            activeOpponent != null -> activeOpponent.id
            lastActiveOpponentId == null -> fallbackOpponent?.id
            players.none { it.id == lastActiveOpponentId && it.id != localPlayerId } -> fallbackOpponent?.id
            else -> lastActiveOpponentId
        }
    }

    return players.firstOrNull { it.id == lastActiveOpponentId && it.id != localPlayerId }
        ?: fallbackOpponent
}

private fun resolvePausedRemotePlayerName(
    rawName: String?,
    players: List<Player>,
    playerNames: Map<String, String>
): String? {
    if (rawName == null) return null

    val matchedPlayer = players.firstOrNull { player ->
        val displayName = resolvePlayerDisplayName(
            playerId = player.id,
            fallbackName = player.name,
            playerNames = playerNames
        )
        displayName == rawName || player.name == rawName || player.id == rawName
    }

    return matchedPlayer?.let { player ->
        resolvePlayerDisplayName(
            playerId = player.id,
            fallbackName = player.name,
            playerNames = playerNames
        )
    } ?: rawName
}

@Composable
private fun rememberHandDrawState(
    localPlayerId: String,
    localHandSize: Int,
    opponentId: String?,
    opponentVisibleHandSize: Int
): HandDrawState {
    val drawAnimatable = remember { Animatable(0f) }
    var localAnimatingCardIndex by remember(localPlayerId) { mutableIntStateOf(-1) }
    var opponentAnimatingCardIndex by remember(opponentId) { mutableIntStateOf(-1) }
    var isAnimatingLocalDraw by remember(localPlayerId) { mutableStateOf(false) }
    var previousLocalHandSize by remember(localPlayerId) { mutableIntStateOf(localHandSize) }
    var previousOpponentHandSize by remember(opponentId) { mutableIntStateOf(opponentVisibleHandSize) }
    var hasInitializedHandTracking by remember(localPlayerId, opponentId) { mutableStateOf(false) }

    LaunchedEffect(localHandSize, opponentVisibleHandSize, opponentId) {
        if (!hasInitializedHandTracking) {
            previousLocalHandSize = localHandSize
            previousOpponentHandSize = opponentVisibleHandSize
            hasInitializedHandTracking = true
            return@LaunchedEffect
        }

        if (localHandSize > previousLocalHandSize && !isAnimatingLocalDraw) {
            localAnimatingCardIndex = localHandSize - 1
            isAnimatingLocalDraw = true
            runDrawAnimation(drawAnimatable)
            isAnimatingLocalDraw = false
            localAnimatingCardIndex = -1
        }

        if (opponentVisibleHandSize > previousOpponentHandSize) {
            opponentAnimatingCardIndex = opponentVisibleHandSize - 1
            runDrawAnimation(drawAnimatable)
            opponentAnimatingCardIndex = -1
        }

        previousLocalHandSize = localHandSize
        previousOpponentHandSize = opponentVisibleHandSize
    }

    return HandDrawState(
        localAnimatingCardIndex = localAnimatingCardIndex,
        opponentAnimatingCardIndex = opponentAnimatingCardIndex,
        isAnimatingLocalDraw = isAnimatingLocalDraw,
        progress = drawAnimatable.value
    )
}

private suspend fun runDrawAnimation(drawAnimatable: Animatable<Float, AnimationVector1D>) {
    drawAnimatable.snapTo(0f)
    drawAnimatable.animateTo(
        targetValue = 1f,
        animationSpec = tween(800, easing = FastOutSlowInEasing)
    )
}

@Composable
private fun BoardOverlays(
    overlayState: BoardOverlayState,
    context: BoardOverlayContext,
    callbacks: BoardOverlayCallbacks
) {
    when {
        overlayState.pendingDoom != null && overlayState.pendingDoomRequiresInsertionUi -> {
            DoomOverlay(
                state = DoomOverlayState(
                    pendingDoom = overlayState.pendingDoom,
                    deckSize = context.deckSize,
                    currentPlayer = context.currentPlayer,
                    selectedPlayerCardIndex = context.selectedPlayerCardIndex,
                    hasDefused = true,
                    doomSliderPosition = context.doomSliderPosition
                ),
                onDefuseTriggered = {},
                onDoomSliderChange = callbacks.onDoomSliderChange,
                onDoomConfirmed = callbacks.onDoomConfirmed,
                onAcceptDoom = {}
            )
        }

        overlayState.pendingDoom != null && overlayState.pendingDoomMessage != null -> {
            DoomResolvedOverlay(
                card = overlayState.pendingDoom,
                currentPlayer = context.currentPlayer,
                selectedPlayerCardIndex = context.selectedPlayerCardIndex,
                message = overlayState.pendingDoomMessage,
                requiresSnackSelection = overlayState.pendingDoomRequiresSelection,
                onDismiss = callbacks.onDoomDismiss
            )
        }

        overlayState.pausedForDoomPlayerName != null &&
            overlayState.pausedForDoomMessage != null &&
            overlayState.pausedForDoomDetail != null -> {
            RemoteDoomPauseOverlay(
                playerName = overlayState.pausedRemotePlayerName ?: overlayState.pausedForDoomPlayerName,
                message = overlayState.pausedForDoomMessage,
                detail = overlayState.pausedForDoomDetail
            )
        }
    }

    overlayState.cardCommandNotice?.let { notice ->
        CardCommandNoticeOverlay(
            notice = notice,
            onDismiss = callbacks.onCardCommandDismiss
        )
    }
}

@Composable
private fun GameBoardStatus(message: String) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundCream)
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            CircularProgressIndicator(color = AccentOrange)
            Text(
                text = message,
                style = MaterialTheme.typography.bodyLarge,
                color = OutlineDark
            )
        }
    }
}

@Composable
private fun OpponentArea(
    opponent: Player?,
    opponentName: String?,
    drawAnimation: Pair<Int, Offset>?,
    drawProgress: () -> Float,
    onHandCenterMeasured: (Offset) -> Unit
) {
    if (opponent == null) return
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(220.dp)
    ) {
        val opponentDummyHand = List(opponent.visibleHandSize()) { Card(CardType.Normal) }
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .offset(y = -300.dp)
                .graphicsLayer {
                    rotationZ = 180f
                }
        ) {
            FannedHand(
                cards = opponentDummyHand,
                uiConfig = FannedHandUiConfig(
                    isOpponent = true,
                    selectedIndex = -1,
                    onCenterMeasured = onHandCenterMeasured
                ),
                animationConfig = FannedHandAnimationConfig(
                    drawAnimation = drawAnimation,
                    drawProgress = drawProgress
                )
            )
        }
        PlayerLabel(
            name = opponentName ?: opponent.name,
            lives = opponent.lives,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 40.dp)
                .zIndex(1f)
        )
    }
}

@Composable
private fun CenterPlayArea(
    deckSize: Int,
    isLocalPlayersTurn: Boolean,
    currentTurnPlayerName: String,
    visibleTurns: List<Player>,
    playerAvatars: Map<String, String>,
    onDrawClick: () -> Unit,
    onDeckCenterMeasured: (Offset) -> Unit
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            DrawDeck(
                deckSize = deckSize,
                onClick = onDrawClick,
                modifier = Modifier.onGloballyPositioned { coordinates ->
                    val position = coordinates.positionInRoot()
                    val center = position + Offset(
                        x = coordinates.size.width / 2f,
                        y = coordinates.size.height / 2f
                    )
                    onDeckCenterMeasured(center)
                }
            )
            TurnTracker(
                visibleTurns = visibleTurns,
                playerAvatars = playerAvatars
            )
        }
        Text(
            text = if (isLocalPlayersTurn) {
                "YOUR TURN - TAP DECK TO DRAW"
            } else {
                "WAITING FOR $currentTurnPlayerName"
            },
            color = OutlineDark,
            fontWeight = FontWeight.Bold,
            fontSize = 12.sp,
            modifier = Modifier.padding(top = 16.dp, bottom = 16.dp)
        )
    }
}

private fun buildVisibleTurnOrder(
    players: List<Player>,
    currentTurnPlayerId: String?,
    currentPlayerIndex: Int,
    futureTurnCount: Int = 6
): List<Player> {
    if (players.isEmpty()) return emptyList()

    val currentIndex = players.indexOfFirst { it.id == currentTurnPlayerId }
        .takeIf { it >= 0 }
        ?: currentPlayerIndex.coerceIn(0, players.lastIndex)

    val alivePlayersInOrder = buildList {
        repeat(players.size) { offset ->
            val player = players[(currentIndex + offset) % players.size]
            if (player.isAlive()) add(player)
        }
    }

    if (alivePlayersInOrder.size <= 1) return alivePlayersInOrder

    return List(futureTurnCount) { index ->
        alivePlayersInOrder[index % alivePlayersInOrder.size]
    }
}

@Composable
private fun BoxScope.DecorativeSideBorders() {
    Box(
        modifier = Modifier
            .fillMaxHeight()
            .width(6.dp)
            .background(AccentOrange)
            .align(Alignment.CenterStart)
            .padding(start = 8.dp)
            .zIndex(5f)
    )
    Box(
        modifier = Modifier
            .fillMaxHeight()
            .width(6.dp)
            .background(AccentOrange)
            .align(Alignment.CenterEnd)
            .padding(end = 8.dp)
            .zIndex(5f)
    )
}

@Composable
private fun PlayerArea(
    content: PlayerAreaContent
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.BottomCenter
    ) {
        Canvas(modifier = Modifier.fillMaxWidth().height(100.dp).offset(y = 50.dp)) {
            drawOval(
                color = CardDarkMaroon,
                topLeft = Offset(-size.width * 0.5f, 0f),
                size = Size(size.width * 2f, size.height * 2f)
            )
        }
    }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.BottomCenter
    ) {
        Box(
            contentAlignment = Alignment.BottomCenter,
            modifier = Modifier.fillMaxWidth().offset(y = 58.dp)
        ) {
            FannedHand(
                cards = content.player.hand,
                uiConfig = content.handUiConfig,
                animationConfig = content.handAnimationConfig,
                callbacks = content.handCallbacks
            )
        }

        PlayerLabel(
            name = content.playerName,
            lives = content.player.lives,
            modifier = Modifier.padding(bottom = 8.dp).zIndex(200f)
        )
    }
}
