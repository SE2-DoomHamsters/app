package com.doomhamsters.ui.cheating

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.doomhamsters.cheating.presentation.SnackStashResolutionDisplay
import com.doomhamsters.cheating.presentation.SnackStashResolutionTone
import com.doomhamsters.ui.theme.AccentOrange
import com.doomhamsters.ui.theme.BackgroundCream
import com.doomhamsters.ui.theme.CardDarkMaroon
import com.doomhamsters.ui.theme.DoomColor
import com.doomhamsters.ui.theme.SnackStashColor

/** Shows the public result of a resolved Snack Stash claim. */
@Composable
fun SnackStashResolutionOverlay(
    display: SnackStashResolutionDisplay,
    onDismiss: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.78f))
            .zIndex(11f)
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .zIndex(12f)
            .padding(top = 118.dp),
        contentAlignment = Alignment.TopCenter
    ) {
        Surface(
            shape = RoundedCornerShape(22.dp),
            color = CardDarkMaroon.copy(alpha = 0.98f)
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(display.title, color = display.tone.toColor(), fontWeight = FontWeight.Black, fontSize = 28.sp)
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = display.message,
                    color = BackgroundCream,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
                display.lifeChangeLines.forEach { line ->
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = line,
                        color = AccentOrange,
                        fontWeight = FontWeight.Black,
                        fontSize = 15.sp
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    colors = ButtonDefaults.buttonColors(containerColor = AccentOrange),
                    onClick = onDismiss
                ) {
                    Text("Continue", color = CardDarkMaroon, fontWeight = FontWeight.Black)
                }
            }
        }
    }
}

private fun SnackStashResolutionTone.toColor(): Color = when (this) {
    SnackStashResolutionTone.DOOM -> DoomColor
    SnackStashResolutionTone.SNACK_STASH -> SnackStashColor
    SnackStashResolutionTone.ACCENT -> AccentOrange
}
