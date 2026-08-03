package com.songloft.tv.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.songloft.tv.data.storage.PreferencesDataStore
import com.songloft.tv.data.storage.dataStore
import kotlinx.coroutines.flow.map

private fun lightScheme(seed: Color) = lightColorScheme(
    primary = seed,
    onPrimary = Color.White,
    secondary = seed.copy(alpha = 0.8f),
    surface = Color(0xFFF5F5F5),
    onSurface = Color(0xFF1C1B1F),
    background = Color.White,
    onBackground = Color(0xFF1C1B1F),
    surfaceVariant = Color(0xFFE7E0EC),
    onSurfaceVariant = Color(0xFF49454F),
)

private fun darkScheme(seed: Color) = darkColorScheme(
    primary = seed,
    onPrimary = Color.White,
    secondary = seed.copy(alpha = 0.7f),
    surface = Color(0xFF1C1B1F),
    onSurface = Color(0xFFE6E1E5),
    background = Color(0xFF111827),
    onBackground = Color(0xFFE6E1E5),
    surfaceVariant = Color(0xFF2D2D2D),
    onSurfaceVariant = Color(0xFFCAC4D0),
)

val TvShapes = Shapes(
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(24.dp),
)

@Composable
fun TvTheme(
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val themeMode by remember {
        context.dataStore.data.map { it[PreferencesDataStore.THEME_MODE] ?: 0 }
    }.collectAsState(initial = 0)
    val themeColorName by remember {
        context.dataStore.data.map { it[PreferencesDataStore.THEME_COLOR] ?: ThemeSeeds.DEFAULT_NAME }
    }.collectAsState(initial = ThemeSeeds.DEFAULT_NAME)

    val darkTheme = when (themeMode) {
        1 -> false
        2 -> true
        else -> isSystemInDarkTheme()
    }
    val seed = seedColorFor(themeColorName)
    val colorScheme = if (darkTheme) darkScheme(seed) else lightScheme(seed)

    MaterialTheme(
        colorScheme = colorScheme,
        typography = androidx.compose.material3.Typography(),
        shapes = TvShapes,
        content = content
    )
}
