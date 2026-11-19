package app.meows.vpn.ui.settings.pages

import app.meows.vpn.i18n.trf
import app.meows.vpn.i18n.tr
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.InstallMobile
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.SystemUpdate
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import app.meows.vpn.MeowsViewModel
import app.meows.vpn.ui.components.formatBytesRu
import app.meows.vpn.ui.settings.SettingsGroup
import app.meows.vpn.ui.settings.SettingsPageScaffold
import app.meows.vpn.ui.settings.SwitchRow
import app.meows.vpn.update.AppLinks
import app.meows.vpn.update.UpdateState

/** Автопроверка обновлений (галочка), ручная проверка, скачивание и установка APK с GitHub. */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun UpdatesPage(vm: MeowsViewModel, onBack: () -> Unit) {
    val u = vm.updates
    val ctx = LocalContext.current
    var auto by remember { mutableStateOf(u.autoCheck) }

    SettingsPageScaffold(tr("Обновления"), onBack) {
        SettingsGroup(footer = tr("Приложение распространяется APK-файлом, поэтому обновления ищутся на GitHub. Проверка — не чаще раза в 12 часов, только когда включена.")) {
            SwitchRow(Icons.Rounded.SystemUpdate, tr("Искать обновления"), auto, tr("При запуске приложения")) { auto = it; u.autoCheck = it }
        }

        SettingsGroup(tr("Версия")) {
            Column(Modifier.fillMaxWidth().padding(16.dp)) {
                Text(trf("Установлена: %s", u.currentVersion), style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(8.dp))
                when (val s = u.state) {
                    UpdateState.Idle -> Text(tr("Проверка ещё не выполнялась."), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    UpdateState.Checking -> Row(verticalAlignment = Alignment.CenterVertically) {
                        LoadingIndicator(Modifier.size(28.dp)); Spacer(Modifier.width(12.dp)); Text(tr("Проверяем…"))
                    }
                    UpdateState.UpToDate -> Text(tr("У вас последняя версия."), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
                    is UpdateState.Available -> {
                        Text(trf("Доступна версия %s", s.info.version) + if (s.info.apkSize > 0) " · ${formatBytesRu(s.info.apkSize)}" else "", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                        if (s.info.notes.isNotBlank()) {
                            Spacer(Modifier.height(8.dp))
                            Text(s.info.notes.trim().take(600), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Spacer(Modifier.height(12.dp))
                        Button(onClick = u::download) { Icon(Icons.Rounded.Download, null); Spacer(Modifier.width(8.dp)); Text(tr("Скачать")) }
                    }
                    is UpdateState.Downloading -> {
                        Text(trf("Скачиваем %s… %d%%", s.info.version, s.percent))
                        Spacer(Modifier.height(8.dp))
                        LinearProgressIndicator(progress = { s.percent / 100f }, modifier = Modifier.fillMaxWidth())
                    }
                    is UpdateState.Downloaded -> {
                        Text(trf("Версия %s скачана.", s.info.version), style = MaterialTheme.typography.titleMedium)
                        Spacer(Modifier.height(12.dp))
                        Button(onClick = {
                            if (!u.install()) vm.toast = tr("Разрешите установку из этого источника и нажмите «Установить» ещё раз")
                        }) { Icon(Icons.Rounded.InstallMobile, null); Spacer(Modifier.width(8.dp)); Text(tr("Установить")) }
                    }
                    is UpdateState.Error -> Text(s.message, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error)
                }
                Spacer(Modifier.height(12.dp))
                FilledTonalButton(onClick = { u.check(manual = true) }, enabled = u.state !is UpdateState.Checking && u.state !is UpdateState.Downloading) {
                    Icon(Icons.Rounded.Refresh, null); Spacer(Modifier.width(8.dp)); Text(tr("Проверить сейчас"))
                }
            }
        }

        SettingsGroup {
            app.meows.vpn.ui.settings.ValueRow(Icons.Rounded.Download, tr("Все релизы на GitHub"), onClick = {
                runCatching { ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(AppLinks.RELEASES_URL)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
            })
        }
    }
}
