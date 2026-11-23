package app.meows.vpn.ui.settings.pages

import app.meows.vpn.i18n.tr
import app.meows.vpn.i18n.trf
import app.meows.vpn.i18n.serversWord
import app.meows.vpn.MobileBypass
import androidx.compose.material.icons.rounded.SignalCellularAlt
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.SwapHoriz
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import app.meows.vpn.MeowsViewModel
import app.meows.vpn.SwitchFilter
import app.meows.vpn.XKStore
import app.meows.vpn.ui.settings.SettingsGroup
import app.meows.vpn.ui.settings.SettingsPageScaffold
import app.meows.vpn.ui.settings.SplitSwitchRow
import app.meows.vpn.ui.settings.SwitchRow

/** Автозапуск, автопереключение серверов, обход в мобильной сети. Локальная сеть и Always-on — в «Для опытных». */
@Composable
fun ConnectionPage(vm: MeowsViewModel, onBack: () -> Unit, openAutoSwitch: () -> Unit, openMobileBypass: () -> Unit) {
    val ctx = LocalContext.current
    vm.rev
    SettingsPageScaffold(tr("Подключение"), onBack) {
        SettingsGroup {
            SwitchRow(Icons.Rounded.PlayArrow, tr("Подключаться при запуске"), vm.flag(XKStore.P_AUTOCONN, false)) { vm.setFlag(XKStore.P_AUTOCONN, it) }
            val on = vm.flag(XKStore.P_AUTOSWITCH, true)
            SplitSwitchRow(
                Icons.Rounded.SwapHoriz, tr("Автопереключение серверов"), on,
                if (on) SwitchFilter.summary(vm.st.prefs(), vm.servers) else tr("Выключено"),
                onOpen = openAutoSwitch,
            ) { vm.setFlag(XKStore.P_AUTOSWITCH, it) }
            val found = MobileBypass.chosen(ctx).size
            val bypassOn = vm.flag(MobileBypass.P_ON, false) && found > 0
            SplitSwitchRow(
                Icons.Rounded.SignalCellularAlt, tr("Обход в мобильной сети"), bypassOn,
                when {
                    found == 0 -> tr("Недоступно на вашем тарифе")
                    bypassOn -> trf("Включено · %d %s", found, serversWord(found))
                    else -> tr("Выключено")
                },
                onOpen = openMobileBypass, enabled = found > 0,
            ) { vm.setMobileBypass(it) }
        }
    }
}
