package app.meows.vpn.ui.settings.pages

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import app.meows.vpn.MeowsViewModel
import app.meows.vpn.XKVpnService
import app.meows.vpn.stats.StatsFeed
import app.meows.vpn.ui.components.SpeedChart
import app.meows.vpn.ui.components.formatBytesRu
import app.meows.vpn.ui.settings.SettingsPageScaffold
import kotlinx.coroutines.delay

/** Скорость сейчас, график за 2 минуты и трафик за подключение (через VPN / напрямую). Замера скорости нет — он грузит серверы. */
@Composable
fun StatsPage(vm: MeowsViewModel, onBack: () -> Unit) {
    var tick by remember { mutableIntStateOf(0) }
    LaunchedEffect(Unit) { while (true) { delay(1000); tick++ } }
    val samples = remember(tick) { StatsFeed.samples() }
    val t = remember(tick) { StatsFeed.totals() }
    val now = samples.lastOrNull()
    val cs = MaterialTheme.colorScheme
    vm.rev
    SettingsPageScaffold("Статистика", onBack) {
        if (vm.vpnState != XKVpnService.CONNECTED) {
            Card2 { Text("Подключитесь — здесь появятся скорость и график трафика.", Modifier.padding(20.dp), color = cs.onSurfaceVariant) }
            return@SettingsPageScaffold
        }
        Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Speed("Получение", now?.down ?: 0, cs.primary, Modifier.weight(1f))
            Speed("Отправка", now?.up ?: 0, cs.tertiary, Modifier.weight(1f))
        }
        Spacer(Modifier.height(12.dp))
        Card2 {
            Column(Modifier.padding(16.dp)) {
                Text("Последние 2 минуты", style = MaterialTheme.typography.labelLarge, color = cs.onSurfaceVariant)
                Spacer(Modifier.height(8.dp))
                SpeedChart(samples, Modifier.fillMaxWidth().height(140.dp))
            }
        }
        Spacer(Modifier.height(12.dp))
        Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Totals("Через VPN", t.vpnDown, t.vpnUp, Modifier.weight(1f))
            Totals("Напрямую", t.directDown, t.directUp, Modifier.weight(1f))
        }
    }
}

@Composable
private fun Card2(content: @Composable () -> Unit) {
    Surface(Modifier.fillMaxWidth().padding(horizontal = 16.dp), shape = MaterialTheme.shapes.extraLarge, color = MaterialTheme.colorScheme.surfaceContainer) { content() }
}

@Composable
private fun Speed(title: String, bytesPerSec: Long, color: Color, modifier: Modifier) {
    Surface(modifier, shape = MaterialTheme.shapes.extraLarge, color = MaterialTheme.colorScheme.surfaceContainer) {
        Column(Modifier.padding(16.dp)) {
            Text(title, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(formatBytesRu(bytesPerSec) + "/с", style = MaterialTheme.typography.titleLarge, color = color)
        }
    }
}

@Composable
private fun Totals(title: String, down: Long, up: Long, modifier: Modifier) {
    Surface(modifier, shape = MaterialTheme.shapes.extraLarge, color = MaterialTheme.colorScheme.surfaceContainer) {
        Column(Modifier.padding(16.dp)) {
            Text(title, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(6.dp))
            Text("↓ " + formatBytesRu(down), style = MaterialTheme.typography.bodyLarge)
            Text("↑ " + formatBytesRu(up), style = MaterialTheme.typography.bodyLarge)
        }
    }
}
