package app.meows.vpn.ui.settings

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.SystemUpdate
import androidx.compose.material.icons.rounded.Autorenew
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import app.meows.vpn.MeowsViewModel
import app.meows.vpn.XKStore
import app.meows.vpn.ui.settings.pages.autoUpdateTitle
import app.meows.vpn.ui.theme.Palettes
import app.meows.vpn.update.AppLinks
import androidx.compose.foundation.layout.Arrangement

/** Главная настроек: короткий список разделов (иконка, название, краткое текущее значение). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsHome(vm: MeowsViewModel, onBack: () -> Unit, open: (SettingsPage) -> Unit) {
    val st = vm.st
    vm.rev
    val ctx = LocalContext.current
    val theme = listOf("Авто", "Светлая", "Тёмная")[vm.themeMode.coerceIn(0, 2)]
    val color = if (vm.dynamicColors) "из обоев" else Palettes.names[vm.palette.coerceIn(0, Palettes.names.lastIndex)]
    val bypassCount = st.bypassCats().size + st.bypassSites().size + st.excludedApps().size

    SettingsPageScaffold("Настройки", onBack) {
        SettingsGroup {
            ValueRow(Icons.Rounded.Palette, "Оформление", summary = "$theme · $color") { open(SettingsPage.Appearance) }
            ValueRow(
                Icons.Rounded.Language, "Маршрутизация",
                summary = (if (st.ruDirect()) "Российское напрямую" else "Всё через VPN") + if (bypassCount > 0) " · исключений: $bypassCount" else "",
            ) { open(SettingsPage.Routing) }
            ValueRow(Icons.Rounded.Autorenew, "Подписка", summary = autoUpdateTitle(st)) { open(SettingsPage.Subscription) }
            ValueRow(
                Icons.Rounded.PlayArrow, "Подключение",
                summary = if (st.flag(XKStore.P_AUTOCONN, false)) "Автозапуск включён" else "Always-on, локальная сеть",
            ) { open(SettingsPage.Connection) }
            ValueRow(
                Icons.Rounded.Speed, "Пинг",
                summary = (if (st.pref(XKStore.P_PING_MODE, 0) == 1) "TCP" else "Реальный") + " · ${st.pref(XKStore.P_PING_TO, 3)} с",
            ) { open(SettingsPage.Ping) }
            ValueRow(
                Icons.Rounded.SystemUpdate, "Обновления приложения",
                summary = if (vm.updates.autoCheck) "Проверять автоматически" else "Проверка выключена",
            ) { open(SettingsPage.Updates) }
            ValueRow(Icons.Rounded.Info, "О приложении", summary = "Версия ${vm.updates.currentVersion}") { open(SettingsPage.About) }
        }

        SettingsGroup {
            ValueRow(Icons.Rounded.Share, "Рассказать друзьям", summary = "Отправить ссылку на приложение") {
                val send = Intent(Intent.ACTION_SEND).setType("text/plain")
                    .putExtra(Intent.EXTRA_TEXT, "Мяус VPN — быстрый и удобный VPN. Скачать приложение для Android: ${AppLinks.RELEASES_URL}")
                runCatching { ctx.startActivity(Intent.createChooser(send, "Рассказать друзьям").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
            }
            ValueRow(Icons.Rounded.Code, "Проект на GitHub", summary = AppLinks.GITHUB_URL.removePrefix("https://")) {
                runCatching { ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(AppLinks.GITHUB_URL)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
            }
        }
        Row(Modifier.fillMaxWidth().padding(top = 20.dp), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
            Text("Мяус VPN ${vm.updates.currentVersion}", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
