package com.doomhamsters.ui.gameboard

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import com.doomhamsters.cards.displayName
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.doomhamsters.model.Card
import com.doomhamsters.model.CardType
import com.doomhamsters.ui.theme.*

/** Renders the shared card-back artwork. */
@Composable
fun CardFaceDown(modifier: Modifier = Modifier, shadowOffset: () -> Float = { 4f }) {
    Box(
        modifier = modifier
            .width(100.dp)
            .height(150.dp)
            .drawBehind {
                val radiusPx = 8.dp.toPx()
                drawRoundRect(
                    color = OutlineDark,
                    topLeft = Offset(0f, shadowOffset().dp.toPx()),
                    size = size,
                    cornerRadius = CornerRadius(radiusPx, radiusPx)
                )
            }
            .background(OutlineDark, RoundedCornerShape(8.dp))
            .padding(2.dp)
            .background(CardDarkMaroon, RoundedCornerShape(6.dp))
            .padding(3.dp)
            .background(OutlineDark, RoundedCornerShape(4.dp))
    )
}

/** Renders the visible front face for a specific card. */
@Composable
fun CardFaceUp(card: Card, isSelected: Boolean, isDefocused: Boolean, modifier: Modifier = Modifier, shadowOffset: () -> Float = { 4f }) {
    val (bgColor, innerBorderColor, textColor) = when (card.type) {
        CardType.Doom -> Triple(DoomColor, Color(0xFFC94A4A), BackgroundCream)
        CardType.SnackStash -> Triple(SnackStashColor, Color(0xFFA3C968), CardDarkMaroon)
        CardType.PowerNap -> Triple(Color(0xFFB8D8E8), Color(0xFF6A9FB5), CardDarkMaroon)
        CardType.QuickPeek -> Triple(Color(0xFFF8E7A2), Color(0xFFD6A93A), CardDarkMaroon)
        CardType.CageSwap -> Triple(Color(0xFFB2DFDB), Color(0xFF4DB6AC), CardDarkMaroon)
        CardType.SignOfFate -> Triple(Color(0xFFDCC8FF), Color(0xFF8B6BC7), CardDarkMaroon)
        CardType.SniffAhead -> Triple(Color(0xFFD4EED4), Color(0xFF6AAF6A), CardDarkMaroon)
        CardType.BegForSnacks -> Triple(Color(0xFFE8C9A0), Color(0xFFB8864E), CardDarkMaroon)
        CardType.FourHamsters -> Triple(Color(0xFFE1BEE7), Color(0xFFBA68C8), CardDarkMaroon)
        CardType.HyperMode -> Triple(Color(0xFFFFA726), Color(0xFFF57C00), CardDarkMaroon)
        CardType.StealCard -> Triple(Color(0xFFF8E7A2), Color(0xFFD6A93A), CardDarkMaroon)
        CardType.Normal -> Triple(BackgroundCream, AccentOrange, CardDarkMaroon)
    }

    val actualInnerBorder = if (isSelected) Color.White else innerBorderColor
    val animatedDarkness by animateFloatAsState(targetValue = if (isDefocused) 0.7f else 0f, animationSpec = tween(250), label = "darkOverlay")

    Box(
        modifier = modifier
            .width(100.dp)
            .height(150.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .drawBehind {
                    val radiusPx = 8.dp.toPx()
                    drawRoundRect(
                        color = OutlineDark,
                        topLeft = Offset(0f, shadowOffset().dp.toPx()),
                        size = size,
                        cornerRadius = CornerRadius(radiusPx, radiusPx)
                    )
                }
                .background(OutlineDark, RoundedCornerShape(8.dp))
                .padding(2.dp)
                .background(actualInnerBorder, RoundedCornerShape(6.dp))
                .padding(3.dp)
                .background(bgColor, RoundedCornerShape(4.dp))
        ) {
            Box(modifier = Modifier.fillMaxSize().padding(8.dp)) {
                Text(
                    text = card.displayName().replace(" ", "\n"),
                    color = textColor,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.align(Alignment.Center)
                )
            }
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer { alpha = animatedDarkness }
                .background(OutlineDark, RoundedCornerShape(8.dp))
        )
    }
}
