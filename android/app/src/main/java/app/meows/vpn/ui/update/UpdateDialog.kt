package app.meows.vpn.ui.update

import app.meows.vpn.i18n.trf
import app.meows.vpn.i18n.tr
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.meows.vpn.MeowsViewModel
import app.meows.vpn.ui.components.formatBytesRu
import app.meows.vpn.update.UpdateState

/**
 * «Доступна новая версия». Две кнопки в стандартных местах диалога: главная залитая («Скачать» → «Установить»)
 * и «Позже» (закрывает окно; автопроверка предложит снова при одном из следующих запусков, не чаще раза в 12 часов).
 */
@Composable
fun UpdateDialog(vm: MeowsViewModel) {
    val u = vm.updates
    if (!u.dialogVisible) return
    val s = u.state
    val info = when (s) {
        is UpdateState.Available -> s.info
        is UpdateState.Downloading -> s.info
        is UpdateState.Downloaded -> s.info
        else -> return
    }
    val busy = s is UpdateState.Downloading

    AlertDialog(
        onDismissRequest = { if (!busy) u.dismissDialog() },
        title = { Text(trf("Доступна версия %s", info.version)) },
        text = {
            Column {
                Text(
                    trf("Сейчас установлена %s", u.currentVersion) + if (info.apkSize > 0) " · ${formatBytesRu(info.apkSize)}" else "",
                    style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (info.notes.isNotBlank()) {
                    Spacer(Modifier.height(12.dp))
                    Surface(shape = MaterialTheme.shapes.large, color = MaterialTheme.colorScheme.surfaceContainerHigh) {
                        Column(Modifier.heightIn(max = 200.dp).verticalScroll(rememberScrollState()).padding(14.dp)) {
                            Text(info.notes.trim(), style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
                if (s is UpdateState.Downloading) {
                    Spacer(Modifier.height(16.dp))
                    Text(trf("Скачиваем… %d%%", s.percent), style = MaterialTheme.typography.labelLarge)
                    Spacer(Modifier.height(8.dp))
                    LinearProgressIndicator(progress = { s.percent / 100f }, modifier = Modifier.fillMaxWidth())
                }
            }
        },
        confirmButton = {
            when (s) {
                is UpdateState.Downloaded -> Button(onClick = {
                    if (u.install()) u.dismissDialog()
                    else vm.toast = tr("Разрешите установку из этого источника и нажмите «Установить» ещё раз")
                }) { Text(tr("Установить")) }
                is UpdateState.Available -> Button(onClick = u::download) { Text(tr("Скачать")) }
                else -> {}
            }
        },
        dismissButton = { if (!busy) TextButton(onClick = u::dismissDialog) { Text(tr("Позже")) } },
    )
}
