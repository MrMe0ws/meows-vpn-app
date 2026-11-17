package app.meows.desktop

import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.background
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.remember
import androidx.compose.ui.BiasAlignment
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.BitmapPainter
import app.meows.desktop.ui.MeowsApp
import app.meows.desktop.ui.Res
import app.meows.desktop.ui.theme.MeowsTheme

fun main() = application {
    val app = remember { AppModel() }
    Window(
        onCloseRequest = { app.shutdown(); exitApplication() },
        title = "Meows",
        icon = BitmapPainter(Res.bitmap("icon.png")),
        state = rememberWindowState(size = DpSize(460.dp, 880.dp)),
    ) {
        window.minimumSize = java.awt.Dimension(400, 640)
        MeowsTheme(app.themeMode, app.palette, app.amoled) {
            // на широком окне держим колонку как на телефоне: контент не растягивается
            Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface), contentAlignment = Alignment.TopCenter) {
                Box(Modifier.widthIn(max = 560.dp).fillMaxSize()) { MeowsApp(app) }
            }
        }
    }
}