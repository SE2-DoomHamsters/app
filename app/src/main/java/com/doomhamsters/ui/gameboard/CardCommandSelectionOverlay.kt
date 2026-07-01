package com.doomhamsters.ui.gameboard

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.doomhamsters.cards.presentation.CardCommandSelectionOption
import com.doomhamsters.cards.presentation.CardCommandSelectionPresentation
import com.doomhamsters.cards.presentation.CardCommandTypeOption
import com.doomhamsters.model.Card
import com.doomhamsters.model.GameState
import com.doomhamsters.ui.theme.AccentOrange
import com.doomhamsters.ui.theme.BackgroundCream
import com.doomhamsters.ui.theme.CardDarkMaroon
import com.doomhamsters.ui.theme.OutlineDark

@Composable
fun CardCommandSelectionOverlay(
    card: Card?,
    gameState: GameState,
    localPlayerId: String,
    onConfirm: (targetPlayerId: String?, requestedCardType: String?, hamsterType: String?) -> Unit,
    onDismiss: () -> Unit
) {
    if (card == null) return

    val selectionState = rememberCardCommandSelectionState(card, gameState, localPlayerId)

    val display = CardCommandSelectionPresentation.display(
        card = card,
        gameState = gameState,
        localPlayerId = localPlayerId,
        selectedTargetPlayerId = selectionState.selectedTargetPlayerId,
        selectedCardType = selectionState.selectedCardType,
        hamsterType = selectionState.hamsterType
    ) ?: return

    OverlayBackdrop(onDismiss = onDismiss)
    OverlayCard(
        display = display,
        selectionState = selectionState,
        onDismiss = onDismiss,
        onConfirm = onConfirm
    )
}

private class CardCommandSelectionState(
    selectedTargetPlayerId: String?,
    selectedCardType: String?,
    hamsterType: String
) {
    var selectedTargetPlayerId by mutableStateOf(selectedTargetPlayerId)
    var selectedCardType by mutableStateOf(selectedCardType)
    var hamsterType by mutableStateOf(hamsterType)
}

@Composable
private fun rememberCardCommandSelectionState(
    card: Card,
    gameState: GameState,
    localPlayerId: String
): CardCommandSelectionState {
    val initialCardType = remember(card.id) {
        CardCommandSelectionPresentation.initialRequestedCardType(card)
    }
    return remember(card.id, gameState.players.size, localPlayerId, initialCardType) {
        CardCommandSelectionState(
            selectedTargetPlayerId = null,
            selectedCardType = initialCardType,
            hamsterType = ""
        )
    }
}

@Composable
private fun OverlayBackdrop(onDismiss: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .zIndex(13f)
            .background(Color.Black.copy(alpha = 0.72f))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onDismiss
            )
    )
}

@Composable
private fun OverlayCard(
    display: com.doomhamsters.cards.presentation.CardCommandSelectionDisplay,
    selectionState: CardCommandSelectionState,
    onDismiss: () -> Unit,
    onConfirm: (targetPlayerId: String?, requestedCardType: String?, hamsterType: String?) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .zIndex(14f)
            .padding(horizontal = 18.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 390.dp)
                .heightIn(max = 468.dp)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = {}
                ),
            shape = RoundedCornerShape(24.dp),
            color = CardDarkMaroon.copy(alpha = 0.98f),
            shadowElevation = 12.dp,
            border = BorderStroke(2.dp, AccentOrange.copy(alpha = 0.72f))
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                CommandHeader(
                    title = display.title,
                    description = display.description
                )

                Spacer(modifier = Modifier.height(8.dp))

                SelectionContent(
                    display = display,
                    selectionState = selectionState
                )

                Spacer(modifier = Modifier.height(10.dp))

                CommandActions(
                    canConfirm = display.canConfirm,
                    onDismiss = onDismiss,
                    onConfirm = {
                        onConfirm(
                            selectionState.selectedTargetPlayerId,
                            selectionState.selectedCardType.takeIf { display.requiresRequestedCardType },
                            selectionState.hamsterType.takeIf { display.requiresHamsterType }
                        )
                    }
                )
            }
        }
    }
}

