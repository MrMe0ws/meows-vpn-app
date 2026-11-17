package app.meows.desktop.ui.logs

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.meows.desktop.log.Route
import app.meows.desktop.log.TrafficLine

/** Соединения: время · протокол · куда · путь (напрямую / через VPN / заблокировано). */
@Composable
fun TrafficList(lines: List<TrafficLine>) {
    val list = rememberLazyListState()
    LaunchedEffect(lines.size) { if (lines.isNotEmpty()) list.scrollToItem(lines.lastIndex) }
    if (lines.isEmpty()) {
        Text("Пока пусто. Подключитесь и откройте любой сайт — здесь будет видно, куда пошёл каждый запрос: напрямую или через VPN.", Modifier.padding(20.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
        return
    }
    LazyColumn(Modifier.fillMaxSize().padding(horizontal = 12.dp, vertical = 8.dp), state = list) {
        items(lines) { l ->
            Row(Modifier.padding(vertical = 3.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(app.meows.desktop.log.AppLog.time(l.time), fontFamily = FontFamily.Monospace, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(l.proto, fontFamily = FontFamily.Monospace, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.width(34.dp))
                Text(l.dest, Modifier.weight(1f), fontFamily = FontFamily.Monospace, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                RouteChip(l.route)
            }
        }
    }
}

@Composable
private fun RouteChip(r: Route) {
    val cs = MaterialTheme.colorScheme
    val (text, bg, fg) = when (r) {
        Route.Direct -> Triple("напрямую", cs.surfaceContainerHighest, cs.onSurfaceVariant)
        Route.Vpn -> Triple("через VPN", cs.primaryContainer, cs.onPrimaryContainer)
        Route.Block -> Triple("блок", cs.errorContainer, cs.onErrorContainer)
    }
    Surface(color = bg, contentColor = fg, shape = MaterialTheme.shapes.small) {
        Text(text, Modifier.padding(horizontal = 10.dp, vertical = 2.dp), style = MaterialTheme.typography.labelMedium, color = Color.Unspecified)
    }
    Spacer(Modifier.width(0.dp))
}
