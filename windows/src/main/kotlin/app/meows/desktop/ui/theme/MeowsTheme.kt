package app.meows.desktop.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.materialkolor.rememberDynamicColorScheme

private val shapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp), small = RoundedCornerShape(12.dp), medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(24.dp), extraLarge = RoundedCornerShape(32.dp),
)

private fun typography(): Typography {
    val d = Typography()
    return d.copy(
        headlineMedium = d.headlineMedium.copy(fontWeight = FontWeight.Bold),
        titleLarge = d.titleLarge.copy(fontWeight = FontWeight.SemiBold),
        titleMedium = d.titleMedium.copy(fontWeight = FontWeight.SemiBold),
    )
}

private fun ColorScheme.amoled() = copy(
    background = Color.Black, surface = Color.Black, surfaceDim = Color.Black, surfaceContainerLowest = Color.Black,
    surfaceContainerLow = Color(0xFF0B0B0B), surfaceContainer = Color(0xFF121212),
    surfaceContainerHigh = Color(0xFF1A1A1A), surfaceContainerHighest = Color(0xFF222222),
)

/** Material 3: цвета из палитры (seed → схема), режим авто/светлая/тёмная, чёрный фон для тёмной. */
@Composable
fun MeowsTheme(themeMode: Int, palette: Int, amoled: Boolean, content: @Composable () -> Unit) {
    val dark = when (themeMode) { 1 -> false; 2 -> true; else -> isSystemInDarkTheme() }
    val i = palette.coerceIn(0, Palettes.seeds.lastIndex)
    var scheme = rememberDynamicColorScheme(seedColor = Palettes.seeds[i], isDark = dark, style = Palettes.styles[i])
    if (dark && amoled) scheme = scheme.amoled()
    MaterialTheme(colorScheme = scheme, typography = typography(), shapes = shapes, content = content)
}