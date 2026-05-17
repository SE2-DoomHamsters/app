package com.doomhamsters.ui.gameboard

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.doomhamsters.ui.theme.*

@Composable
fun DrawDeck(
    deckSize: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val displaySize = minOf(deckSize, 8)
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(140.dp, 190.dp)
            .clickable { onClick() }
    ) {
        if (deckSize > 0) {
            for (i in displaySize - 1 downTo 0) {
                CardFaceDown(
                    modifier = Modifier.graphicsLayer {
                        translationX = (i * 3f).dp.toPx()
                        translationY = (i * 3f).dp.toPx()
                    }
                )
            }
        }
        Text(
            text = "$deckSize",
            color = BackgroundCream,
            fontWeight = FontWeight.Bold,
            modifier = Modifier
                .background(CardDarkMaroon, RoundedCornerShape(12.dp))
                .padding(horizontal = 12.dp, vertical = 6.dp)
        )
    }
}
