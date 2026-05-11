package com.doomhamsters.ui.gameboard

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.doomhamsters.model.Card
import com.doomhamsters.model.CardType
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.sin

private const val piF = PI.toFloat()

fun getCardDescription(type: CardType): String {
    return when (type) {
        CardType.Doom -> "Lose 1 life.\nDiscard Snack Stash to defuse."
        CardType.SnackStash -> "Defuses one Doom card.\nKeep it safe!"
        CardType.Normal -> "Just a regular card.\nNothing to see here."
    }
}

private data class FannedHandState(
    val isOpponent: Boolean,
    val selectedIndex: Int,
    val targetCenterIndex: Int,
    val smoothCenter: Float,
    val animatingCardIndex: Int
)

@Composable
fun FannedHand(
    cards: List<Card>,
    isOpponent: Boolean,
    selectedIndex: Int,
    animatingCardIndex: Int = -1,
    drawProgress: () -> Float = { 1f },
    onCardSelected: (Int) -> Unit = {}
) {
    if (cards.isEmpty()) return

    var targetCenterIndex by remember(cards.size) { mutableIntStateOf((cards.size - 1) / 2) }

    LaunchedEffect(selectedIndex) {
        if (selectedIndex in cards.indices && !isOpponent) {
            targetCenterIndex = selectedIndex
        }
    }

    var dragAccumulator by remember { mutableFloatStateOf(0f) }
    val dragThreshold = 50f

    val draggableState = rememberDraggableState { delta ->
        dragAccumulator += delta
        if (dragAccumulator > dragThreshold) {
            if (targetCenterIndex > 0) targetCenterIndex--
            dragAccumulator = 0f
        } else if (dragAccumulator < -dragThreshold) {
            if (targetCenterIndex < cards.size - 1) targetCenterIndex++
            dragAccumulator = 0f
        }
    }

    val smoothCenterIndexState = animateFloatAsState(
        targetValue = targetCenterIndex.toFloat(),
        animationSpec = tween(250),
        label = "smoothCenter"
    )

    val containerModifier = Modifier
        .fillMaxWidth()
        .height(240.dp)
        .then(
            if (isOpponent) Modifier else Modifier.draggable(
                state = draggableState,
                orientation = Orientation.Horizontal,
                onDragStopped = { dragAccumulator = 0f }
            )
        )

    val handState = FannedHandState(
        isOpponent = isOpponent,
        selectedIndex = selectedIndex,
        targetCenterIndex = targetCenterIndex,
        smoothCenter = smoothCenterIndexState.value,
        animatingCardIndex = animatingCardIndex
    )

    Box(
        contentAlignment = Alignment.Center,
        modifier = containerModifier
    ) {
        cards.forEachIndexed { i, card ->
            FannedCard(
                card = card,
                index = i,
                state = handState,
                drawProgress = drawProgress,
                onCardSelected = {
                    targetCenterIndex = i
                    onCardSelected(i)
                }
            )
        }
    }
}

@Composable
private fun FannedCard(
    card: Card,
    index: Int,
    state: FannedHandState,
    drawProgress: () -> Float,
    onCardSelected: () -> Unit
) {
    val isSelected = index == state.selectedIndex
    val isCenter = index == state.targetCenterIndex
    val isAnimating = index == state.animatingCardIndex
    val isDefocused = !state.isOpponent && !isCenter

    val staticAbsOffset = abs(index - state.targetCenterIndex)

    val selectionLiftState = animateFloatAsState(
        targetValue = if (isSelected) -60f else 0f,
        animationSpec = tween(250),
        label = "lift"
    )

    val currentProgress: () -> Float = {
        if (isAnimating) drawProgress() else 1f
    }

    val zIndexValue = getCardZIndex(isSelected, isAnimating, staticAbsOffset)

    val translationModifier = Modifier
        .zIndex(zIndexValue)
        .graphicsLayer {
            val trans = getCardTranslation(
                index = index,
                smoothCenter = state.smoothCenter,
                isOpponent = state.isOpponent,
                isAnimating = isAnimating,
                selectionLift = selectionLiftState.value,
                progress = currentProgress()
            )
            translationX = trans.x
            translationY = trans.y
        }
        .size(100.dp, 150.dp)

    val origin = if (state.isOpponent) TransformOrigin(0.5f, -0.2f) else TransformOrigin(0.5f, 1.2f)

    val clickableModifier = if (!state.isOpponent && !isAnimating) {
        Modifier.clickable { onCardSelected() }
    } else {
        Modifier
    }

    val cardScaleModifier = Modifier
        .fillMaxSize()
        .graphicsLayer {
            val transform = getCardTransform(
                index = index,
                smoothCenter = state.smoothCenter,
                isOpponent = state.isOpponent,
                isAnimating = isAnimating,
                progress = currentProgress()
            )
            rotationZ = transform.rotationZ
            scaleX = transform.scale
            scaleY = transform.scale
            transformOrigin = origin
            cameraDistance = 12f * density
        }
        .then(clickableModifier)

    val dynamicShadowOffset: () -> Float = {
        getCardShadow(index, state.smoothCenter, isAnimating, currentProgress())
    }

    Box(modifier = translationModifier) {
        Box(modifier = cardScaleModifier) {
            CardContent(
                card = card,
                state = state,
                isSelected = isSelected,
                isDefocused = isDefocused,
                isAnimating = isAnimating,
                drawProgress = drawProgress,
                dynamicShadowOffset = dynamicShadowOffset
            )
        }
        CardTooltip(card, state, isSelected, isAnimating)
    }
}

