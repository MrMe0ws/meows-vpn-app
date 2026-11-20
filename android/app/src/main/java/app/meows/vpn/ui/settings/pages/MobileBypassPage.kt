package app.meows.vpn.ui.settings.pages

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.SignalCellularAlt
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import app.meows.vpn.MeowsViewModel
import app.meows.vpn.MobileBypass
import app.meows.vpn.i18n.tr
import app.meows.vpn.ui.settings.SettingsGroup
import app.meows.vpn.ui.settings.SettingsPageScaffold
import app.meows.vpn.ui.settings.SwitchRow

/** Обход в мобильной сети: тумблер и выбор серверов обхода из всех подписок (найденные по названию отмечены сами). */
@Composable
fun MobileBypassPage(vm: MeowsViewModel, onBack: () -> Unit) {
    val ctx = LocalContext.current
    vm.rev
    val all = MobileBypass.entries(ctx)
    val selected = MobileBypass.selectedKeys(ctx, all)
    val has = all.any { it.key in selected }
    val on = vm.flag(MobileBypass.P_ON, false) && has
    SettingsPageScaffold(tr("Обход в мобильной сети"), onBack) {
        SettingsGroup(
            footer = if (has) tr("Когда телефон переходит с Wi-Fi на мобильный интернет, Meows подключится к серверу обхода. На Wi-Fi вернётся прежний сервер. Работает, пока VPN включён.")
            else tr("В вашей подписке нет серверов обхода (с «обход» или «белые списки» в названии). Если они называются иначе, отметьте их ниже вручную."),
        ) {
            SwitchRow(Icons.Rounded.SignalCellularAlt, tr("Обход в мобильной сети"), on, enabled = has) { vm.setMobileBypass(it) }
        }
        if (all.isEmpty()) SettingsGroup { Text(tr("Добавьте подписку, чтобы выбрать серверы."), Modifier.padding(16.dp), color = MaterialTheme.colorScheme.onSurfaceVariant) }
        val groups = all.groupBy { it.store.id() }.values.toList()
        groups.forEachIndexed { n, list ->
            SettingsGroup(
                list.first().store.title(),
                footer = if (n == groups.lastIndex)
                    tr("Если отмечено несколько серверов, Meows возьмёт первый, который отвечает. Серверы с «обход» или «белые списки» в названии отмечены сами.") else null,
            ) {
                list.forEach { e ->
                    ListItem(
                        modifier = Modifier.clickable { vm.toggleBypassServer(e.key) },
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                        leadingContent = {
                            Surface(shape = CircleShape, color = MaterialTheme.colorScheme.secondaryContainer) {
                                Text(e.server.flag?.takeIf { it.isNotBlank() } ?: "·", Modifier.padding(horizontal = 8.dp, vertical = 6.dp), style = MaterialTheme.typography.titleMedium)
                            }
                        },
                        headlineContent = { Text(e.server.title) },
                        supportingContent = if (MobileBypass.isAuto(e.server)) { { Text(tr("Найден по названию")) } } else null,
                        trailingContent = { Checkbox(e.key in selected, onCheckedChange = null) },
                    )
                }
            }
        }
    }
}
