package com.doomhamsters.ui.gameboard


import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.doomhamsters.ui.theme.*

@Composable
fun TooltipBubble(
    title: String,
    description: String,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    actionEnabled: Boolean = true,
    onAction: (() -> Unit)? = null
) {
    var isVisible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { isVisible = true }

    val scaleX by animateFloatAsState(targetValue = if (isVisible) 1f else 0.2f, animationSpec = spring(dampingRatio = 0.5f, stiffness = Spring.StiffnessMedium), label = "bubbleScaleX")
    val scaleY by animateFloatAsState(targetValue = if (isVisible) 1f else 0.0f, animationSpec = spring(dampingRatio = 0.4f, stiffness = Spring.StiffnessLow), label = "bubbleScaleY")

    Box(
        modifier = modifier
            .graphicsLayer {
                this.scaleX = scaleX
                this.scaleY = scaleY
                transformOrigin = TransformOrigin(0.5f, 1f)
            }
            .drawWithCache {
                val cornerRadius = 8.dp.toPx()
                val pointerHeight = 8.dp.toPx()
                val pointerWidth = 12.dp.toPx()
                val rectHeight = size.height - pointerHeight

                val path = Path().apply {
                    moveTo(0f, cornerRadius)
                    quadraticTo(0f, 0f, cornerRadius, 0f)
                    lineTo(size.width - cornerRadius, 0f)
                    quadraticTo(size.width, 0f, size.width, cornerRadius)
                    lineTo(size.width, rectHeight - cornerRadius)
                    quadraticTo(size.width, rectHeight, size.width - cornerRadius, rectHeight)
                    lineTo(size.width / 2f + pointerWidth / 2f, rectHeight)
                    lineTo(size.width / 2f, size.height)
                    lineTo(size.width / 2f - pointerWidth / 2f, rectHeight)
                    lineTo(cornerRadius, rectHeight)
                    quadraticTo(0f, rectHeight, 0f, rectHeight - cornerRadius)
                    close()
                }

                onDrawBehind {
                    drawPath(path, color = BackgroundCream)
                    drawPath(path, color = CardDarkMaroon, style = Stroke(width = 4f))
                }
            }
            .padding(horizontal = 6.dp)
            .padding(top = 6.dp, bottom = 14.dp)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = title, color = CardDarkMaroon, fontWeight = FontWeight.Black, fontSize = 12.sp)
            Text(text = description, color = CardDarkMaroon, fontWeight = FontWeight.Bold, fontSize = 9.sp, textAlign = TextAlign.Center, lineHeight = 10.sp)
            if (actionLabel != null && onAction != null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
                        .background(
                            if (actionEnabled) AccentOrange else OutlineDark.copy(alpha = 0.45f),
                            RoundedCornerShape(999.dp)
                        )
                        .then(
                            if (actionEnabled) {
                                Modifier.clickable { onAction() }
                            } else {
                                Modifier
                            }
                        )
                        .padding(vertical = 6.dp, horizontal = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = actionLabel,
                        color = BackgroundCream,
                        fontWeight = FontWeight.Black,
                        fontSize = 10.sp,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}