@Composable
private fun CardContent(
    card: Card,
    state: FannedHandState,
    isSelected: Boolean,
    isDefocused: Boolean,
    isAnimating: Boolean,
    drawProgress: () -> Float,
    dynamicShadowOffset: () -> Float
) {
    if (state.isOpponent) {
        CardFaceDown()
        return
    }
    if (isAnimating) {
        AnimatedCardFaces(card, isSelected, isDefocused, drawProgress, dynamicShadowOffset)
        return
    }
    CardFaceUp(card = card, isSelected = isSelected, isDefocused = isDefocused, shadowOffset = dynamicShadowOffset)
}

@Composable
private fun BoxScope.CardTooltip(
    card: Card,
    state: FannedHandState,
    isSelected: Boolean,
    isAnimating: Boolean
) {
    if (!state.isOpponent && isSelected && !isAnimating) {
        TooltipBubble(
            title = card.type.name,
            description = getCardDescription(card.type),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .offset(y = (-105).dp)
                .requiredWidth(96.dp)
        )
    }
}

private fun getCardZIndex(isSelected: Boolean, isAnimating: Boolean, staticAbsOffset: Int): Float {
    if (isAnimating) return 200f
    if (isSelected) return 100f
    return 50f - staticAbsOffset
}

@Composable
private fun AnimatedCardFaces(
    card: Card,
    isSelected: Boolean,
    isDefocused: Boolean,
    drawProgress: () -> Float,
    dynamicShadowOffset: () -> Float
) {
    CardFaceDown(
        modifier = Modifier.graphicsLayer {
            val rotY = 180f * (1f - ((drawProgress() - 0.3f) / 0.7f).coerceIn(0f, 1f))
            alpha = if (rotY > 90f) 1f else 0f
            rotationY = 180f
        },
        shadowOffset = dynamicShadowOffset
    )
    CardFaceUp(
        card = card,
        isSelected = isSelected,
        isDefocused = isDefocused,
        modifier = Modifier.graphicsLayer {
            val rotY = 180f * (1f - ((drawProgress() - 0.3f) / 0.7f).coerceIn(0f, 1f))
            alpha = if (rotY <= 90f) 1f else 0f
        },
        shadowOffset = dynamicShadowOffset
    )
    Box(
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer {
                val progress = drawProgress()
                alpha = if (progress in 0.5f..0.8f) sin((progress - 0.5f) / 0.3f * piF) else 0f
            }
            .background(Color.White, RoundedCornerShape(8.dp))
    )
}

private data class CardTranslation(val x: Float, val y: Float)
private data class CardTransform(val rotationZ: Float, val scale: Float)

private fun getCardTranslation(
    index: Int,
    smoothCenter: Float,
    isOpponent: Boolean,
    isAnimating: Boolean,
    selectionLift: Float,
    progress: Float
): CardTranslation {
    val offsetFromCenter = index - smoothCenter
    val absOffset = abs(offsetFromCenter)

    val yDrop = if (isOpponent) -(absOffset * absOffset) * 35f else (absOffset * absOffset) * 15f
    val finalY = yDrop + selectionLift
    val finalX = offsetFromCenter * 110f

    if (!isAnimating) return CardTranslation(finalX, finalY)

    val slideFraction = (progress / 0.3f).coerceIn(0f, 1f)
    val flyFraction = ((progress - 0.3f) / 0.7f).coerceIn(0f, 1f)
    val arcY = sin(flyFraction * piF) * -150f

    val transX = (slideFraction * 140f) + (finalX - (slideFraction * 140f)) * flyFraction
    val transY = (-480f + (slideFraction * 20f)) + (finalY - (-480f + (slideFraction * 20f))) * flyFraction + arcY

    return CardTranslation(transX, transY)
}

private fun getCardTransform(
    index: Int,
    smoothCenter: Float,
    isOpponent: Boolean,
    isAnimating: Boolean,
    progress: Float
): CardTransform {
    val offsetFromCenter = index - smoothCenter
    val absOffset = abs(offsetFromCenter)

    val baseRotation = offsetFromCenter * 18f
    val rotation = if (isOpponent) baseRotation + 180f else baseRotation
    val targetScale = 1.3f - (absOffset * 0.25f).coerceAtMost(0.8f)

    if (!isAnimating) return CardTransform(rotation, targetScale)

    val flyFraction = ((progress - 0.3f) / 0.7f).coerceIn(0f, 1f)
    val currentScaleAnim = 1f + (targetScale - 1f) * flyFraction + sin(flyFraction * piF) * 0.5f

    return CardTransform(rotation * flyFraction, currentScaleAnim)
}

private fun getCardShadow(
    index: Int,
    smoothCenter: Float,
    isAnimating: Boolean,
    progress: Float
): Float {
    val absOffset = abs(index - smoothCenter)
    val targetScale = 1.3f - (absOffset * 0.25f).coerceAtMost(0.8f)

    if (!isAnimating) return 4f + (targetScale - 1f) * 10f

    val flyFrac = ((progress - 0.3f) / 0.7f).coerceIn(0f, 1f)
    val currentScaleAnim = 1f + (targetScale - 1f) * flyFrac + sin(flyFrac * piF) * 0.5f
    return 4f + (currentScaleAnim - 1f) * 20f
}