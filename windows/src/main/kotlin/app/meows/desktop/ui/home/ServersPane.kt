package app.meows.desktop.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import app.meows.desktop.ui.components.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.meows.desktop.AppModel

/** Середина: карточки подписок (закреплённые выше) — у каждой свои «Обновить», «Пинг», меню «⋮» — и их серверы. */
@Composable
fun ServersPane(app: AppModel, onAdd: () -> Unit, modifier: Modifier = Modifier) {
    val collapsed = remember { mutableStateListOf<Int>() }   // свёрнутые карточки (по id подписки)
    val entries = app.ordered

    Column(modifier) {
        if (entries.isEmpty()) {
            Column(Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Добавьте подписку", style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.height(8.dp))
                Text(
                    "Ссылку подписки можно взять в боте или личном кабинете. Серверы появятся автоматически.",
                    style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(16.dp))
                Button(onClick = onAdd) { Text("Добавить подписку") }
            }
        } else LazyColumn(Modifier.fillMaxWidth(), contentPadding = PaddingValues(bottom = 16.dp)) {
            entries.forEach { e ->
                val expanded = e.id !in collapsed
                val servers = e.data.servers
                item(key = "head${e.id}") {
                    SubscriptionCard(
                        e.data, expanded, { if (expanded) collapsed.add(e.id) else collapsed.remove(e.id) }, e.refreshing, e.pinging, e.pinned,
                        onRefresh = { app.refresh(e, false) }, onPing = { app.pingAll(e) }, onFastest = { app.selectFastest(e) },
                        onPin = { app.togglePin(e) }, onDelete = { app.deleteSubscription(e) },
                    )
                }
                if (expanded) itemsIndexed(servers, key = { i, _ -> "srv${e.id}_$i" }) { i, s ->
                    ServerRow(
                        s, selected = app.isActive(e) && i == e.selected, last = i == servers.lastIndex, ping = e.pings[i], busy = i in e.pingBusy,
                        onSelect = { app.select(e, i) }, onPing = { app.pingServer(e, i) },
                    )
                }
                item(key = "gap${e.id}") { Spacer(Modifier.height(16.dp)) }
            }
        }
    }
}
