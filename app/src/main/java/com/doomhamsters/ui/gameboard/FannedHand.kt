package com.doomhamsters.ui.gameboard



import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import com.doomhamsters.cards.CardRegistry
import com.doomhamsters.cards.cardDescription
import com.doomhamsters.cards.displayName
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
import androidx.compose.runtime.MutableFloatState
import androidx.compose.runtime.MutableIntState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.doomhamsters.model.Card
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.sin

private const val piF = PI.toFloat()
private val CardWidth = 100.dp
private val CardHeight = 150.dp
private val TooltipContainerWidth = 112.dp
private val TooltipContainerHeight = 260.dp
private data class DrawAnimationSpec(
    val cardIndex: Int,
    val startOffset: Offset
)

private data class FannedHandState(
    val isOpponent: Boolean,
    val selectedIndex: Int,
    val targetCenterIndex: Int,
    val smoothCenter: Float,
    val drawAnimation: DrawAnimationSpec?
)

data class FannedHandUiConfig(
    val isOpponent: Boolean,
    val selectedIndex: Int,
    val disableDefocus: Boolean = false,
    val suppressTooltip: Boolean = false,
    val interactionEnabled: Boolean = true,
    val modifier: Modifier = Modifier,
    val onCenterMeasured: ((Offset) -> Unit)? = null
)

data class FannedHandAnimationConfig(
    val drawAnimation: Pair<Int, Offset>? = null,
    val drawProgress: () -> Float = { 1f }
)

data class FannedHandCallbacks(
    val canActivateCard: (Card) -> Boolean = { false },
    val onCardSelected: (Int) -> Unit = {},
    val onCardActivated: (Card) -> Unit = {}
)

@Composable
fun FannedHand(
    cards: List<Card>,
    uiConfig: FannedHandUiConfig,
    animationConfig: FannedHandAnimationConfig = FannedHandAnimationConfig(),
    callbacks: FannedHandCallbacks = FannedHandCallbacks()
) {
    if (cards.isEmpty()) return

    val targetCenterIndexState = remember(cards.size) { mutableIntStateOf((cards.size - 1) / 2) }
    val dragAccumulatorState = remember { mutableFloatStateOf(0f) }
    val targetCenterIndex by targetCenterIndexState

    SyncTargetCenterIndex(
        selectedIndex = uiConfig.selectedIndex,
        cardIndices = cards.indices,
        isOpponent = uiConfig.isOpponent,
        targetCenterIndexState = targetCenterIndexState
    )

    val draggableState = rememberTargetCenterDraggableState(
        cardCount = cards.size,
        targetCenterIndexState = targetCenterIndexState,
        dragAccumulatorState = dragAccumulatorState
    )

    val smoothCenterIndexState = animateFloatAsState(
        targetValue = targetCenterIndex.toFloat(),
        animationSpec = tween(250),
        label = "smoothCenter"
    )

    val containerModifier = rememberContainerModifier(
        uiConfig = uiConfig,
        draggableState = draggableState,
        dragAccumulatorState = dragAccumulatorState
    )

    val handState = FannedHandState(
        isOpponent = uiConfig.isOpponent,
        selectedIndex = uiConfig.selectedIndex,
        targetCenterIndex = targetCenterIndex,
        smoothCenter = smoothCenterIndexState.value,
        drawAnimation = animationConfig.drawAnimation?.let { (cardIndex, startOffset) ->
            DrawAnimationSpec(cardIndex = cardIndex, startOffset = startOffset)
        }
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
                uiConfig = uiConfig,
                animationConfig = animationConfig,
                callbacks = withCenteredSelection(callbacks, targetCenterIndexState)
            )
        }
    }
}

@Composable
private fun SyncTargetCenterIndex(
    selectedIndex: Int,
    cardIndices: IntRange,
    isOpponent: Boolean,
    targetCenterIndexState: MutableIntState
) {
    LaunchedEffect(selectedIndex) {
        if (selectedIndex in cardIndices && !isOpponent) {
            targetCenterIndexState.intValue = selectedIndex
        }
    }
}

