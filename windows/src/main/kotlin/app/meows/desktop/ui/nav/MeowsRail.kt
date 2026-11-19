package app.meows.desktop.ui.nav

import app.meows.desktop.i18n.tr
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.TooltipArea
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ChevronLeft
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Public
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Terminal
import androidx.compose.material.icons.rounded.Timeline
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import app.meows.desktop.ui.components.IconButton
import app.meows.desktop.ui.components.NavigationRailItem

enum class Page { Home, Settings, Stats, Logs, About }

/**
 * Левая навигационная рейка (Material 3): Серверы · Добавить подписку · Настройки · Статистика · Логи; «О программе» — внизу.
 * Сверху — кнопка-стрелка: свернуть рейку до одних иконок (при наведении — подсказка с названием) и развернуть обратно.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MeowsRail(page: Page, onPage: (Page) -> Unit, onAdd: () -> Unit, expanded: Boolean, onToggle: () -> Unit) {
    @Composable
    fun item(selected: Boolean, onClick: () -> Unit, icon: ImageVector, label: String) {
        val core = @Composable {
            NavigationRailItem(selected, onClick, { Icon(icon, label) }, label = if (expanded) ({ Text(label) }) else null)
        }
        if (expanded) core() else TooltipArea(
            tooltip = {
                Surface(shape = MaterialTheme.shapes.small, color = MaterialTheme.colorScheme.inverseSurface, shadowElevation = 4.dp) {
                    Text(label, Modifier.padding(horizontal = 10.dp, vertical = 6.dp), color = MaterialTheme.colorScheme.inverseOnSurface, style = MaterialTheme.typography.labelLarge)
                }
            },
        ) { core() }
    }

    NavigationRail(
        modifier = Modifier.width(if (expanded) 96.dp else 72.dp),
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        header = {
            Spacer(Modifier.height(12.dp))
            IconButton(onClick = onToggle) { Icon(if (expanded) Icons.Rounded.ChevronLeft else Icons.Rounded.ChevronRight, if (expanded) tr("Свернуть меню") else tr("Развернуть меню")) }
            Spacer(Modifier.height(8.dp))
        },
    ) {
        item(page == Page.Home, { onPage(Page.Home) }, Icons.Rounded.Public, tr("Серверы"))
        item(false, onAdd, Icons.Rounded.Add, tr("Добавить"))
        item(page == Page.Settings, { onPage(Page.Settings) }, Icons.Rounded.Settings, tr("Настройки"))
        item(page == Page.Stats, { onPage(Page.Stats) }, Icons.Rounded.Timeline, tr("Статистика"))
        item(page == Page.Logs, { onPage(Page.Logs) }, Icons.Rounded.Terminal, tr("Логи"))
        Spacer(Modifier.weight(1f))
        item(page == Page.About, { onPage(Page.About) }, Icons.Rounded.Info, tr("О программе"))
        Spacer(Modifier.height(8.dp))
    }
}
