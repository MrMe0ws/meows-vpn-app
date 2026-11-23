package app.meows.vpn.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import com.materialkolor.hct.Hct
import com.materialkolor.ktx.toColor
import com.materialkolor.ktx.toHct

/** Индекс палитры «Сумерки» — глубокий сине-фиолетовый, как у Happ. */
const val INDIGO_PALETTE = 11

/**
 * Разводит слои: фон темнее карточек, а у светлой темы карточки белые на сером фоне
 * (как в системных настройках Android). Без этого карточки сливаются с фоном.
 * Каждый «этаж» контейнеров сдвигается на ступень, так что диалоги и шторки остаются выше карточек.
 */
fun ColorScheme.separateLayers(dark: Boolean): ColorScheme = if (dark) copy(
    background = surfaceContainerLowest, surface = surfaceContainerLowest, surfaceDim = surfaceContainerLowest,
    surfaceContainerLow = surfaceContainer, surfaceContainer = surfaceContainerHigh,
    surfaceContainerHigh = surfaceContainerHighest, surfaceContainerHighest = lerp(surfaceContainerHighest, onSurface, 0.10f),
) else copy(
    background = surfaceContainer, surface = surfaceContainer, surfaceDim = surfaceContainerHigh,
    surfaceContainerLow = surfaceContainerLowest, surfaceContainer = surfaceContainerLowest,
    surfaceContainerHigh = surfaceContainerLowest, surfaceContainerHighest = surfaceContainerHighest,
)

/** Тёмная «Сумерки»: тёмно-синий фон, чуть более светлые индиго-карточки, мягкий сине-фиолетовый акцент. */
fun ColorScheme.indigoDark(): ColorScheme = copy(
    primary = Color(0xFF9A93FF), onPrimary = Color(0xFF1B1666),
    primaryContainer = Color(0xFF332E8C), onPrimaryContainer = Color(0xFFE2DFFF),
    secondary = Color(0xFFC3C0F0), onSecondary = Color(0xFF2B2955),
    secondaryContainer = Color(0xFF2E2C66), onSecondaryContainer = Color(0xFFE2E0FF),
    tertiary = Color(0xFF8FB4FF), onTertiary = Color(0xFF0F2A66),
    tertiaryContainer = Color(0xFF26407F), onTertiaryContainer = Color(0xFFD9E5FF),
    background = Color(0xFF12103A), surface = Color(0xFF12103A), surfaceDim = Color(0xFF0E0C2E),
    onBackground = Color(0xFFE6E4FF), onSurface = Color(0xFFE6E4FF), onSurfaceVariant = Color(0xFFA9A6D6),
    surfaceContainerLowest = Color(0xFF0E0C2E), surfaceContainerLow = Color(0xFF191650),
    surfaceContainer = Color(0xFF211E5E), surfaceContainerHigh = Color(0xFF2A2770), surfaceContainerHighest = Color(0xFF353282),
    outline = Color(0xFF7C79B5), outlineVariant = Color(0xFF3A3779),
)

/**
 * Тёмная тема, окрашенная в цвет палитры (как «Сумерки»): фон и карточки берут оттенок seed-цвета,
 * а не серые. [chroma] — насыщенность поверхностей (0 — чисто серые). Акценты остаются от генератора схем.
 */
fun ColorScheme.tintedDark(seed: Color, chroma: Double): ColorScheme {
    val hue = seed.toHct().hue
    fun c(tone: Double, k: Double = 1.0) = Hct.from(hue, chroma * k, tone).toColor()
    return copy(
        background = c(7.0), surface = c(7.0), surfaceDim = c(4.0),
        surfaceContainerLowest = c(4.0), surfaceContainerLow = c(11.0), surfaceContainer = c(15.0),
        surfaceContainerHigh = c(20.0), surfaceContainerHighest = c(26.0),
        onBackground = c(92.0, 0.2), onSurface = c(92.0, 0.2), onSurfaceVariant = c(76.0, 0.4),
        outline = c(56.0, 0.4), outlineVariant = c(30.0, 0.6),
    )
}
