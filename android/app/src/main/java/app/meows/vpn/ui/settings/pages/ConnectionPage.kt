package app.meows.vpn.ui.settings.pages

import android.content.Intent
import android.provider.Settings
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Router
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import app.meows.vpn.MeowsViewModel
import app.meows.vpn.XKStore
import app.meows.vpn.ui.settings.SettingsGroup
import app.meows.vpn.ui.settings.SettingsPageScaffold
import app.meows.vpn.ui.settings.SwitchRow
import app.meows.vpn.ui.settings.ValueRow

/** Автозапуск, локальная сеть, системные Always-on VPN и Kill switch. */
@Composable
fun ConnectionPage(vm: MeowsViewModel, onBack: () -> Unit) {
    val ctx = LocalContext.current
    SettingsPageScaffold("Подключение", onBack) {
        SettingsGroup {
            SwitchRow(Icons.Rounded.PlayArrow, "Подключаться при запуске", vm.flag(XKStore.P_AUTOCONN, false)) { vm.setFlag(XKStore.P_AUTOCONN, it) }
            SwitchRow(Icons.Rounded.Router, "Локальная сеть мимо VPN", vm.flag(XKStore.P_LAN, true), "Роутер, принтеры, Chromecast") {
                vm.setFlag(XKStore.P_LAN, it, reconnect = true)
            }
        }
        SettingsGroup(footer = "Kill switch включается в системе: ⚙ рядом с приложением → «Постоянная VPN» и «Блокировать соединения без VPN».") {
            ValueRow(Icons.Rounded.Shield, "Always-on и Kill switch") {
                runCatching { ctx.startActivity(Intent(Settings.ACTION_VPN_SETTINGS)) }
                vm.toast = "Нажмите ⚙ рядом с Мяус VPN → «Постоянная VPN»"
            }
        }
    }
}