@Composable
private fun SelectionContent(
    display: com.doomhamsters.cards.presentation.CardCommandSelectionDisplay,
    selectionState: CardCommandSelectionState
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        TargetPlayerSection(display, selectionState)
        RequestedCardSection(display, selectionState)
        HamsterTypeSection(display, selectionState)
    }
}

@Composable
private fun TargetPlayerSection(
    display: com.doomhamsters.cards.presentation.CardCommandSelectionDisplay,
    selectionState: CardCommandSelectionState
) {
    if (!display.requiresTargetPlayer) return

    SelectionSection(title = "Target player") {
        if (display.targetOptions.isEmpty()) {
            EmptySelectionMessage("No available target players.")
        } else {
            SelectionGrid(
                options = display.targetOptions,
                selectedValue = selectionState.selectedTargetPlayerId,
                onSelect = { selectionState.selectedTargetPlayerId = it }
            )
        }
    }
}

@Composable
private fun RequestedCardSection(
    display: com.doomhamsters.cards.presentation.CardCommandSelectionDisplay,
    selectionState: CardCommandSelectionState
) {
    if (!display.requiresRequestedCardType) return

    SelectionSection(title = "Requested card") {
        CardTypeWheel(
            options = display.cardTypeOptions,
            selectedValue = selectionState.selectedCardType,
            onSelect = { selectionState.selectedCardType = it }
        )
    }
}

@Composable
private fun HamsterTypeSection(
    display: com.doomhamsters.cards.presentation.CardCommandSelectionDisplay,
    selectionState: CardCommandSelectionState
) {
    if (!display.requiresHamsterType) return

    SelectionSection(title = "Hamster type") {
        HamsterTypeField(
            value = selectionState.hamsterType,
            onValueChange = { selectionState.hamsterType = it }
        )
    }
}

@Composable
private fun CommandHeader(
    title: String,
    description: String
) {
    Text(
        text = "PLAY CARD",
        color = AccentOrange,
        fontWeight = FontWeight.Black,
        fontSize = 10.sp,
        letterSpacing = 1.4.sp
    )
    Text(
        text = title,
        color = BackgroundCream,
        fontWeight = FontWeight.Black,
        fontSize = 19.sp,
        textAlign = TextAlign.Center
    )
    Spacer(modifier = Modifier.height(4.dp))
    Text(
        text = description,
        color = BackgroundCream.copy(alpha = 0.82f),
        fontWeight = FontWeight.Bold,
        fontSize = 10.sp,
        lineHeight = 12.sp,
        textAlign = TextAlign.Center
    )
}

@Composable
private fun SelectionSection(
    title: String,
    content: @Composable () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            text = title.uppercase(),
            color = AccentOrange,
            fontWeight = FontWeight.Black,
            fontSize = 10.sp,
            letterSpacing = 1.sp
        )
        content()
    }
}

