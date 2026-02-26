package com.robocar.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColors = darkColorScheme(
    primary = AccentBlue,
    onPrimary = Color.White,
    secondary = AccentGreen,
    onSecondary = Color.White,
    error = AccentRed,
    background = BgDark,
    surface = CardBg,
    onBackground = TextPrimary,
    onSurface = TextPrimary
)

@Composable
fun RoboCarTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkColors,
        content = content
    )
}
