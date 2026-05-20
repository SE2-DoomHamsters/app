package com.doomhamsters.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

val BackgroundCream = Color(0xFFEBEBEB)
val CardDarkMaroon = Color(0xFF3B1525)
val AccentOrange = Color(0xFFF08C50)
val OutlineDark = Color(0xFF4A1A2A)
val SnackStashColor = Color(0xFF86B049)
val DoomColor = Color(0xFF9E2A2B)

private val DarkColorScheme = darkColorScheme(
    primary = AccentOrange,
    secondary = CardDarkMaroon,
    tertiary = OutlineDark,
    background = OutlineDark,
    surface = CardDarkMaroon,
    onPrimary = CardDarkMaroon,
    onSecondary = BackgroundCream,
    onTertiary = BackgroundCream,
    onBackground = BackgroundCream,
    onSurface = BackgroundCream
)

private val LightColorScheme = lightColorScheme(
    primary = AccentOrange,
    secondary = CardDarkMaroon,
    tertiary = OutlineDark,
    background = BackgroundCream,
    surface = BackgroundCream,
    onPrimary = BackgroundCream,
    onSecondary = BackgroundCream,
    onTertiary = BackgroundCream,
    onBackground = CardDarkMaroon,
    onSurface = CardDarkMaroon
)

/** Applies the app's Material theme to the provided content. */
@Composable
fun DoomHamstersTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}