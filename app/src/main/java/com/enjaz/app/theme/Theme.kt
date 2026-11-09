package com.enjaz.app.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val Crimson = Color(0xFF650D24)
val Gold = Color(0xFFD4AF37)
val Ink = Color(0xFF160D12)
val Surface1 = Color(0xFF22141A)
val TextSoft = Color(0xFFE9DFD2)
val Danger = Color(0xFFE53935)

private val Scheme = darkColorScheme(
    primary = Gold, onPrimary = Ink,
    secondary = Crimson, onSecondary = TextSoft,
    background = Ink, onBackground = TextSoft,
    surface = Surface1, onSurface = TextSoft,
    primaryContainer = Crimson, onPrimaryContainer = Gold
)

@Composable
fun EnjazTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = Scheme, content = content)
}
