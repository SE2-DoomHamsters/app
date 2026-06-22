package com.doomhamsters.ui.mascot

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import coil3.ImageLoader
import coil3.compose.AsyncImage
import coil3.compose.AsyncImagePainter
import coil3.compose.LocalPlatformContext
import coil3.gif.AnimatedImageDecoder
import coil3.request.ImageRequest
import com.doomhamsters.mascot.presentation.MascotAnimation
import com.doomhamsters.mascot.presentation.MascotPresentation

// Squash-and-stretch tuning. Anchored at the feet so the mascot compresses
// against the ground shadow.
private const val SQUASH_SCALE_Y = 0.18f      // how much shorter at full squash
private const val SQUASH_SCALE_X = 0.10f       // how much wider at full squash
// Feet position within the 450x450 sprite (245, 400), as fractions. The sprite
// fills the square box via ContentScale.Fit, so these map straight to the box.
private const val SQUASH_PIVOT_X = 245f / 450f
private const val SQUASH_PIVOT_Y = 400f / 450f
private const val SQUASH_DOWN_MS = 110         // squash in
private const val SQUASH_UP_MS = 180           // stretch back out

/**
 * Board mascot renderer for the animation resolved by MascotViewModelFeature.
 *
 * It makes no game decisions (those live in the feature/presentation), but it does
 * own the view-level animation: deciding whether a swap squashes, sequencing the
 * squash-and-stretch, and keeping the previous sprite until the next one loads.
 *
 * @param animation the animation to display, already resolved by the feature.
 * @param modifier placement/sizing for the mascot on the board.
 */
@Composable
fun BoardMascot(
    animation: MascotAnimation,
    modifier: Modifier = Modifier,
) {
    var previous by remember { mutableStateOf(animation) }
    val squashThisSwap = MascotPresentation.shouldSquash(from = previous, to = animation)
    LaunchedEffect(animation) { previous = animation }

    AnimatedGif(
        assetUri = animation.assetUri,
        squash = squashThisSwap,
        modifier = modifier,
    )
}

/**
 * Generic animated-GIF player with a flat ground shadow and an optional
 * squash-and-stretch on swap.
 *
 * - The previously shown sprite is kept on screen until the incoming one has
 *   loaded, so a swap never flashes an empty frame.
 * - When [squash] is set, the mascot squashes down, swaps the sprite at the
 *   bottom of the squash to hide the seam, then stretches back out.
 *
 * Uses [FilterQuality.None] so aliased pixel art stays crisp.
 */
@Composable
private fun AnimatedGif(
    assetUri: String,
    squash: Boolean,
    modifier: Modifier = Modifier,
    filterQuality: FilterQuality = FilterQuality.None,
) {
    val context = LocalPlatformContext.current
    val imageLoader = remember(context) {
        ImageLoader.Builder(context)
            .components { add(AnimatedImageDecoder.Factory()) }
            .build()
    }

    // This swap/transition state machine stays in the composable on purpose: it is
    // view-level animation, not a game decision. The squash sequence is driven by an
    // Animatable, the swap timing by LaunchedEffect, and the keep-previous overlay by
    // Coil's per-image load callback (see Sprite.onState) -- all Compose/Coil
    // primitives that cannot live in MascotViewModelFeature or the presentation layer.
    // The game-state-to-animation decisions (resolve/transitionFor/shouldSquash) are
    // kept out of here in MascotPresentation.

    // What is actually drawn (displayedUri swaps at the squash trough) vs. what has
    // finished loading (shown drives the keep-previous overlay).
    var displayedUri by remember { mutableStateOf(assetUri) }
    var shown by remember { mutableStateOf(assetUri) }
    val squashAnim = remember { Animatable(0f) } // 0 = at rest, 1 = fully squashed

    LaunchedEffect(assetUri) {
        if (assetUri == displayedUri) return@LaunchedEffect
        if (squash) {
            squashAnim.animateTo(1f, tween(SQUASH_DOWN_MS, easing = FastOutSlowInEasing))
            displayedUri = assetUri // swap hidden at the bottom of the squash
            squashAnim.animateTo(0f, tween(SQUASH_UP_MS, easing = FastOutSlowInEasing))
        } else {
            displayedUri = assetUri
        }
    }

    Box(modifier = modifier) {
        // Ground shadow: a squished ellipse that widens slightly as the mascot squashes.
        Canvas(modifier = Modifier.matchParentSize()) {
            val spread = 1f + SQUASH_SCALE_X * squashAnim.value
            val shadowWidth = size.width * 0.46f * spread
            val shadowHeight = shadowWidth * 0.26f
            val centerX = size.width * SQUASH_PIVOT_X
            val centerY = size.height * SQUASH_PIVOT_Y
            drawOval(
                color = Color.Black.copy(alpha = 0.20f),
                topLeft = Offset(centerX - shadowWidth / 2f, centerY - shadowHeight / 2f),
                size = Size(shadowWidth, shadowHeight),
            )
        }

        // The mascot, with the squash transform anchored at its feet.
        Box(
            modifier = Modifier
                .matchParentSize()
                .graphicsLayer {
                    val s = squashAnim.value
                    scaleY = 1f - SQUASH_SCALE_Y * s
                    scaleX = 1f + SQUASH_SCALE_X * s
                    transformOrigin = TransformOrigin(SQUASH_PIVOT_X, SQUASH_PIVOT_Y)
                }
        ) {
            // The loaded sprite, plus the incoming one on top while it loads. Each is
            // keyed by its uri so the already-loaded sprite keeps its identity (and
            // does not reload) when the incoming one becomes the shown one.
            val layers = if (displayedUri == shown) listOf(shown) else listOf(shown, displayedUri)
            layers.forEach { uri ->
                key(uri) {
                    Sprite(
                        assetUri = uri,
                        imageLoader = imageLoader,
                        filterQuality = filterQuality,
                        modifier = Modifier.matchParentSize(),
                        onLoaded = { if (uri == displayedUri) shown = uri },
                    )
                }
            }
        }
    }
}

@Composable
private fun Sprite(
    assetUri: String,
    imageLoader: ImageLoader,
    filterQuality: FilterQuality,
    modifier: Modifier = Modifier,
    onLoaded: () -> Unit = {},
) {
    val context = LocalPlatformContext.current
    val request = remember(assetUri, context) {
        ImageRequest.Builder(context)
            .data(assetUri)
            .build()
    }
    AsyncImage(
        model = request,
        imageLoader = imageLoader,
        contentDescription = null,
        modifier = modifier,
        contentScale = ContentScale.Fit,
        filterQuality = filterQuality,
        onState = { state ->
            if (state is AsyncImagePainter.State.Success) onLoaded()
        },
    )
}
