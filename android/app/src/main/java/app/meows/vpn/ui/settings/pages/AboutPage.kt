package app.meows.vpn.ui.settings.pages

import app.meows.vpn.i18n.tr
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountCircle
import androidx.compose.material.icons.rounded.Fingerprint
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Link
import androidx.compose.material.icons.rounded.Memory
import androidx.compose.material.icons.rounded.PhoneAndroid
import androidx.compose.material.icons.rounded.SupportAgent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import app.meows.vpn.MeowsViewModel
import app.meows.vpn.shop.ShopLinks
import app.meows.vpn.ui.settings.SettingsGroup
import app.meows.vpn.ui.settings.SettingsPageScaffold
import app.meows.vpn.ui.settings.ValueRow

/** Версия, ядро, устройство, HWID, ссылки на кабинет и поддержку (то же, что на странице «О программе» в Windows). */
@Composable
fun AboutPage(vm: MeowsViewModel, onBack: () -> Unit, openUrlSchemes: () -> Unit) {
    val ctx = LocalContext.current
    val hwid = remember { vm.st.hwid() }
    val device = "Android ${Build.VERSION.RELEASE} · ${Build.MANUFACTURER} ${Build.MODEL} · ${Build.SUPPORTED_ABIS.firstOrNull() ?: "?"}"
    fun open(url: String) { runCatching { ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) } }
    SettingsPageScaffold(tr("О приложении"), onBack) {
        SettingsGroup(footer = tr("HWID отправляется в панель при обновлении подписки, чтобы считать устройства. Он не меняется при переустановке. Нажмите, чтобы скопировать.")) {
            ValueRow(Icons.Rounded.Info, tr("Версия приложения"), vm.updates.currentVersion)
            ValueRow(Icons.Rounded.Memory, tr("Ядро"), summary = CORE)
            ValueRow(Icons.Rounded.PhoneAndroid, tr("Устройство"), summary = device)
            ValueRow(Icons.Rounded.Fingerprint, tr("ID устройства (HWID)"), summary = hwid, onClick = {
                vm.copy(hwid, "hwid"); vm.toast = tr("HWID скопирован")
            })
        }
        SettingsGroup {
            ShopLinks.support(vm.st)?.let { u -> ValueRow(Icons.Rounded.SupportAgent, tr("Поддержка"), summary = u.removePrefix("https://"), onClick = { open(u) }) }
            ShopLinks.cabinet(vm.st)?.let { u -> ValueRow(Icons.Rounded.AccountCircle, tr("Личный кабинет"), summary = u.removePrefix("https://"), onClick = { open(u) }) }
        }
        SettingsGroup {   // для тех, кто делает свой магазин: обычному пользователю не нужно, поэтому не на главной
            ValueRow(Icons.Rounded.Link, tr("Для разработчиков"), summary = tr("Схемы URL-адресов"), onClick = openUrlSchemes)
        }
    }
}

private const val CORE = "Xray-core 26.10.31 (a52db8f)"
