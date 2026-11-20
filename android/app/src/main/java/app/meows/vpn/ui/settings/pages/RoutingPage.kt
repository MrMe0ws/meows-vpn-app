package app.meows.vpn.ui.settings.pages

import app.meows.vpn.i18n.tr
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material.icons.rounded.Block
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.Public
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
fun RoutingPage(vm: MeowsViewModel, onBack: () -> Unit, openBypass: () -> Unit, openApps: () -> Unit) {
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
            SwitchRow(Icons.Rounded.Block, tr("Блокировать рекламу"), vm.flag(XKStore.P_ADS, false), busy = vm.isApplying(XKStore.P_ADS)) {
                vm.setFlag(XKStore.P_ADS, it, reconnect = true)
            }
        }
        SettingsGroup(footer = tr("Включайте, только если какой-то российский сайт всё равно открывается через VPN: правило добавляется поверх подписки и отправляет напрямую все зоны .ru, .su, .рф.")) {
            SwitchRow(Icons.Rounded.Public, tr("Все российские сайты напрямую"), vm.flag(XKStore.P_RU, false), if (allVpn) offNote else tr("Расширенно, поверх правил подписки"), enabled = !allVpn, busy = vm.isApplying(XKStore.P_RU)) {
                vm.setFlag(XKStore.P_RU, it, reconnect = true)
            }
        }
        SettingsGroup(footer = if (allVpn) offNote else tr("Свои исключения: выбранные категории и сайты идут мимо VPN, выбранные приложения целиком работают без VPN.")) {
            ValueRow(Icons.Rounded.Language, tr("Мимо VPN: категории и сайты"), "${st.bypassCats().size + st.bypassSites().size}", onClick = openBypass)
            ValueRow(Icons.Rounded.Apps, tr("Приложения без VPN"), "${st.excludedApps().size}", onClick = openApps)
        }
    }
}