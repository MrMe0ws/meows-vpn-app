package app.meows.vpn.ui.settings.pages

import android.content.Intent
import android.net.Uri
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material.icons.rounded.Fingerprint
import androidx.compose.material.icons.rounded.Info
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import app.meows.vpn.MeowsViewModel
import app.meows.vpn.ui.settings.SettingsGroup
import app.meows.vpn.ui.settings.SettingsPageScaffold
import app.meows.vpn.ui.settings.ValueRow
import app.meows.vpn.update.AppLinks

/** Версия, HWID, ссылка на проект. */
@Composable
fun AboutPage(vm: MeowsViewModel, onBack: () -> Unit) {
    val ctx = LocalContext.current
    val hwid = remember { vm.st.hwid() }
    SettingsPageScaffold("О приложении", onBack) {
        SettingsGroup(footer = "HWID отправляется в панель при обновлении подписки. Он не меняется при переустановке. Нажмите, чтобы скопировать.") {
            ValueRow(Icons.Rounded.Info, "Версия", vm.updates.currentVersion)
            ValueRow(Icons.Rounded.Fingerprint, "ID устройства (HWID)", hwid.take(8) + "…", onClick = {
                vm.copy(hwid, "hwid"); vm.toast = "HWID скопирован"
            })
        }
        SettingsGroup {
            ValueRow(Icons.Rounded.Code, "Проект на GitHub", summary = AppLinks.GITHUB_URL.removePrefix("https://"), onClick = {
                runCatching { ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(AppLinks.GITHUB_URL)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
            })
        }
    }
}
