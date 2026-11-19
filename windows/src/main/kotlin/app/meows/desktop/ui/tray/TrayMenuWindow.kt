package app.meows.desktop.ui.tray

import app.meows.desktop.i18n.tr
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogWindow
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.rememberDialogState
import app.meows.desktop.AppModel
import app.meows.desktop.ConnState
import app.meows.desktop.ui.Res
import app.meows.desktop.ui.components.hand
import app.meows.desktop.ui.theme.MeowsTheme
import java.awt.GraphicsEnvironment
import java.awt.Point
import java.awt.event.WindowAdapter
import java.awt.event.WindowEvent

private const val W = 256
private const val H = 212

/** Меню трея в стиле приложения (Material 3): статус, «Открыть», «Подключить/Отключить», «Выход». Закрывается при потере фокуса. */
@Composable
fun TrayMenuWindow(app: AppModel, anchor: Point?, onHide: () -> Unit) {
    if (anchor == null) return
    val area = GraphicsEnvironment.getLocalGraphicsEnvironment().maximumWindowBounds   // экран без панели задач
    val x = (anchor.x - W / 2).coerceIn(area.x, area.x + area.width - W)
    val y = (if (anchor.y > area.y + area.height) area.y + area.height - H - 6 else anchor.y).coerceIn(area.y, area.y + area.height - H)
    val state = rememberDialogState(position = WindowPosition(x.dp, y.dp), size = DpSize(W.dp, H.dp))
    DialogWindow(onCloseRequest = onHide, state = state, title = "Meows", undecorated = true, transparent = true, resizable = false, alwaysOnTop = true) {
        DisposableEffect(window) {
            val l = object : WindowAdapter() { override fun windowLostFocus(e: WindowEvent?) { onHide() } }
            window.addWindowFocusListener(l); window.toFront(); window.requestFocus()
            onDispose { window.removeWindowFocusListener(l) }
        }
        MeowsTheme(app.themeMode, app.palette, app.amoled) {
            val cs = MaterialTheme.colorScheme
            Surface(Modifier.fillMaxSize().padding(4.dp), shape = RoundedCornerShape(16.dp), color = cs.surfaceContainer, border = BorderStroke(1.dp, cs.outlineVariant), shadowElevation = 8.dp) {
                Column(Modifier.padding(vertical = 8.dp)) {
                    Row(Modifier.padding(horizontal = 16.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                        Image(Res.bitmap("icon.png"), null, Modifier.size(32.dp)); Spacer(Modifier.width(12.dp))
                        Column {
                            Text("Meows", style = MaterialTheme.typography.titleMedium)
                            val on = app.state == ConnState.Connected
                            Text(
                                if (on) tr("Подключено") else if (app.state == ConnState.Connecting) tr("Подключаемся…") else tr("Отключено"),
                                style = MaterialTheme.typography.bodySmall, color = if (on) cs.primary else cs.onSurfaceVariant,
                            )
                        }
                    }
                    HorizontalDivider(Modifier.padding(vertical = 6.dp), color = cs.outlineVariant)
                    Item(tr("Открыть Meows")) { onHide(); app.windowVisible = true; app.showTick++ }
                    Item(if (app.state == ConnState.Idle) tr("Подключить") else tr("Отключить")) { onHide(); app.toggle() }
                    HorizontalDivider(Modifier.padding(vertical = 6.dp), color = cs.outlineVariant)
                    Item(tr("Выход")) { onHide(); app.shutdown(); app.exitRequested = true }
                }
            }
        }
    }
}

@Composable
private fun Item(text: String, onClick: () -> Unit) {
    Box(Modifier.fillMaxWidth().height(40.dp).clickable(onClick = onClick).hand().padding(horizontal = 16.dp), contentAlignment = Alignment.CenterStart) {
        Text(text, style = MaterialTheme.typography.bodyLarge)
    }
}
