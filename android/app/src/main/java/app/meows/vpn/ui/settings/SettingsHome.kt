package app.meows.vpn.ui.settings

import app.meows.vpn.ui.settings.pages.languageName
import androidx.compose.material.icons.rounded.Translate
import app.meows.vpn.i18n.trf
import app.meows.vpn.i18n.tr
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.SystemUpdate
import androidx.compose.material.icons.rounded.Timeline
import androidx.compose.material.icons.rounded.Tune
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
import app.meows.vpn.Edition
import app.meows.vpn.update.AppLinks
import androidx.compose.foundation.layout.Arrangement

/** Главная настроек: короткий список разделов (иконка, название, краткое текущее значение). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsHome(vm: MeowsViewModel, onBack: () -> Unit, open: (SettingsPage) -> Unit) {
    val st = vm.st
    vm.rev
    val ctx = LocalContext.current
    val theme = listOf(tr("Авто"), tr("Светлая"), tr("Тёмная"))[vm.themeMode.coerceIn(0, 2)]
    val color = if (vm.dynamicColors) tr("из обоев") else Palettes.names[vm.palette.coerceIn(0, Palettes.names.lastIndex)]
    val bypassCount = st.bypassCats().size + st.bypassSites().size + st.excludedApps().size

    SettingsPageScaffold(tr("Настройки"), onBack) {
        // группы по смыслу: внешний вид / работа VPN / измерения / приложение; между группами зазор больше, чем между пунктами
        SettingsGroup {
            ValueRow(Icons.Rounded.Palette, tr("Оформление"), summary = "$theme · $color") { open(SettingsPage.Appearance) }
            ValueRow(Icons.Rounded.Translate, tr("Язык"), summary = languageName()) { open(SettingsPage.Language) }
        }
        SettingsGroup {
            ValueRow(Icons.Rounded.Autorenew, tr("Подписка"), summary = autoUpdateTitle(st)) { open(SettingsPage.Subscription) }
            ValueRow(
                Icons.Rounded.PlayArrow, tr("Подключение"),
                summary = if (st.flag(XKStore.P_AUTOCONN, false)) tr("Автозапуск включён") else tr("Автозапуск, автопереключение"),
            ) { open(SettingsPage.Connection) }
            ValueRow(
                Icons.Rounded.Language, tr("Маршрутизация"),
                summary = if (st.allVpn()) tr("Весь трафик через VPN") else (if (st.ruApps()) tr("Российские приложения напрямую") else tr("По правилам подписки")) + if (bypassCount > 0) " · " + trf("исключений: %d", bypassCount) else "",
            ) { open(SettingsPage.Routing) }
        }
        SettingsGroup {
            ValueRow(
                Icons.Rounded.Speed, tr("Пинг"),
                summary = (if (st.pref(XKStore.P_PING_MODE, 0) == 1) "TCP" else tr("Реальный")) + " · ${st.pref(XKStore.P_PING_TO, XKStore.PING_TO_DEFAULT)} ${tr("с")}",
            ) { open(SettingsPage.Ping) }
            ValueRow(Icons.Rounded.Timeline, tr("Статистика"), summary = tr("Скорость и трафик за подключение")) { open(SettingsPage.Stats) }
        }
        SettingsGroup {
            if (!Edition.isPlay) ValueRow(
                Icons.Rounded.SystemUpdate, tr("Обновления приложения"),
                summary = if (vm.updates.autoCheck) tr("Проверять автоматически") else tr("Проверка выключена"),
            ) { open(SettingsPage.Updates) }
            ValueRow(Icons.Rounded.Info, tr("О приложении"), summary = "${tr("Версия")} ${vm.updates.currentVersion}") { open(SettingsPage.About) }
        }

        SettingsGroup {
            ValueRow(Icons.Rounded.Tune, tr("Для опытных"), summary = tr("Маршруты, локальная сеть, защита от обрыва")) { open(SettingsPage.Advanced) }
        }
        SettingsGroup {
            ValueRow(Icons.Rounded.Share, tr("Рассказать друзьям"), summary = tr("Отправить ссылку на приложение")) {
                val send = Intent(Intent.ACTION_SEND).setType("text/plain")
                    .putExtra(Intent.EXTRA_TEXT, trf("Meows — быстрый и удобный VPN. Скачать приложение для Android: %s", if (Edition.isPlay) Edition.PLAY_URL else AppLinks.RELEASES_URL))
                runCatching { ctx.startActivity(Intent.createChooser(send, tr("Рассказать друзьям")).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
            }
        }
        Row(Modifier.fillMaxWidth().padding(top = 20.dp), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
            Text("Meows ${vm.updates.currentVersion}", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
