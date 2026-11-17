package app.meows.desktop.ui.nav

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Public
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.meows.desktop.ui.Res

enum class Page { Home, Settings }

/** Левая навигационная рейка (Material 3): Серверы · Добавить подписку · Настройки. Сюда же встанут «Логи» и другие разделы. */
@Composable
fun MeowsRail(page: Page, onPage: (Page) -> Unit, onAdd: () -> Unit) {
    NavigationRail(
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        header = { Image(Res.bitmap("icon.png"), "Meows", Modifier.padding(top = 12.dp).size(40.dp)); Spacer(Modifier.height(8.dp)) },
    ) {
        NavigationRailItem(page == Page.Home, { onPage(Page.Home) }, { Icon(Icons.Rounded.Public, null) }, label = { Text("Серверы") })
        NavigationRailItem(false, onAdd, { Icon(Icons.Rounded.Add, null) }, label = { Text("Добавить") })
        NavigationRailItem(page == Page.Settings, { onPage(Page.Settings) }, { Icon(Icons.Rounded.Settings, null) }, label = { Text("Настройки") })
    }
}
