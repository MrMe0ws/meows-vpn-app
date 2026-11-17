package app.meows.vpn.ui.settings.pages

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material.icons.rounded.Block
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.Router
import androidx.compose.runtime.Composable
import app.meows.vpn.MeowsViewModel
import app.meows.vpn.XKStore
import app.meows.vpn.ui.settings.SettingsGroup
import app.meows.vpn.ui.settings.SettingsPageScaffold
import app.meows.vpn.ui.settings.SwitchRow
import app.meows.vpn.ui.settings.ValueRow

/** Что идёт через VPN, а что напрямую. Подстраницы: категории/сайты и приложения. */
@Composable
fun RoutingPage(vm: MeowsViewModel, onBack: () -> Unit, openBypass: () -> Unit, openApps: () -> Unit) {
    val st = vm.st
    vm.rev
    SettingsPageScaffold("Маршрутизация", onBack) {
        SettingsGroup(footer = "Российские сайты и приложения работают через обычный интернет. Заблокированные в РФ сайты всё равно идут через VPN.") {
            SwitchRow(Icons.Rounded.Router, "Российское напрямую", vm.flag(XKStore.P_RU, true), "Сайты и приложения РФ без VPN") {
                vm.setFlag(XKStore.P_RU, it, reconnect = true)
            }
            SwitchRow(Icons.Rounded.Block, "Блокировать рекламу", vm.flag(XKStore.P_ADS, false)) {
                vm.setFlag(XKStore.P_ADS, it, reconnect = true)
            }
        }
        SettingsGroup(footer = "Категории и свои сайты идут мимо VPN. Выбранные приложения целиком работают без VPN.") {
            ValueRow(Icons.Rounded.Language, "Мимо VPN: категории и сайты", "${st.bypassCats().size + st.bypassSites().size}", onClick = openBypass)
            ValueRow(Icons.Rounded.Apps, "Приложения без VPN", "${st.excludedApps().size}", onClick = openApps)
        }
    }
}
