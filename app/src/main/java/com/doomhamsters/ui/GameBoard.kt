package com.doomhamsters.ui

import androidx.compose.animation.core.Animatable
import com.doomhamsters.ui.theme.*
import com.doomhamsters.ui.gameboard.*
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
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
    onGameOver: (String) -> Unit
) {
    val gameState by viewModel.gameState.collectAsState()
    val pendingDoom by viewModel.pendingDoom.collectAsState()

    var selectedPlayerCardIndex by remember { mutableIntStateOf(-1) }
    var doomSliderPosition by remember { mutableFloatStateOf(0f) }
    var hasDefused by remember(pendingDoom) { mutableStateOf(false) }

    val coroutineScope = rememberCoroutineScope()
    val drawAnimatable = remember { Animatable(0f) }
    var isAnimatingDraw by remember { mutableStateOf(false) }

    if (gameState == null) return
    val state = gameState!!
    val currentPlayer = state.players.getOrNull(state.currentPlayerIndex) ?: return
    val opponent = state.players.firstOrNull { it.id != currentPlayer.id }

    val handleDraw = {
        if (!isAnimatingDraw && state.deck.size() > 0 && pendingDoom == null) {
            selectedPlayerCardIndex = -1
            isAnimatingDraw = true

            viewModel.draw(currentPlayer.id)

            coroutineScope.launch {
                drawAnimatable.snapTo(0f)
                drawAnimatable.animateTo(
                    targetValue = 1f,
                    animationSpec = tween(800, easing = FastOutSlowInEasing)
                )

                if (viewModel.pendingDoom.value == null) {
                    viewModel.advanceTurn()
                }
                isAnimatingDraw = false
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(BackgroundCream)) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier.weight(0.8f).fillMaxWidth(),
                contentAlignment = Alignment.TopCenter
            ) {
                OpponentArea(opponent = opponent)
            }

            Box(
                modifier = Modifier.weight(1.7f).fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                CenterPlayArea(deckSize = state.deck.size(), onDrawClick = handleDraw)
            }
            Spacer(modifier = Modifier.weight(1.5f))
        }

        DecorativeSideBorders()

        if (pendingDoom != null) {
            DoomOverlay(
                state = DoomOverlayState(
                    pendingDoom = pendingDoom!!,
                    deckSize = state.deck.size(),
                    currentPlayer = currentPlayer,
                    selectedPlayerCardIndex = selectedPlayerCardIndex,
                    hasDefused = hasDefused,
                    doomSliderPosition = doomSliderPosition
                ),
                onDefuseTriggered = {
                    hasDefused = true
                    selectedPlayerCardIndex = -1
                },
                onDoomSliderChange = { doomSliderPosition = it },
                onDoomConfirmed = {
                    viewModel.insertDoom(doomSliderPosition.toInt().coerceAtMost(state.deck.size()))
                    viewModel.advanceTurn()
                    doomSliderPosition = 0f
                    hasDefused = false
                },
                onAcceptDoom = { }
            )
        }

        PlayerArea(
            currentPlayer = currentPlayer,
            selectedPlayerCardIndex = selectedPlayerCardIndex,
            isAnimatingDraw = isAnimatingDraw,
            drawProgress = { drawAnimatable.value },
            onCardSelected = {
                selectedPlayerCardIndex = if (selectedPlayerCardIndex == it) -1 else it
            }
        )
    }
}

@Composable
private fun OpponentArea(opponent: Player?) {
    if (opponent == null) return
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        val opponentDummyHand = List(opponent.hand.size) { Card(CardType.Normal) }
        FannedHand(cards = opponentDummyHand, isOpponent = true, selectedIndex = -1)
        Spacer(modifier = Modifier.height(16.dp))
        PlayerLabel(name = opponent.id, lives = opponent.lives)
    }
}

@Composable
private fun CenterPlayArea(deckSize: Int, onDrawClick: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        DrawDeck(deckSize = deckSize, onClick = onDrawClick)
        Text(
            text = "TAP DECK TO DRAW",
            color = OutlineDark,
            fontWeight = FontWeight.Bold,
            fontSize = 12.sp,
            modifier = Modifier.padding(top = 16.dp, bottom = 16.dp)
        )
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
    )
    Box(
        modifier = Modifier
            .fillMaxHeight()
            .width(6.dp)
            .background(AccentOrange)
            .align(Alignment.CenterEnd)
            .padding(end = 8.dp)
    )
}

@Composable
private fun PlayerArea(
    currentPlayer: Player,
    selectedPlayerCardIndex: Int,
    isAnimatingDraw: Boolean,
    drawProgress: () -> Float,
    onCardSelected: (Int) -> Unit
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
            modifier = Modifier.fillMaxWidth().offset(y = 75.dp)
        ) {
            FannedHand(
                cards = currentPlayer.hand,
                isOpponent = false,
                selectedIndex = selectedPlayerCardIndex,
                animatingCardIndex = if (isAnimatingDraw) currentPlayer.hand.size - 1 else -1,
                drawProgress = drawProgress,
                onCardSelected = onCardSelected
            )
        }

        PlayerLabel(
            name = currentPlayer.id,
            lives = currentPlayer.lives,
            modifier = Modifier.padding(bottom = 8.dp).zIndex(200f)
        )
    }
}