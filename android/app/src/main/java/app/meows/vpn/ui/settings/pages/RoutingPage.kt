package app.meows.vpn.ui.settings.pages

import app.meows.vpn.i18n.tr
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material.icons.rounded.Router
import androidx.compose.material.icons.rounded.VpnLock
import androidx.compose.runtime.Composable
import app.meows.vpn.MeowsViewModel
import app.meows.vpn.XKStore
import app.meows.vpn.ui.settings.SettingsGroup
import app.meows.vpn.ui.settings.SettingsPageScaffold
import app.meows.vpn.ui.settings.SwitchRow
import app.meows.vpn.ui.settings.ValueRow

/**
 * Что идёт через VPN, а что напрямую. Основной split (РФ напрямую, заблокированное через VPN) задаёт сама подписка;
 * здесь — российские приложения мимо VPN и личные исключения. Подстраницы: категории/сайты и приложения.
 */
@Composable
fun RoutingPage(vm: MeowsViewModel, onBack: () -> Unit, openApps: () -> Unit) {
    val st = vm.st
    vm.rev
    val allVpn = vm.flag(XKStore.P_ALL_VPN, false)
    val offNote = tr("В режиме «Весь трафик через VPN» не действует")
    SettingsPageScaffold(tr("Маршрутизация"), onBack) {
        SettingsGroup(footer = tr("Правила маршрутизации (российское напрямую, заблокированное через VPN) приходят вместе с подпиской. Российские приложения целиком работают без VPN — банки и Госуслуги не ломаются от иностранного IP.")) {
            SwitchRow(Icons.Rounded.VpnLock, tr("Весь трафик через VPN"), vm.flag(XKStore.P_ALL_VPN, false), busy = vm.isApplying(XKStore.P_ALL_VPN)) {
                vm.setFlag(XKStore.P_ALL_VPN, it, reconnect = true)
            }
            SwitchRow(Icons.Rounded.Router, tr("Российские приложения напрямую"), vm.flag(XKStore.P_RU_APPS, true), if (allVpn) offNote else tr("Сбербанк, Госуслуги, Яндекс и другие"), enabled = !allVpn, busy = vm.isApplying(XKStore.P_RU_APPS)) {
                vm.setFlag(XKStore.P_RU_APPS, it, reconnect = true)
            }
        }
        SettingsGroup(footer = if (allVpn) offNote else tr("Выбранные приложения целиком работают без VPN.")) {
            ValueRow(Icons.Rounded.Apps, tr("Приложения без VPN"), "${st.excludedApps().size}", onClick = openApps)
        }
    }
}