package app.meows.desktop.ui.settings

import app.meows.desktop.i18n.tr
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import app.meows.desktop.AppModel
import app.meows.desktop.ui.components.Switch
import app.meows.desktop.ui.components.hand

/** Интеграция с Windows: автозапуск, подключение при запуске, сворачивание в трей. */
@Composable
fun SystemGroup(app: AppModel, openAutoSwitch: () -> Unit) {
    Group(tr("Система"), tr("При автозапуске Meows стартует свёрнутым в трей. Иконка в трее: открыть, подключить/отключить, выйти.")) {
        SwitchRow(tr("Запускать вместе с Windows"), null, app.autoStart, app::chooseAutoStart)
        SwitchRow(tr("Подключаться при запуске"), tr("Если подписка добавлена"), app.autoConnect, app::chooseAutoConnect)
        SplitSwitchRow(
            tr("Автопереключение серверов"), if (app.autoSwitch) app.switchFilter.summary(app.sub?.servers.orEmpty()) else tr("Выключено"),
            app.autoSwitch, openAutoSwitch, app::chooseAutoSwitch,
        )
        SwitchRow(tr("Напоминать об окончании"), tr("Срок подписки и трафик — уведомление у иконки в трее"), app.remind, app::chooseRemind)
        SwitchRow(tr("Сворачивать в трей при закрытии"), tr("Окно скрывается, VPN продолжает работать"), app.closeToTray, app::chooseCloseToTray)
    }
}

/** Строка с переходом и отдельным тумблером: текст открывает подробности, тумблер включает. */
@Composable
private fun SplitSwitchRow(title: String, sub: String, checked: Boolean, onOpen: () -> Unit, onChange: (Boolean) -> Unit) {
    Row(Modifier.height(IntrinsicSize.Min)) {
        // текст и стрелка — одна нажимаемая область: подсветка при наведении заливает их вместе
        Row(Modifier.weight(1f).fillMaxHeight().clickable(onClick = onOpen).hand(), verticalAlignment = Alignment.CenterVertically) {
            ListItem(
                modifier = Modifier.weight(1f), colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                headlineContent = { Text(title) }, supportingContent = { Text(sub) },
            )
            Icon(Icons.Rounded.ChevronRight, null, Modifier.padding(end = 16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        VerticalDivider(Modifier.padding(vertical = 12.dp))
        Box(Modifier.fillMaxHeight().clickable { onChange(!checked) }.hand().padding(horizontal = 16.dp), contentAlignment = Alignment.Center) { Switch(checked, onChange) }
    }
}