@Composable
private fun rememberTargetCenterDraggableState(
    cardCount: Int,
    targetCenterIndexState: MutableIntState,
    dragAccumulatorState: MutableFloatState
) = rememberDraggableState { delta ->
    dragAccumulatorState.floatValue += delta
    updateTargetCenterFromDrag(
        cardCount = cardCount,
        targetCenterIndexState = targetCenterIndexState,
        dragAccumulatorState = dragAccumulatorState
    )
}

private fun updateTargetCenterFromDrag(
    cardCount: Int,
    targetCenterIndexState: MutableIntState,
    dragAccumulatorState: MutableFloatState
) {
    val dragThreshold = 50f
    val targetCenterIndex = targetCenterIndexState.intValue
    val dragAccumulator = dragAccumulatorState.floatValue
    when {
        dragAccumulator > dragThreshold && targetCenterIndex > 0 -> {
            targetCenterIndexState.intValue = targetCenterIndex - 1
            dragAccumulatorState.floatValue = 0f
        }

        dragAccumulator < -dragThreshold && targetCenterIndex < cardCount - 1 -> {
            targetCenterIndexState.intValue = targetCenterIndex + 1
            dragAccumulatorState.floatValue = 0f
        }
    }
}

private fun rememberContainerModifier(
    uiConfig: FannedHandUiConfig,
    draggableState: androidx.compose.foundation.gestures.DraggableState,
    dragAccumulatorState: MutableFloatState
): Modifier {
    return Modifier
        .fillMaxWidth()
        .height(240.dp)
        .then(uiConfig.modifier)
        .then(centerMeasurementModifier(uiConfig.onCenterMeasured))
        .then(interactionModifier(uiConfig, draggableState, dragAccumulatorState))
}

private fun centerMeasurementModifier(
    onCenterMeasured: ((Offset) -> Unit)?
): Modifier {
    return onCenterMeasured?.let { callback ->
        Modifier.onGloballyPositioned { coordinates ->
            val position = coordinates.positionInRoot()
            val center = position + Offset(
                x = coordinates.size.width / 2f,
                y = coordinates.size.height / 2f
            )
            callback(center)
        }
    } ?: Modifier
}

private fun interactionModifier(
    uiConfig: FannedHandUiConfig,
    draggableState: androidx.compose.foundation.gestures.DraggableState,
    dragAccumulatorState: MutableFloatState
): Modifier {
    if (uiConfig.isOpponent || !uiConfig.interactionEnabled) return Modifier

    return Modifier.draggable(
        state = draggableState,
        orientation = Orientation.Horizontal,
        onDragStopped = { dragAccumulatorState.floatValue = 0f }
    )
}

private fun withCenteredSelection(
    callbacks: FannedHandCallbacks,
    targetCenterIndexState: MutableIntState
): FannedHandCallbacks {
    return callbacks.copy(
        onCardSelected = { selectedIndex ->
            targetCenterIndexState.intValue = selectedIndex
            callbacks.onCardSelected(selectedIndex)
        }
    )
}

