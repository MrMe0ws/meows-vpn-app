package app.meows.desktop.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.awt.ComposeWindow
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import app.meows.desktop.system.TitleBar

/** Заголовок окна — почти цвет темы: цвет рейки с примесью основного цвета. Меняется вместе с темой и палитрой. */
@Composable
fun TitleBarTint(window: ComposeWindow, visible: Boolean) {
    val cs = MaterialTheme.colorScheme
    val bg = lerp(cs.surfaceContainer, cs.primary, 0.18f)
    val dark = cs.surface.luminance() < 0.5f
    LaunchedEffect(bg, dark, visible) { TitleBar.apply(window.windowHandle, bg.toArgb(), cs.onSurface.toArgb(), dark) }
}
