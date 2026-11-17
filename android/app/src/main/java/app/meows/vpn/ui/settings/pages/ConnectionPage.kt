package app.meows.vpn.ui.settings.pages

import android.content.Intent
import android.provider.Settings
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Router
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.SwapHoriz
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import app.meows.vpn.MeowsViewModel
import app.meows.vpn.XKStore
import app.meows.vpn.ui.settings.SettingsGroup
import app.meows.vpn.ui.settings.SettingsPageScaffold
import app.meows.vpn.ui.settings.SwitchRow
import app.meows.vpn.ui.settings.ValueRow

/** Автозапуск, автопереключение серверов, локальная сеть, системная «защита от обрыва» (Always-on VPN / Kill switch). */
@Composable
fun ConnectionPage(vm: MeowsViewModel, onBack: () -> Unit) {
    val ctx = LocalContext.current
    SettingsPageScaffold("Подключение", onBack) {
        SettingsGroup {
            SwitchRow(Icons.Rounded.PlayArrow, "Подключаться при запуске", vm.flag(XKStore.P_AUTOCONN, false)) { vm.setFlag(XKStore.P_AUTOCONN, it) }
            SwitchRow(Icons.Rounded.SwapHoriz, "Автопереключение серверов", vm.flag(XKStore.P_AUTOSWITCH, true), "Если сервер перестал отвечать, Meows подключится к ближайшему рабочему. Выключите, если нужна конкретная страна") {
                vm.setFlag(XKStore.P_AUTOSWITCH, it)
            }
            SwitchRow(Icons.Rounded.Router, "Локальная сеть мимо VPN", vm.flag(XKStore.P_LAN, true), "Роутер, принтеры, Chromecast") {
                vm.setFlag(XKStore.P_LAN, it, reconnect = true)
            }
        }
        SettingsGroup(footer = "Включается в настройках Android (по умолчанию выключено). «Блокировать соединения без VPN» включайте осторожно: тогда интернета не будет и у приложений, которые мы намеренно пускаем напрямую (российские приложения, обновления Meows).") {
            ValueRow(Icons.Rounded.Shield, "Защита от обрыва VPN", summary = "VPN включается сам и не даёт трафику уйти открыто при обрыве") {
                runCatching { ctx.startActivity(Intent(Settings.ACTION_VPN_SETTINGS)) }
                vm.toast = "Нажмите ⚙ рядом с Meows → «Постоянная VPN»"
            }
        }
    }
}
