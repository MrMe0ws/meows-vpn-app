package app.meows.desktop.ui.home

import app.meows.desktop.i18n.tr
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material3.CircularProgressIndicator
import app.meows.desktop.ui.components.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.meows.desktop.AppModel
import app.meows.desktop.ConnState

/** Правая панель: режим работы сверху, большая кнопка подключения и статус в центре, выбранный сервер и пинг внизу. */
@Composable
fun ConnectPane(app: AppModel, modifier: Modifier = Modifier) {
    Surface(modifier, shape = MaterialTheme.shapes.extraLarge, color = MaterialTheme.colorScheme.surfaceContainerLow) {
        Column(Modifier.fillMaxSize().padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            ModePicker(app.mode, app.allVpn, enabled = app.state != ConnState.Connecting, onPick = app::chooseMode, onAllVpn = app::chooseAllVpn)
            Spacer(Modifier.weight(1f))
            ConnectButton(app.state, 240.dp) { app.toggle() }
            Spacer(Modifier.height(16.dp))
            StatusText(app.state, app.connectedAt)
            Spacer(Modifier.weight(1f))
            SelectedServer(app)
        }
    }
}

@Composable
private fun SelectedServer(app: AppModel) {
    val server = app.sub?.servers?.getOrNull(app.selected) ?: return
    Surface(Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.large, color = MaterialTheme.colorScheme.surfaceContainerHigh) {
        Column(Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                FlagBadge(server.flag)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(server.title, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(server.subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                app.pings[app.selected]?.let { PingChip(it) }
            }
            Spacer(Modifier.height(12.dp))
            FilledTonalButton(onClick = { app.pingServer(app.selected) }, enabled = !app.pingingSelected, modifier = Modifier.fillMaxWidth()) {
                if (app.pingingSelected) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp) else Icon(Icons.Rounded.Speed, null, Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp)); Text(tr("Проверить пинг"))
            }
        }
    }
}
