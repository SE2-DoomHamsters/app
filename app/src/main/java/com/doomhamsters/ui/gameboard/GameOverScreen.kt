package com.doomhamsters.ui.gameboard

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.ImageLoader
import coil3.compose.AsyncImage
import coil3.gif.AnimatedImageDecoder
import coil3.request.ImageRequest
import com.doomhamsters.ui.theme.AccentOrange
import com.doomhamsters.ui.theme.BackgroundCream
import com.doomhamsters.ui.theme.CardDarkMaroon
import com.doomhamsters.ui.theme.DoomColor
import com.doomhamsters.ui.theme.OutlineDark
import kotlinx.coroutines.launch

/** Displays the winner and a return-to-lobby action after a game ends. */
@Composable
fun GameOverScreen(
    winnerId: String,
    winnerName: String = winnerId,
    onRestart: () -> Unit
) {
    val context = LocalContext.current
    val imageLoader = remember(context) {
        ImageLoader.Builder(context)
            .components { add(AnimatedImageDecoder.Factory()) }
            .build()
    }

    val bgAlpha = remember { Animatable(0f) }
    val panelOffsetY = remember { Animatable(160f) }
    val panelAlpha = remember { Animatable(0f) }
    val returnProgress = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        launch { bgAlpha.animateTo(1f, tween(600)) }
        launch {
            kotlinx.coroutines.delay(250)
            launch {
                panelOffsetY.animateTo(0f, tween(550, easing = FastOutSlowInEasing))
            }
            panelAlpha.animateTo(1f, tween(500))
        }
        returnProgress.animateTo(1f, tween(2500, easing = LinearEasing))
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .alpha(bgAlpha.value)
            .background(CardDarkMaroon)
    ) {
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .width(6.dp)
                .background(AccentOrange)
                .align(Alignment.CenterStart)
        )
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .width(6.dp)
                .background(AccentOrange)
                .align(Alignment.CenterEnd)
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 32.dp)
                .statusBarsPadding()
                .navigationBarsPadding(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.weight(1f))

            Text(
                text = "GAME OVER",
                color = DoomColor,
                fontWeight = FontWeight.Black,
                fontSize = 44.sp,
                letterSpacing = 4.sp
            )

            Spacer(modifier = Modifier.height(28.dp))

            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data("file:///android_asset/mascot/your_turn.gif")
                    .build(),
                imageLoader = imageLoader,
                contentDescription = null,
                modifier = Modifier.size(160.dp)
            )

            Spacer(modifier = Modifier.height(28.dp))

            Box(
                modifier = Modifier
                    .graphicsLayer {
                        translationY = panelOffsetY.value
                        alpha = panelAlpha.value
                    }
                    .fillMaxWidth()
                    .background(OutlineDark.copy(alpha = 0.9f), RoundedCornerShape(20.dp))
                    .border(2.dp, AccentOrange, RoundedCornerShape(20.dp))
                    .padding(horizontal = 24.dp, vertical = 28.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "★  WINNER  ★",
                        color = AccentOrange,
                        fontWeight = FontWeight.Black,
                        fontSize = 13.sp,
                        letterSpacing = 6.sp
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = winnerName,
                        color = BackgroundCream,
                        fontWeight = FontWeight.Black,
                        fontSize = 34.sp,
                        textAlign = TextAlign.Center
                    )
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            LinearProgressIndicator(
                progress = { returnProgress.value },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp)),
                color = AccentOrange,
                trackColor = OutlineDark
            )

            Spacer(modifier = Modifier.height(14.dp))

            Button(
                onClick = onRestart,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(50),
                colors = ButtonDefaults.buttonColors(containerColor = AccentOrange)
            ) {
                Text(
                    text = "Back to Lobby",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = CardDarkMaroon
                )
            }

            Spacer(modifier = Modifier.height(28.dp))
        }
    }
}