@Composable
private fun FannedCard(
    card: Card,
    index: Int,
    state: FannedHandState,
    uiConfig: FannedHandUiConfig,
    animationConfig: FannedHandAnimationConfig,
    callbacks: FannedHandCallbacks
) {
    val isSelected = index == state.selectedIndex
    val isCenter = index == state.targetCenterIndex
    val isAnimating = index == state.drawAnimation?.cardIndex
    val isDefocused = !uiConfig.disableDefocus && !state.isOpponent && !isCenter

    val staticAbsOffset = abs(index - state.targetCenterIndex)

    val selectionLiftState = animateFloatAsState(
        targetValue = if (isSelected) -60f else 0f,
        animationSpec = tween(250),
        label = "lift"
    )

    val currentProgress = if (isAnimating) animationConfig.drawProgress() else 1f

    val zIndexValue = getCardZIndex(isSelected, isAnimating, staticAbsOffset)

    val translationModifier = Modifier
        .zIndex(zIndexValue)
        .graphicsLayer {
            val trans = getCardTranslation(
                index = index,
                smoothCenter = state.smoothCenter,
                isOpponent = state.isOpponent,
                isAnimating = isAnimating,
                animationStartOffset = state.drawAnimation?.startOffset,
                selectionLift = selectionLiftState.value,
                progress = currentProgress
            )
            translationX = trans.x
            translationY = trans.y
        }
        .size(TooltipContainerWidth, TooltipContainerHeight)

    val origin = if (state.isOpponent) TransformOrigin(0.5f, -0.2f) else TransformOrigin(0.5f, 1.2f)

    val clickableModifier = if (!state.isOpponent && !isAnimating && uiConfig.interactionEnabled) {
        Modifier.clickable { callbacks.onCardSelected(index) }
    } else {
        Modifier
    }

    val cardScaleModifier = Modifier
        .size(CardWidth, CardHeight)
        .graphicsLayer {
            val transform = getCardTransform(
                index = index,
                smoothCenter = state.smoothCenter,
                isOpponent = state.isOpponent,
                isAnimating = isAnimating,
                progress = currentProgress
            )
            rotationZ = transform.rotationZ
            scaleX = transform.scale
            scaleY = transform.scale
            transformOrigin = origin
            cameraDistance = 12f * density
        }
        .then(clickableModifier)

    val dynamicShadowOffset: () -> Float = {
        getCardShadow(index, state.smoothCenter, isAnimating, currentProgress)
    }

    Box(modifier = translationModifier) {
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .then(cardScaleModifier)
        ) {
            CardContent(
                card = card,
                state = state,
                isSelected = isSelected,
                isDefocused = isDefocused,
                isAnimating = isAnimating,
                drawProgress = currentProgress,
                dynamicShadowOffset = dynamicShadowOffset
            )
        }
        CardTooltip(
            card = card,
            state = state,
            isSelected = isSelected,
            isAnimating = isAnimating,
            suppressTooltip = uiConfig.suppressTooltip,
            canActivateCard = callbacks.canActivateCard,
            onCardActivated = callbacks.onCardActivated
        )
    }
}

@Composable
private fun CardContent(
    card: Card,
    state: FannedHandState,
    isSelected: Boolean,
    isDefocused: Boolean,
    isAnimating: Boolean,
    drawProgress: Float,
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
    isAnimating: Boolean,
    suppressTooltip: Boolean,
    canActivateCard: (Card) -> Boolean,
    onCardActivated: (Card) -> Unit
) {
    if (!state.isOpponent && isSelected && !isAnimating && !suppressTooltip) {
        val definition = CardRegistry.definitionFor(card)
        TooltipBubble(
            title = card.displayName(),
            description = card.cardDescription(),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .requiredWidth(TooltipContainerWidth),
            actionLabel = if (definition.command != null) "Activate card" else null,
            actionEnabled = canActivateCard(card),
            onAction = if (definition.command != null) {
                { onCardActivated(card) }
            } else {
                null
            }
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
    drawProgress: Float,
    dynamicShadowOffset: () -> Float
) {
    CardFaceDown(
        modifier = Modifier.graphicsLayer {
            val rotY = 180f * (1f - ((drawProgress - 0.3f) / 0.7f).coerceIn(0f, 1f))
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
            val rotY = 180f * (1f - ((drawProgress - 0.3f) / 0.7f).coerceIn(0f, 1f))
            alpha = if (rotY <= 90f) 1f else 0f
        },
        shadowOffset = dynamicShadowOffset
    )
    Box(
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer {
                alpha = if (drawProgress in 0.5f..0.8f) {
                    sin((drawProgress - 0.5f) / 0.3f * piF)
                } else {
                    0f
                }
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
    animationStartOffset: Offset?,
    selectionLift: Float,
    progress: Float
): CardTranslation {
    val offsetFromCenter = index - smoothCenter
    val absOffset = abs(offsetFromCenter)

    val yDrop = if (isOpponent) -(absOffset * absOffset) * 35f else (absOffset * absOffset) * 15f
    val finalY = yDrop + selectionLift
    val finalX = offsetFromCenter * 110f

    if (!isAnimating) return CardTranslation(finalX, finalY)

    val startOffset = animationStartOffset ?: Offset(140f, -480f)
    val flyFraction = progress.coerceIn(0f, 1f)
    val arcDirection = if (isOpponent) 1f else -1f
    val arcY = sin(flyFraction * piF) * 80f * arcDirection

    val transX = startOffset.x + (finalX - startOffset.x) * flyFraction
    val transY = startOffset.y + (finalY - startOffset.y) * flyFraction + arcY

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
