package com.roninx.app.ui.theme

import androidx.compose.material.MaterialTheme
import androidx.compose.material.Typography
import androidx.compose.material.darkColors
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorPalette = darkColors(
    primary = RoninRed,
    primaryVariant = RoninRed,
    secondary = RoninGold,
    background = RoninDark,
    surface = RoninSurface,
    onPrimary = Color.White,
    onSecondary = Color.Black,
    onBackground = RoninText,
    onSurface = RoninText
)

@Composable
fun RoninTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colors = DarkColorPalette,
        content = content
    )
}
