package app.meows.vpn.ui.settings.pages

import app.meows.vpn.i18n.tr
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.SwapHoriz
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import app.meows.vpn.MeowsViewModel
import app.meows.vpn.SwitchFilter
import app.meows.vpn.XKStore
import app.meows.vpn.ui.settings.SettingsGroup
import app.meows.vpn.ui.settings.SettingsPageScaffold
import app.meows.vpn.ui.settings.SwitchRow

/** Автопереключение: тумблер и выбор серверов, на которые можно переключаться (все, кроме отмеченных / только отмеченные). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AutoSwitchPage(vm: MeowsViewModel, onBack: () -> Unit) {
    val sp = vm.st.prefs()
    val servers = vm.servers
    val mode = vm.pref(SwitchFilter.P_MODE, 0)
    val marked = SwitchFilter.marked(sp, servers)
    SettingsPageScaffold(tr("Автопереключение"), onBack) {
        SettingsGroup(footer = tr("Если сервер перестал отвечать, Meows подключится к другому рабочему. Выключите, если нужна конкретная страна.")) {
            SwitchRow(Icons.Rounded.SwapHoriz, tr("Автопереключение серверов"), vm.flag(XKStore.P_AUTOSWITCH, true)) { vm.setFlag(XKStore.P_AUTOSWITCH, it) }
        }
        SettingsGroup(
            tr("Куда переключаться"),
            footer = if (mode == 1) tr("Meows переключится только на отмеченные серверы.")
            else tr("Meows не переключится на отмеченные серверы. Российские исключены по умолчанию: через них не открываются заблокированные сайты. Сервер, выбранный вручную, остаётся как есть."),
        ) {
            val modes = listOf(tr("Все, кроме"), tr("Выбранные"))
            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth().padding(16.dp)) {
                modes.forEachIndexed { i, m ->
                    SegmentedButton(selected = mode == i, onClick = { vm.setSwitchMode(i) }, shape = SegmentedButtonDefaults.itemShape(i, modes.size)) { Text(m, maxLines = 1) }
                }
            }
            if (servers.isEmpty()) Text(tr("Добавьте подписку, чтобы выбрать серверы."), Modifier.padding(16.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
            servers.forEach { s ->
                val on = s.title in marked
                ListItem(
                    modifier = Modifier.clickable { vm.toggleSwitchTarget(s.title) },
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                    leadingContent = {
                        Surface(shape = CircleShape, color = MaterialTheme.colorScheme.secondaryContainer) {
                            Text(s.flag?.takeIf { it.isNotBlank() } ?: "·", Modifier.padding(horizontal = 8.dp, vertical = 6.dp), style = MaterialTheme.typography.titleMedium)
                        }
                    },
                    headlineContent = { Text(s.title) },
                    supportingContent = if (mode == 0 && on) { { Text(tr("Исключён")) } } else null,
                    trailingContent = { Checkbox(on, onCheckedChange = null) },
                )
            }
        }
    }
}
