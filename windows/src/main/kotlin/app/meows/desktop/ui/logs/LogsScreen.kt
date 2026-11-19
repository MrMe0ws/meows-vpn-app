package app.meows.desktop.ui.logs

import app.meows.desktop.i18n.tr
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.selection.SelectionContainer
import app.meows.desktop.ui.components.FilledTonalButton
import app.meows.desktop.ui.components.FilterChip
import androidx.compose.material3.MaterialTheme
import app.meows.desktop.ui.components.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.meows.desktop.log.AppLog
import app.meows.desktop.ui.home.copyText

private const val TRAFFIC = "TRAFFIC"
private fun filters() = listOf(tr("Всё") to null, tr("Трафик") to TRAFFIC, "VPN" to "VPN", tr("Ядро") to "CORE", tr("Подписка") to "SUB")

/** Логи: события приложения, вывод ядра и «Трафик» (куда и как пошло каждое соединение); фильтр по разделу, копирование для поддержки, очистка. Ссылка подписки в логи не попадает. */
@Composable
fun LogsScreen() {
    var tag by remember { mutableStateOf<String?>(null) }
    val showTraffic = tag == TRAFFIC
    val shown = if (showTraffic) emptyList() else AppLog.lines.filter { tag == null || it.tag == tag }
    val traffic = AppLog.traffic.toList()
    val list = rememberLazyListState()
    LaunchedEffect(shown.size, tag) { if (shown.isNotEmpty()) list.scrollToItem(shown.lastIndex) }

    Column(Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        Text(tr("Логи"), Modifier.padding(start = 8.dp, top = 20.dp, bottom = 12.dp), style = MaterialTheme.typography.headlineMedium)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            filters().forEach { (title, t) -> FilterChip(selected = tag == t, onClick = { tag = t }, label = { Text(title) }) }
            Spacer(Modifier.weight(1f))
            FilledTonalButton(
                onClick = { copyText(if (showTraffic) traffic.joinToString("\n") { AppLog.format(it) } else shown.joinToString("\n") { AppLog.format(it) }) },
                enabled = shown.isNotEmpty() || traffic.isNotEmpty() && showTraffic,
            ) { Text(tr("Скопировать")) }
            OutlinedButton(onClick = AppLog::clear, enabled = shown.isNotEmpty() || traffic.isNotEmpty()) { Text(tr("Очистить")) }
        }
        Spacer(Modifier.height(12.dp))
        Surface(Modifier.fillMaxSize().padding(bottom = 16.dp), shape = MaterialTheme.shapes.large, color = MaterialTheme.colorScheme.surfaceContainer) {
            if (showTraffic) TrafficList(traffic)
            else if (shown.isEmpty()) Text(tr("Пока пусто. Здесь появятся события подключения, подписки и ядра."), Modifier.padding(20.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
            else SelectionContainer {
                LazyColumn(Modifier.fillMaxSize().padding(12.dp), state = list) {
                    itemsIndexed(shown) { _, l ->
                        Text(AppLog.format(l), fontFamily = FontFamily.Monospace, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface)
                    }
                }
            }
        }
    }
}
