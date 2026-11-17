package app.meows.desktop.ui.nav

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Terminal
import androidx.compose.material.icons.rounded.Public
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationRail
import app.meows.desktop.ui.components.NavigationRailItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.meows.desktop.ui.Res

enum class Page { Home, Settings, Logs, About }

/** Левая навигационная рейка (Material 3): Серверы · Добавить подписку · Настройки · Логи; «О программе» — внизу. */
@Composable
fun MeowsRail(page: Page, onPage: (Page) -> Unit, onAdd: () -> Unit) {
    NavigationRail(
        modifier = Modifier.width(96.dp),
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        header = { Image(Res.bitmap("icon.png"), "Meows", Modifier.padding(top = 12.dp).size(40.dp)); Spacer(Modifier.height(8.dp)) },
    ) {
        NavigationRailItem(page == Page.Home, { onPage(Page.Home) }, { Icon(Icons.Rounded.Public, null) }, label = { Text("Серверы") })
        NavigationRailItem(false, onAdd, { Icon(Icons.Rounded.Add, null) }, label = { Text("Добавить") })
        NavigationRailItem(page == Page.Settings, { onPage(Page.Settings) }, { Icon(Icons.Rounded.Settings, null) }, label = { Text("Настройки") })
        NavigationRailItem(page == Page.Logs, { onPage(Page.Logs) }, { Icon(Icons.Rounded.Terminal, null) }, label = { Text("Логи") })
        Spacer(Modifier.weight(1f))
        NavigationRailItem(page == Page.About, { onPage(Page.About) }, { Icon(Icons.Rounded.Info, null) }, label = { Text("О программе") })
        Spacer(Modifier.height(8.dp))
    }
}
