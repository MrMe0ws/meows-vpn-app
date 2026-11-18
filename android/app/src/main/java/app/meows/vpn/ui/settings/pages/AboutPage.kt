package app.meows.vpn.ui.settings.pages

import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountCircle
import androidx.compose.material.icons.rounded.Fingerprint
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Memory
import androidx.compose.material.icons.rounded.PhoneAndroid
import androidx.compose.material.icons.rounded.SupportAgent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import app.meows.vpn.Edition
import app.meows.vpn.MeowsViewModel
import app.meows.vpn.shop.ShopLinks
import app.meows.vpn.ui.settings.SettingsGroup
import app.meows.vpn.ui.settings.SettingsPageScaffold
import app.meows.vpn.ui.settings.ValueRow

/** Версия, ядро, устройство, HWID, ссылки на кабинет и поддержку (то же, что на странице «О программе» в Windows). */
@Composable
fun AboutPage(vm: MeowsViewModel, onBack: () -> Unit) {
    val ctx = LocalContext.current
    val hwid = remember { vm.st.hwid() }
    val device = "Android ${Build.VERSION.RELEASE} · ${Build.MANUFACTURER} ${Build.MODEL} · ${Build.SUPPORTED_ABIS.firstOrNull() ?: "?"}"
    fun open(url: String) { runCatching { ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) } }
    SettingsPageScaffold("О приложении", onBack) {
        SettingsGroup(footer = "HWID отправляется в панель при обновлении подписки, чтобы считать устройства. Он не меняется при переустановке. Нажмите, чтобы скопировать.") {
            ValueRow(Icons.Rounded.Info, "Версия приложения", vm.updates.currentVersion)
            ValueRow(Icons.Rounded.Memory, "Ядро", summary = CORE)
            ValueRow(Icons.Rounded.PhoneAndroid, "Устройство", summary = device)
            ValueRow(Icons.Rounded.Fingerprint, "ID устройства (HWID)", summary = hwid, onClick = {
                vm.copy(hwid, "hwid"); vm.toast = "HWID скопирован"
            })
        }
        SettingsGroup {
            ValueRow(Icons.Rounded.SupportAgent, "Поддержка", summary = ShopLinks.support(vm.st).removePrefix("https://"), onClick = { open(ShopLinks.support(vm.st)) })
            if (!Edition.isPlay) ValueRow(Icons.Rounded.AccountCircle, "Личный кабинет", summary = ShopLinks.cabinet(vm.st).removePrefix("https://"), onClick = { open(ShopLinks.cabinet(vm.st)) })
        }
    }
}

private const val CORE = "Xray-core 26.10.31 (a52db8f)"