@Composable
private fun SelectionGrid(
    options: List<CardCommandSelectionOption>,
    selectedValue: String?,
    onSelect: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        options.chunked(2).forEach { rowOptions ->
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                rowOptions.forEach { option ->
                    SelectionChip(
                        label = option.label,
                        selected = selectedValue == option.value,
                        onClick = { onSelect(option.value) },
                        modifier = Modifier.weight(1f)
                    )
                }
                if (rowOptions.size == 1) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun SelectionChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(999.dp)
    Box(
        modifier = modifier
            .clip(shape)
            .background(
                if (selected) {
                    AccentOrange
                } else {
                    BackgroundCream.copy(alpha = 0.1f)
                }
            )
            .border(
                width = 1.dp,
                color = if (selected) AccentOrange else BackgroundCream.copy(alpha = 0.26f),
                shape = shape
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 9.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = if (selected) CardDarkMaroon else BackgroundCream,
            fontWeight = FontWeight.Black,
            fontSize = 12.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun CardTypeWheel(
    options: List<CardCommandTypeOption>,
    selectedValue: String?,
    onSelect: (String) -> Unit
) {
    if (options.isEmpty()) {
        EmptySelectionMessage("No requestable cards.")
        return
    }

    val selectedIndex = options.indexOfFirst { it.wireValue == selectedValue }
        .takeIf { it >= 0 }
        ?: 0
    val selectedOption = options[selectedIndex]
    var dragOffset by remember(selectedValue) { mutableStateOf(0f) }
    val dragState = rememberDraggableState { delta ->
        dragOffset += delta
    }

    fun selectIndex(index: Int) {
        onSelect(options[index.coerceIn(options.indices)].wireValue)
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = OutlineDark.copy(alpha = 0.62f),
        border = BorderStroke(1.dp, BackgroundCream.copy(alpha = 0.18f))
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                WheelArrow(
                    label = "<",
                    enabled = selectedIndex > 0,
                    onClick = { selectIndex(selectedIndex - 1) }
                )

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(108.dp)
                        .draggable(
                            state = dragState,
                            orientation = Orientation.Horizontal,
                            onDragStopped = { velocity ->
                                when {
                                    dragOffset < -36f || velocity < -250f -> selectIndex(selectedIndex + 1)
                                    dragOffset > 36f || velocity > 250f -> selectIndex(selectedIndex - 1)
                                }
                                dragOffset = 0f
                            }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    CardFaceUp(
                        card = selectedOption.toPreviewCard(),
                        isSelected = true,
                        isDefocused = false,
                        cardWidth = 72.dp,
                        cardHeight = 108.dp,
                        labelFontSize = 10.sp
                    )
                }

                WheelArrow(
                    label = ">",
                    enabled = selectedIndex < options.lastIndex,
                    onClick = { selectIndex(selectedIndex + 1) }
                )
            }

            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = selectedOption.label,
                color = BackgroundCream,
                fontWeight = FontWeight.Black,
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )
            Text(
                text = "Swipe card or tap arrows  ${selectedIndex + 1}/${options.size}",
                color = AccentOrange.copy(alpha = 0.9f),
                fontWeight = FontWeight.Bold,
                fontSize = 9.sp,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun WheelArrow(
    label: String,
    enabled: Boolean,
    onClick: () -> Unit
) {
    val shape = RoundedCornerShape(999.dp)
    Box(
        modifier = Modifier
            .size(36.dp)
            .clip(shape)
            .background(
                if (enabled) {
                    AccentOrange
                } else {
                    BackgroundCream.copy(alpha = 0.08f)
                }
            )
            .then(
                if (enabled) {
                    Modifier.clickable(onClick = onClick)
                } else {
                    Modifier
                }
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = if (enabled) CardDarkMaroon else BackgroundCream.copy(alpha = 0.32f),
            fontWeight = FontWeight.Black,
            fontSize = 18.sp
        )
    }
}

@Composable
private fun HamsterTypeField(
    value: String,
    onValueChange: (String) -> Unit
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text("Example: hamster_ninja") },
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
        textStyle = androidx.compose.ui.text.TextStyle(
            color = BackgroundCream,
            fontWeight = FontWeight.Bold
        ),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = AccentOrange,
            unfocusedBorderColor = BackgroundCream.copy(alpha = 0.42f),
            focusedLabelColor = AccentOrange,
            unfocusedLabelColor = BackgroundCream.copy(alpha = 0.72f),
            cursorColor = AccentOrange,
            focusedTextColor = BackgroundCream,
            unfocusedTextColor = BackgroundCream
        )
    )
}

@Composable
private fun EmptySelectionMessage(message: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(OutlineDark.copy(alpha = 0.58f), RoundedCornerShape(14.dp))
            .padding(horizontal = 14.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = message,
            color = BackgroundCream.copy(alpha = 0.82f),
            fontWeight = FontWeight.Bold,
            fontSize = 13.sp,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun CommandActions(
    canConfirm: Boolean,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        OutlinedButton(
            onClick = onDismiss,
            modifier = Modifier.weight(1f),
            border = BorderStroke(1.dp, BackgroundCream.copy(alpha = 0.44f)),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = BackgroundCream)
        ) {
            Text("Cancel", fontWeight = FontWeight.Black)
        }
        Button(
            enabled = canConfirm,
            onClick = onConfirm,
            modifier = Modifier.weight(1f),
            colors = ButtonDefaults.buttonColors(
                containerColor = AccentOrange,
                contentColor = CardDarkMaroon,
                disabledContainerColor = OutlineDark.copy(alpha = 0.82f),
                disabledContentColor = BackgroundCream.copy(alpha = 0.48f)
            )
        ) {
            Text("Play", fontWeight = FontWeight.Black)
        }
    }
}
