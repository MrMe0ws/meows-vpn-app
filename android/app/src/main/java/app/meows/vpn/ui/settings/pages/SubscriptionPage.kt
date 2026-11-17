package app.meows.vpn.ui.settings.pages

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Autorenew
import androidx.compose.material.icons.rounded.Fingerprint
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.Update
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import app.meows.vpn.MeowsViewModel
import app.meows.vpn.XKStore
import app.meows.vpn.ui.settings.ChoiceDialog
import app.meows.vpn.ui.settings.SettingsGroup
import app.meows.vpn.ui.settings.SettingsPageScaffold
import app.meows.vpn.ui.settings.SwitchRow
import app.meows.vpn.ui.settings.ValueRow

fun autoUpdateValue(st: XKStore): String {
    val m = st.pref(XKStore.P_AUTO, 0)
    return when {
        m < 0 -> "Выключено"
        m == 0 -> "По подписке (${st.autoUpdateHours()} ч)"
        else -> "Каждые $m ч"
    }
}

fun autoUpdateTitle(st: XKStore): String = "Автообновление: " + autoUpdateValue(st).replaceFirstChar { it.lowercase() }

/** Обновление подписки, HWID, напоминания о сроке. */
@Composable
fun SubscriptionPage(vm: MeowsViewModel, onBack: () -> Unit) {
    var dialog by remember { mutableStateOf(false) }
    SettingsPageScaffold("Подписка", onBack) {
        SettingsGroup(footer = "Напоминания приходят за 3 дня, за 1 день и в день окончания подписки.") {
            ValueRow(Icons.Rounded.Autorenew, "Автообновление", autoUpdateValue(vm.st)) { dialog = true }
            SwitchRow(Icons.Rounded.Update, "Обновлять при запуске", vm.flag(XKStore.P_LAUNCH, true)) { vm.setFlag(XKStore.P_LAUNCH, it) }
            SwitchRow(Icons.Rounded.Notifications, "Напоминать об окончании", vm.flag(XKStore.P_REMIND, true)) { vm.setFlag(XKStore.P_REMIND, it) }
        }
        SettingsGroup(footer = "HWID — идентификатор устройства для лимита устройств в панели. Если выключить, подписка может не обновиться.") {
            SwitchRow(Icons.Rounded.Fingerprint, "Отправлять HWID", vm.flag(XKStore.P_HWID, true)) { vm.setFlag(XKStore.P_HWID, it) }
        }
    }
    if (dialog) {
        val names = listOf("По подписке", "Выключено", "Каждый час", "Каждые 3 часа", "Каждые 6 часов", "Каждые 12 часов", "Раз в сутки")
        val vals = listOf(0, -1, 1, 3, 6, 12, 24)
        ChoiceDialog("Автообновление", names, vals.indexOf(vm.pref(XKStore.P_AUTO, 0)), { dialog = false }) {
            vm.setPref(XKStore.P_AUTO, vals[it]); dialog = false
        }
    }
}
