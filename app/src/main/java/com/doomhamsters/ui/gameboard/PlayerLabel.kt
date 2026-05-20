package com.doomhamsters.ui.gameboard

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.doomhamsters.ui.theme.*

/** Renders a player's name and life total with the board label styling. */
@Composable
fun PlayerLabel(name: String, lives: Int, modifier: Modifier = Modifier) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = modifier) {
        OutlinedText(text = name, outlineColor = Color.White, fillColor = CardDarkMaroon, fontSize = 20.sp, outlineWidth = 2.dp)
        OutlinedText(text = "Lives: $lives", outlineColor = Color.White, fillColor = CardDarkMaroon, fontSize = 16.sp, outlineWidth = 1.5.dp)
    }
}

/** Draws bold text with a filled center and outline stroke. */
@Composable
fun OutlinedText(text: String, outlineColor: Color, fillColor: Color, fontSize: TextUnit, outlineWidth: Dp = 2.dp) {
    Box(contentAlignment = Alignment.Center) {
        Text(
            text = text,
            color = outlineColor,
            fontSize = fontSize,
            fontWeight = FontWeight.Black,
            style = TextStyle.Default.copy(
                drawStyle = Stroke(
                    miter = 10f,
                    width = with(LocalDensity.current) { outlineWidth.toPx() },
                    join = StrokeJoin.Round
                )
            )
        )
        Text(
            text = text,
            color = fillColor,
            fontSize = fontSize,
            fontWeight = FontWeight.Black
        )
    }
}
