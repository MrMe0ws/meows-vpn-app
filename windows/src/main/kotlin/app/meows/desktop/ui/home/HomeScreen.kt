package app.meows.desktop.ui.home

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.meows.desktop.AppModel

/** Главная в три колонки (рейка слева — в [app.meows.desktop.ui.MeowsApp]): серверы · управление подключением. */
@Composable
fun HomeScreen(app: AppModel, onAdd: () -> Unit) {
    Row(Modifier.fillMaxSize().padding(12.dp)) {
        ServersPane(app, onAdd, Modifier.weight(1f).fillMaxSize())
        ConnectPane(app, Modifier.width(400.dp).fillMaxSize().padding(start = 12.dp))
    }
}
