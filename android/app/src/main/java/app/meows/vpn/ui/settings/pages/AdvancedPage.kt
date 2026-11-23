package app.meows.vpn.ui.settings.pages

import android.content.Intent
import android.provider.Settings
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.AltRoute
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.Public
import androidx.compose.material.icons.rounded.Router
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import app.meows.vpn.MeowsViewModel
import app.meows.vpn.XKStore
import app.meows.vpn.i18n.tr
import app.meows.vpn.ui.settings.SettingsGroup
import app.meows.vpn.ui.settings.SettingsPageScaffold
import app.meows.vpn.ui.settings.SwitchRow
import app.meows.vpn.ui.settings.ValueRow

/** «Для опытных»: настройки, которые меняют работу сети и требуют понимания, что это такое. По умолчанию трогать не нужно. */
@Composable
fun AdvancedPage(vm: MeowsViewModel, onBack: () -> Unit, openBypass: () -> Unit, openProfiles: () -> Unit) {
    val ctx = LocalContext.current
    val st = vm.st
    vm.rev
    val allVpn = vm.flag(XKStore.P_ALL_VPN, false)
    val offNote = tr("В режиме «Весь трафик через VPN» не действует")
    SettingsPageScaffold(tr("Для опытных"), onBack) {
        SettingsGroup(title = tr("Маршруты"), footer = tr("Эти настройки меняют работу сети. Если не уверены, оставьте по умолчанию.")) {
            ValueRow(Icons.AutoMirrored.Rounded.AltRoute, tr("Профили маршрутизации"), summary = routeProfileTitle(vm), onClick = openProfiles)
            ValueRow(Icons.Rounded.Language, tr("Мимо VPN: категории и сайты"), "${st.bypassCats().size + st.bypassSites().size}", onClick = openBypass)
            SwitchRow(Icons.Rounded.Public, tr("Все российские сайты напрямую"), vm.flag(XKStore.P_RU, false), if (allVpn) offNote else tr("Расширенно, поверх правил подписки"), enabled = !allVpn, busy = vm.isApplying(XKStore.P_RU)) {
                vm.setFlag(XKStore.P_RU, it, reconnect = true)
            }
        }
        SettingsGroup(title = tr("Сеть"), footer = tr("«Защита от обрыва VPN» включается в настройках Android (по умолчанию выключено). «Блокировать соединения без VPN» включайте осторожно: тогда интернета не будет и у приложений, которые мы намеренно пускаем напрямую (российские приложения, обновления Meows).")) {
            SwitchRow(Icons.Rounded.Router, tr("Локальная сеть мимо VPN"), vm.flag(XKStore.P_LAN, true), tr("Роутер, принтеры, Chromecast"), busy = vm.isApplying(XKStore.P_LAN)) {
                vm.setFlag(XKStore.P_LAN, it, reconnect = true)
            }
            ValueRow(Icons.Rounded.Shield, tr("Защита от обрыва VPN"), summary = tr("VPN включается сам и не даёт трафику уйти открыто при обрыве")) {
                runCatching { ctx.startActivity(Intent(Settings.ACTION_VPN_SETTINGS)) }
                vm.toast = tr("Нажмите ⚙ рядом с Meows → «Постоянная VPN»")
            }
        }
    }
}
