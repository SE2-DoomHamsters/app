package com.doomhamsters.ui


import androidx.compose.animation.core.Animatable
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
import com.doomhamsters.model.Card
import com.doomhamsters.model.CardType
import com.doomhamsters.model.Player
import com.doomhamsters.viewmodel.GameBoardViewModel
import kotlinx.coroutines.launch

@Composable
fun GameBoard(
    viewModel: GameBoardViewModel,
    playerAvatars: Map<String, String> = emptyMap(),
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
    var localAnimatingCardIndex by remember { mutableIntStateOf(-1) }
    var opponentAnimatingCardIndex by remember { mutableIntStateOf(-1) }

    val drawAnimatable = remember { Animatable(0f) }
    var isAnimatingLocalDraw by remember { mutableStateOf(false) }
    var isAnimatingOpponentDraw by remember { mutableStateOf(false) }

    LaunchedEffect(viewModel) {
        launch {
            viewModel.error.collect { latestError = it }
        }
        launch {
            viewModel.gameOver.collect(onGameOver)
        }
    }

    val state = gameState
    if (state == null) {
        GameBoardStatus(
            message = latestError ?: "Waiting for game state from server..."
        )
        return
    }

    val currentTurnPlayer = state.players.firstOrNull { it.id == state.currentTurnPlayerId }
        ?: state.players.getOrNull(state.currentPlayerIndex)
    if (currentTurnPlayer == null) {
        GameBoardStatus("Game state is invalid: current player is missing.")
        return
    }

    val localPlayer = state.players.firstOrNull { it.id == viewModel.localPlayerId }
    if (localPlayer == null) {
        GameBoardStatus(
            latestError ?: "Joined game, but player '${viewModel.localPlayerId}' is missing from game state."
        )
        return
    }

    val fallbackOpponent = state.players.firstOrNull { it.id != localPlayer.id }
    val activeOpponent = currentTurnPlayer.takeIf { it.id != localPlayer.id }
    var lastActiveOpponentId by remember(localPlayer.id) { mutableStateOf<String?>(null) }

    LaunchedEffect(activeOpponent?.id, fallbackOpponent?.id, state.players.size) {
        when {
            activeOpponent != null -> lastActiveOpponentId = activeOpponent.id
            lastActiveOpponentId == null -> lastActiveOpponentId = fallbackOpponent?.id
            state.players.none { it.id == lastActiveOpponentId && it.id != localPlayer.id } -> {
                lastActiveOpponentId = fallbackOpponent?.id
            }
        }
    }

    val topOpponent = state.players.firstOrNull { it.id == lastActiveOpponentId && it.id != localPlayer.id }
        ?: fallbackOpponent
    val opponentVisibleHandSize = topOpponent?.visibleHandSize() ?: 0
    val isResolvingLocalDoom =
        pendingDoom != null ||
            state.resolvingDoomPlayerId == viewModel.localPlayerId
    val isLifeLossDoomOverlay =
        pendingDoom != null &&
            !pendingDoomRequiresSelection &&
            !pendingDoomRequiresInsertionUi

    LaunchedEffect(isResolvingLocalDoom) {
        if (isResolvingLocalDoom) {
            selectedPlayerCardIndex = -1
            doomSliderPosition = 0f
        }
    }

    LaunchedEffect(pendingDoomRequiresSelection) {
        if (!pendingDoomRequiresSelection) {
            selectedPlayerCardIndex = -1
        }
    }

    var previousLocalHandSize by remember(localPlayer.id) { mutableIntStateOf(localPlayer.hand.size) }
    var previousOpponentHandSize by remember(topOpponent?.id) { mutableIntStateOf(opponentVisibleHandSize) }
    var hasInitializedHandTracking by remember(localPlayer.id, topOpponent?.id) { mutableStateOf(false) }

    LaunchedEffect(localPlayer.hand.size, opponentVisibleHandSize, topOpponent?.id) {
        if (!hasInitializedHandTracking) {
            previousLocalHandSize = localPlayer.hand.size
            previousOpponentHandSize = opponentVisibleHandSize
            hasInitializedHandTracking = true
            return@LaunchedEffect
        }

        if (localPlayer.hand.size > previousLocalHandSize && !isAnimatingLocalDraw) {
            localAnimatingCardIndex = localPlayer.hand.lastIndex
            isAnimatingLocalDraw = true
            drawAnimatable.snapTo(0f)
            drawAnimatable.animateTo(
                targetValue = 1f,
                animationSpec = tween(800, easing = FastOutSlowInEasing)
            )
            isAnimatingLocalDraw = false
            localAnimatingCardIndex = -1
        }

        if (opponentVisibleHandSize > previousOpponentHandSize && !isAnimatingOpponentDraw) {
            opponentAnimatingCardIndex = opponentVisibleHandSize - 1
            isAnimatingOpponentDraw = true
            drawAnimatable.snapTo(0f)
            drawAnimatable.animateTo(
                targetValue = 1f,
                animationSpec = tween(800, easing = FastOutSlowInEasing)
            )
            isAnimatingOpponentDraw = false
            opponentAnimatingCardIndex = -1
        }

        previousLocalHandSize = localPlayer.hand.size
        previousOpponentHandSize = opponentVisibleHandSize
    }

    val visibleTurns = buildVisibleTurnOrder(
        players = state.players,
        currentTurnPlayerId = state.currentTurnPlayerId,
        currentPlayerIndex = state.currentPlayerIndex
    )
    val localDrawStartOffset = remember(deckCenter, localHandCenter) {
        val deck = deckCenter
        val hand = localHandCenter
        if (deck != null && hand != null) deck - hand else null
    }
    val opponentDrawStartOffset = remember(deckCenter, opponentHandCenter) {
        val deck = deckCenter
        val hand = opponentHandCenter
        if (deck != null && hand != null) hand - deck else null
    }
    val handleDraw = {
        if (isLocalPlayersTurn && !isAnimatingLocalDraw && state.deckSize > 0 && !isResolvingLocalDoom) {
            selectedPlayerCardIndex = -1

            viewModel.draw(localPlayer.id)
        }
    }

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
                    currentTurnPlayerName = currentTurnPlayer.name,
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
                drawAnimation = opponentAnimatingCardIndex.takeIf { it >= 0 }?.let { index ->
                    index to (opponentDrawStartOffset ?: Offset.Zero)
                },
                drawProgress = { drawAnimatable.value },
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

        if (pendingDoom != null && pendingDoomRequiresInsertionUi) {
            DoomOverlay(
                state = DoomOverlayState(
                    pendingDoom = pendingDoom!!,
                    deckSize = state.deckSize,
                    currentPlayer = localPlayer,
                    selectedPlayerCardIndex = selectedPlayerCardIndex,
                    hasDefused = true,
                    doomSliderPosition = doomSliderPosition
                ),
                onDefuseTriggered = {},
                onDoomSliderChange = { doomSliderPosition = it },
                onDoomConfirmed = {
                    viewModel.insertDoom(doomSliderPosition.toInt())
                    doomSliderPosition = 0f
                    selectedPlayerCardIndex = -1
                },
                onAcceptDoom = {}
            )
        } else if (pendingDoom != null && pendingDoomMessage != null) {
            DoomResolvedOverlay(
                card = pendingDoom!!,
                currentPlayer = localPlayer,
                selectedPlayerCardIndex = selectedPlayerCardIndex,
                message = pendingDoomMessage!!,
                requiresSnackSelection = pendingDoomRequiresSelection,
                onDismiss = { viewModel.dismissDoomNotice(selectedPlayerCardIndex) }
            )
        } else if (
            pausedForDoomPlayerName != null &&
            pausedForDoomMessage != null &&
            pausedForDoomDetail != null
        ) {
            RemoteDoomPauseOverlay(
                playerName = pausedForDoomPlayerName!!,
                message = pausedForDoomMessage!!,
                detail = pausedForDoomDetail!!
            )
        }

        if (cardCommandNotice != null) {
            CardCommandNoticeOverlay(
                notice = cardCommandNotice!!,
                onDismiss = viewModel::dismissCardCommandNotice
            )
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .zIndex(if (pendingDoomRequiresSelection) 7f else 0f)
        ) {
            PlayerArea(
                currentPlayer = localPlayer,
                selectedPlayerCardIndex = selectedPlayerCardIndex,
                disableDefocus = pendingDoomRequiresSelection,
                suppressTooltip = isResolvingLocalDoom,
                interactionEnabled = !isLifeLossDoomOverlay,
                localDrawAnimation = localAnimatingCardIndex.takeIf { it >= 0 }?.let { index ->
                    index to (localDrawStartOffset ?: Offset.Zero)
                },
                drawProgress = { drawAnimatable.value },
                canActivateCard = viewModel::canActivateCard,
                onHandCenterMeasured = { localHandCenter = it },
                onCardSelected = {
                    selectedPlayerCardIndex = if (selectedPlayerCardIndex == it) -1 else it
                },
                onCardActivated = { card ->
                    selectedPlayerCardIndex = -1
                    viewModel.activateCard(card)
                }
            )
        }
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
                isOpponent = true,
                selectedIndex = -1,
                drawAnimation = drawAnimation,
                drawProgress = drawProgress,
                onCenterMeasured = onHandCenterMeasured
            )
        }
        PlayerLabel(
            name = opponent.name,
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
    currentPlayer: Player,
    selectedPlayerCardIndex: Int,
    disableDefocus: Boolean,
    suppressTooltip: Boolean,
    interactionEnabled: Boolean,
    localDrawAnimation: Pair<Int, Offset>?,
    drawProgress: () -> Float,
    canActivateCard: (Card) -> Boolean,
    onHandCenterMeasured: (Offset) -> Unit,
    onCardSelected: (Int) -> Unit,
    onCardActivated: (Card) -> Unit
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
                cards = currentPlayer.hand,
                isOpponent = false,
                selectedIndex = selectedPlayerCardIndex,
                disableDefocus = disableDefocus,
                suppressTooltip = suppressTooltip,
                interactionEnabled = interactionEnabled,
                drawAnimation = localDrawAnimation,
                drawProgress = drawProgress,
                canActivateCard = canActivateCard,
                onCenterMeasured = onHandCenterMeasured,
                onCardSelected = onCardSelected,
                onCardActivated = onCardActivated
            )
        }

        PlayerLabel(
            name = currentPlayer.name,
            lives = currentPlayer.lives,
            modifier = Modifier.padding(bottom = 8.dp).zIndex(200f)
        )
    }
}
