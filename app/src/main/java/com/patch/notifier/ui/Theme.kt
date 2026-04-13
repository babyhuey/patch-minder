package com.patch.notifier.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val Navy = Color(0xFF1A1A2E)
val NavyLight = Color(0xFF252547)
val Blue = Color(0xFF3D5AFE)
val BlueBright = Color(0xFF6979F8)
val TextPrimary = Color(0xFFE0E0FF)
val TextSecondary = Color(0xFF8B8BA7)
val TextMuted = Color(0xFF6C6C8A)
val BorderColor = Color(0xFF4A4A7A)

private val DarkColors = darkColorScheme(
    primary = Blue,
    onPrimary = Color.White,
    background = Navy,
    surface = NavyLight,
    onBackground = TextPrimary,
    onSurface = TextPrimary,
    outline = BorderColor,
    onSurfaceVariant = TextSecondary,
)

@Composable
fun PatchTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkColors,
        content = content,
    )
}
