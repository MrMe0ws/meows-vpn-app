package app.meows.vpn.ui.settings.pages

import app.meows.vpn.i18n.tr
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Public
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.runtime.Composable
import app.meows.vpn.MeowsViewModel
import app.meows.vpn.XKStore
import app.meows.vpn.ui.settings.PickRow
import app.meows.vpn.ui.settings.SettingsGroup
import app.meows.vpn.ui.settings.SettingsPageScaffold
import app.meows.vpn.ui.settings.SwitchRow

/** Как и чем мерить задержку до серверов. */
@Composable
fun PingPage(vm: MeowsViewModel, onBack: () -> Unit) {
    SettingsPageScaffold(tr("Пинг"), onBack) {
        SettingsGroup(footer = tr("«Реальный» — настоящий запрос через каждый сервер: честная задержка до сайта. «TCP» — только время соединения с сервером.")) {
            PickRow(vm, Icons.Rounded.Speed, tr("Способ"), XKStore.P_PING_MODE, 0, listOf(tr("Реальный (через VPN)"), "TCP"), listOf(0, 1))
            PickRow(vm, Icons.Rounded.Public, tr("Адрес проверки"), XKStore.P_PING_URL, 0, listOf("Google", "Cloudflare", "Apple"), listOf(0, 1, 2))
            PickRow(vm, Icons.Rounded.Timer, tr("Таймаут"), XKStore.P_PING_TO, XKStore.PING_TO_DEFAULT, listOf(tr("3 с"), tr("5 с"), tr("6 с"), tr("8 с"), tr("10 с")), listOf(3, 5, 6, 8, 10))
        }
        SettingsGroup {
            PickRow(vm, Icons.Rounded.Info, tr("Показывать результат"), XKStore.P_PING_KEEP, 60, listOf(tr("30 с"), tr("1 мин"), tr("3 мин"), tr("5 мин"), tr("Всегда")), listOf(30, 60, 180, 300, 0))
            SwitchRow(Icons.Rounded.Speed, tr("Пинг при запуске"), vm.flag(XKStore.P_PING_LAUNCH, false)) { vm.setFlag(XKStore.P_PING_LAUNCH, it) }
        }
    }
}
