package com.songloft.tv.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

private val LightColorScheme = lightColorScheme(
    primary = Seed,
    onPrimary = androidx.compose.ui.graphics.Color.White,
    secondary = Seed.copy(alpha = 0.8f),
    surface = androidx.compose.ui.graphics.Color(0xFFF5F5F5),
    onSurface = androidx.compose.ui.graphics.Color(0xFF1C1B1F),
    background = androidx.compose.ui.graphics.Color.White,
    onBackground = androidx.compose.ui.graphics.Color(0xFF1C1B1F),
    surfaceVariant = androidx.compose.ui.graphics.Color(0xFFE7E0EC),
    onSurfaceVariant = androidx.compose.ui.graphics.Color(0xFF49454F),
)

private val DarkColorScheme = darkColorScheme(
    primary = Seed,
    onPrimary = androidx.compose.ui.graphics.Color.White,
    secondary = Seed.copy(alpha = 0.7f),
    surface = androidx.compose.ui.graphics.Color(0xFF1C1B1F),
    onSurface = androidx.compose.ui.graphics.Color(0xFFE6E1E5),
    background = androidx.compose.ui.graphics.Color(0xFF111827),
    onBackground = androidx.compose.ui.graphics.Color(0xFFE6E1E5),
    surfaceVariant = androidx.compose.ui.graphics.Color(0xFF2D2D2D),
    onSurfaceVariant = androidx.compose.ui.graphics.Color(0xFFCAC4D0),
)

val TvShapes = Shapes(
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(24.dp),
)

@Composable
fun TvTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = androidx.compose.material3.Typography(),
        shapes = TvShapes,
        content = content
    )
}
