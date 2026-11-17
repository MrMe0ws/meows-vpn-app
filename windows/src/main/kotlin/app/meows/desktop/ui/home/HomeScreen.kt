package app.meows.desktop.ui.home

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.meows.desktop.AppModel
import app.meows.desktop.ui.Res

/** Главная: название, кнопка подключения, подписка и список серверов на одном экране; действия — в нижней панели. */
@Composable
fun HomeScreen(app: AppModel, openSettings: () -> Unit) {
    var expanded by remember { mutableStateOf(true) }
    var adding by remember { mutableStateOf(false) }
    val sub = app.sub
    val servers = sub?.servers.orEmpty()

    Box(Modifier.fillMaxSize()) {
        LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 100.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            item {
                Row(Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                    Image(Res.bitmap("icon.png"), null, Modifier.size(28.dp)); Spacer(Modifier.width(10.dp))
                    Text("Meows", style = MaterialTheme.typography.titleLarge)
                }
            }
            item { ConnectButton(app.state, 200.dp) { app.toggle() } }
            item { Spacer(Modifier.height(12.dp)); StatusText(app.state, app.connectedAt); Spacer(Modifier.height(16.dp)) }
            if (sub == null) {
                item {
                    Column(Modifier.fillMaxWidth().padding(horizontal = 24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Добавьте подписку", style = MaterialTheme.typography.titleLarge)
                        Spacer(Modifier.height(8.dp))
                        Text("Ссылку подписки можно взять в боте или личном кабинете. Серверы появятся автоматически.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
                        Spacer(Modifier.height(16.dp))
                        Button(onClick = { adding = true }) { Text("Добавить подписку") }
                    }
                }
            } else {
                item { SubscriptionCard(sub, expanded, { expanded = !expanded }, app::deleteSubscription) }
                if (expanded) itemsIndexed(servers) { i, s ->
                    ServerRow(s, selected = i == app.selected, last = i == servers.lastIndex, ping = app.pings[i]) { app.select(i) }
                }
            }
        }
        HomeToolbar(
            hasSub = sub != null, pinging = app.pinging, refreshing = app.refreshing,
            onSettings = openSettings, onAdd = { adding = true }, onPing = app::pingAll, onRefresh = { app.refresh(false) },
            modifier = Modifier.align(Alignment.BottomCenter),
        )
    }
    if (adding) AddDialog(onAdd = app::addSubscription, onDismiss = { adding = false })
}