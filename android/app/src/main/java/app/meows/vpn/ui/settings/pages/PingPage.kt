package app.meows.vpn.ui.settings.pages

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Public
import androidx.compose.material.icons.rounded.Speed
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
    SettingsPageScaffold("Пинг", onBack) {
        SettingsGroup(footer = "«Реальный» — настоящий запрос через каждый сервер: честная задержка до сайта. «TCP» — только время соединения с сервером.") {
            PickRow(vm, Icons.Rounded.Speed, "Способ", XKStore.P_PING_MODE, 0, listOf("Реальный (через VPN)", "TCP"), listOf(0, 1))
            PickRow(vm, Icons.Rounded.Public, "Адрес проверки", XKStore.P_PING_URL, 0, listOf("Google", "Cloudflare", "Apple"), listOf(0, 1, 2))
        }
        SettingsGroup {
            PickRow(vm, Icons.Rounded.Info, "Показывать результат", XKStore.P_PING_KEEP, 60, listOf("30 с", "1 мин", "3 мин", "5 мин", "Всегда"), listOf(30, 60, 180, 300, 0))
            SwitchRow(Icons.Rounded.Speed, "Пинг при запуске", vm.flag(XKStore.P_PING_LAUNCH, false)) { vm.setFlag(XKStore.P_PING_LAUNCH, it) }
        }
    }
}
