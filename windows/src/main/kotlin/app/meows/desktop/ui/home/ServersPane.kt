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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.meows.desktop.AppModel

/** Середина: карточка подписки (обновить · пинг · меню) и её серверы. */
@Composable
fun ServersPane(app: AppModel, onAdd: () -> Unit, modifier: Modifier = Modifier) {
    var expanded by remember { mutableStateOf(true) }
    val sub = app.sub
    val servers = sub?.servers.orEmpty()

    Column(modifier) {
        if (sub == null) {
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
            item {
                SubscriptionCard(
                    sub, expanded, { expanded = !expanded }, app.refreshing, app.pinging,
                    onRefresh = { app.refresh(false) }, onPing = app::pingAll, onDelete = app::deleteSubscription,
                )
            }
            if (expanded) itemsIndexed(servers) { i, s ->
                ServerRow(
                    s, selected = i == app.selected, last = i == servers.lastIndex, ping = app.pings[i], busy = i in app.pingBusy,
                    onSelect = { app.select(i) }, onPing = { app.pingServer(i) },
                )
            }
        }
    }
}
