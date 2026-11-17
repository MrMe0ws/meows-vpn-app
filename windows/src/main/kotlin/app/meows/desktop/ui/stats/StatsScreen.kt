package app.meows.desktop.ui.stats

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.meows.desktop.AppModel
import app.meows.desktop.ConnState
import app.meows.desktop.log.AppLog
import app.meows.desktop.log.Route
import app.meows.desktop.stats.StatsFeed
import app.meows.desktop.ui.formatBytesRu
import kotlinx.coroutines.delay

/** Статистика: скорость сейчас и график за 2 минуты, трафик за подключение (через VPN / напрямую), куда чаще ходили. Замера скорости нет — он грузит серверы. */
@Composable
fun StatsScreen(app: AppModel) {
    var tick by remember { mutableIntStateOf(0) }
    LaunchedEffect(Unit) { while (true) { delay(1000); tick++ } }
    val samples = remember(tick) { StatsFeed.samples() }
    val t = remember(tick) { StatsFeed.totals() }
    val now = samples.lastOrNull()
    val cs = MaterialTheme.colorScheme

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp)) {
        Text("Статистика", Modifier.padding(start = 8.dp, top = 20.dp, bottom = 16.dp), style = MaterialTheme.typography.headlineMedium)
        if (app.state != ConnState.Connected) {
            Surface(Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.extraLarge, color = cs.surfaceContainer) {
                Text("Подключитесь — здесь появятся скорость и график трафика.", Modifier.padding(24.dp), color = cs.onSurfaceVariant)
            }
            return@Column
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Speed("Получение", now?.down ?: 0, cs.primary, Modifier.weight(1f))
            Speed("Отправка", now?.up ?: 0, cs.tertiary, Modifier.weight(1f))
        }
        Spacer(Modifier.height(12.dp))
        Surface(Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.extraLarge, color = cs.surfaceContainer) {
            Column(Modifier.padding(20.dp)) {
                Text("Последние 2 минуты", style = MaterialTheme.typography.labelLarge, color = cs.onSurfaceVariant)
                Spacer(Modifier.height(8.dp))
                SpeedChart(samples, Modifier.fillMaxWidth().height(160.dp))
            }
        }
        Spacer(Modifier.height(12.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Totals("Через VPN", t.vpnDown, t.vpnUp, Modifier.weight(1f))
            Totals("Напрямую", t.directDown, t.directUp, Modifier.weight(1f))
        }
        Spacer(Modifier.height(12.dp))
        TopDomains(tick)
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun Speed(title: String, bytesPerSec: Long, color: androidx.compose.ui.graphics.Color, modifier: Modifier) {
    Surface(modifier, shape = MaterialTheme.shapes.extraLarge, color = MaterialTheme.colorScheme.surfaceContainer) {
        Column(Modifier.padding(20.dp)) {
            Text(title, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(formatBytesRu(bytesPerSec) + "/с", style = MaterialTheme.typography.headlineMedium, color = color)
        }
    }
}

@Composable
private fun Totals(title: String, down: Long, up: Long, modifier: Modifier) {
    Surface(modifier, shape = MaterialTheme.shapes.extraLarge, color = MaterialTheme.colorScheme.surfaceContainer) {
        Column(Modifier.padding(20.dp)) {
            Text(title, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(6.dp))
            Text("Получено: " + formatBytesRu(down), style = MaterialTheme.typography.bodyLarge)
            Text("Отправлено: " + formatBytesRu(up), style = MaterialTheme.typography.bodyLarge)
        }
    }
}

/** Куда чаще всего ходили (по числу соединений из журнала трафика) и каким путём. */
@Composable
private fun TopDomains(tick: Int) {
    val cs = MaterialTheme.colorScheme
    val top = remember(tick) {
        AppLog.traffic.toList().groupBy { it.dest.substringBeforeLast(':') }
            .map { (host, l) -> Triple(host, l.size, l.last().route) }.sortedByDescending { it.second }.take(8)
    }
    Surface(Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.extraLarge, color = cs.surfaceContainer) {
        Column(Modifier.padding(20.dp)) {
            Text("Куда чаще ходили", style = MaterialTheme.typography.labelLarge, color = cs.onSurfaceVariant)
            Spacer(Modifier.height(8.dp))
            if (top.isEmpty()) Text("Пока нет данных.", color = cs.onSurfaceVariant)
            top.forEach { (host, n, route) ->
                Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(host, Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text("$n", Modifier.padding(horizontal = 12.dp), color = cs.onSurfaceVariant)
                    Box(Modifier.width(96.dp)) {
                        Text(when (route) { Route.Direct -> "напрямую"; Route.Vpn -> "через VPN"; Route.Block -> "блок" }, color = if (route == Route.Vpn) cs.primary else cs.onSurfaceVariant, style = MaterialTheme.typography.labelLarge)
                    }
                }
            }
        }
    }
}
